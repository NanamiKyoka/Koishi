package com.nanami.koishi.feature.tools.lan_transfer.engine.discovery

import android.content.Context
import android.net.wifi.WifiManager
import com.nanami.koishi.feature.tools.lan_transfer.engine.model.LanDevice
import com.nanami.koishi.feature.tools.lan_transfer.engine.model.LanDeviceProtocol
import com.nanami.koishi.feature.tools.lan_transfer.engine.model.toLanDevice
import com.nanami.koishi.feature.tools.lan_transfer.engine.protocol.LocalDeviceInfoProvider
import com.nanami.koishi.feature.tools.lan_transfer.engine.protocol.LocalSendProtocol
import com.nanami.koishi.feature.tools.lan_transfer.engine.protocol.LocalSendRegisterClient
import com.nanami.koishi.feature.tools.lan_transfer.engine.protocol.MulticastMessageDto
import com.nanami.koishi.feature.tools.lan_transfer.engine.protocol.RegisterRequestDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class DeviceDiscovery(
    context: Context,
    private val deviceInfoProvider: LocalDeviceInfoProvider,
    private val client: LocalSendRegisterClient,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    },
    private val interfaceProvider: NetworkInterfaceProvider = NetworkInterfaceProvider()
) {
    private val appContext = context.applicationContext

    fun discover(
        alias: String,
        serverPort: Int = LocalSendProtocol.DEFAULT_PORT,
        multicastPort: Int = LocalSendProtocol.DEFAULT_PORT,
        protocol: LanDeviceProtocol = LanDeviceProtocol.HTTPS,
        announce: Boolean = true
    ): Flow<LanDevice> = callbackFlow {
        val localInfo = deviceInfoProvider.create(alias, serverPort, protocol)
        val multicastLock = acquireMulticastLock()
        val sockets = createSockets(multicastPort)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val inFlight = ConcurrentHashMap.newKeySet<String>()
        val confirmedFingerprints = ConcurrentHashMap.newKeySet<String>()
        val confirmedCount = AtomicInteger(0)

        sockets.forEach { socket ->
            scope.launch {
                val buffer = ByteArray(RECEIVE_BUFFER_SIZE)
                while (currentCoroutineContext().isActive) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    try {
                        socket.receive(packet)
                    } catch (_: Exception) {
                        break
                    }
                    val host = packet.address?.hostAddress ?: continue
                    val message = runCatching {
                        json.decodeFromString<MulticastMessageDto>(
                            String(packet.data, packet.offset, packet.length, Charsets.UTF_8)
                        )
                    }.getOrNull() ?: continue
                    if (message.fingerprint == localInfo.fingerprint) continue
                    if (!inFlight.add(message.fingerprint)) continue
                    scope.launch {
                        try {
                            val remoteProtocol = LanDeviceProtocol.fromWire(message.protocol)
                                ?: return@launch
                            val response = client.register(
                                host = host,
                                port = message.port,
                                protocol = remoteProtocol,
                                payload = localInfo
                            )
                            val device = response.toLanDevice(
                                host = host,
                                port = message.port,
                                protocol = remoteProtocol,
                                lastSeenAt = System.currentTimeMillis()
                            )
                            if (confirmedFingerprints.add(device.fingerprint)) {
                                confirmedCount.incrementAndGet()
                                trySend(device)
                            }
                        } finally {
                            inFlight.remove(message.fingerprint)
                        }
                    }
                }
            }
        }

        if (announce) {
            scope.launch {
                ANNOUNCE_DELAYS.forEach { delayMs ->
                    delay(delayMs)
                    sockets.forEach { socket -> runCatching { socket.sendAnnouncement(localInfo, multicastPort) } }
                }
            }
        }

        scope.launch {
            delay(SUBNET_SCAN_DELAY_MS)
            if (confirmedCount.get() > 0) return@launch
            scanSubnetForDevices(localInfo).forEach { device ->
                if (confirmedFingerprints.add(device.fingerprint)) {
                    confirmedCount.incrementAndGet()
                    trySend(device)
                }
            }
        }

        awaitClose {
            sockets.forEach { runCatching { it.close() } }
            scope.cancel()
            if (multicastLock.isHeld) multicastLock.release()
        }
    }

    private suspend fun scanSubnetForDevices(localInfo: RegisterRequestDto): List<LanDevice> = coroutineScope {
        val interfaces = interfaceProvider.activeIpv4Interfaces()
        val localAddresses = interfaces.map { it.address.hostAddress }.toSet()
        val hosts = interfaces
            .flatMap { localInterface ->
                val address = localInterface.address.hostAddress.orEmpty()
                val parts = address.split('.')
                if (parts.size != 4) return@flatMap emptyList()
                val localSubnet = parts[2].toIntOrNull() ?: return@flatMap emptyList()
                val radius = if (localInterface.prefixLength < 24) NEIGHBOR_SCAN_RADIUS else 0
                val firstSubnet = (localSubnet - radius).coerceAtLeast(0)
                val lastSubnet = (localSubnet + radius).coerceAtMost(255)
                (firstSubnet..lastSubnet).flatMap { subnet ->
                    (1..254).map { host -> "${parts[0]}.${parts[1]}.$subnet.$host" }
                }
            }
            .filterNot { it in localAddresses }
            .distinct()
        val found = mutableListOf<LanDevice>()
        hosts.chunked(SUBNET_SCAN_CONCURRENCY).forEach { chunk ->
            found += chunk
                .map { host -> async(Dispatchers.IO) { probeHost(host, localInfo) } }
                .awaitAll()
                .filterNotNull()
        }
        found.distinctBy { it.fingerprint }
    }

    private suspend fun probeHost(host: String, localInfo: RegisterRequestDto): LanDevice? {
        val https = runCatching {
            withTimeout(DISCOVERY_TIMEOUT_MS) {
                client.register(
                    host = host,
                    port = LocalSendProtocol.DEFAULT_PORT,
                    protocol = LanDeviceProtocol.HTTPS,
                    payload = localInfo,
                    expectedFingerprint = null
                )
            }
        }.getOrNull()
        if (https != null) {
            return https.toLanDevice(
                host = host,
                port = LocalSendProtocol.DEFAULT_PORT,
                protocol = LanDeviceProtocol.HTTPS,
                lastSeenAt = System.currentTimeMillis()
            )
        }
        val http = runCatching {
            withTimeout(DISCOVERY_TIMEOUT_MS) {
                client.register(
                    host = host,
                    port = LocalSendProtocol.DEFAULT_PORT,
                    protocol = LanDeviceProtocol.HTTP,
                    payload = localInfo,
                    expectedFingerprint = null
                )
            }
        }.getOrNull() ?: return null
        return http.toLanDevice(
            host = host,
            port = LocalSendProtocol.DEFAULT_PORT,
            protocol = LanDeviceProtocol.HTTP,
            lastSeenAt = System.currentTimeMillis()
        )
    }

    private suspend fun acquireMulticastLock(): WifiManager.MulticastLock =
        withContext(Dispatchers.IO) {
            val wifiManager = appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            wifiManager.createMulticastLock(MULTICAST_LOCK_TAG).apply {
                setReferenceCounted(false)
                acquire()
            }
        }

    private fun createSockets(multicastPort: Int): List<MulticastSocket> {
        val interfaces = interfaceProvider.activeIpv4Interfaces()
        val sockets = interfaces.mapNotNull { localInterface ->
            runCatching {
                MulticastSocket(null).apply {
                    reuseAddress = true
                    bind(InetSocketAddress(multicastPort))
                    networkInterface = localInterface.networkInterface
                    joinGroup(
                        InetSocketAddress(LocalSendProtocol.MULTICAST_GROUP, multicastPort),
                        localInterface.networkInterface
                    )
                    timeToLive = 1
                }
            }.getOrNull()
        }
        if (sockets.isNotEmpty()) return sockets

        return runCatching {
            listOf(
                MulticastSocket(null).apply {
                    reuseAddress = true
                    bind(InetSocketAddress(multicastPort))
                    joinGroup(InetAddress.getByName(LocalSendProtocol.MULTICAST_GROUP))
                    timeToLive = 1
                }
            )
        }.getOrDefault(emptyList())
    }

    private fun MulticastSocket.sendAnnouncement(
        localInfo: RegisterRequestDto,
        multicastPort: Int
    ) {
        val message = MulticastMessageDto(
            alias = localInfo.alias,
            version = localInfo.version,
            deviceModel = localInfo.deviceModel,
            deviceType = localInfo.deviceType,
            fingerprint = localInfo.fingerprint,
            port = localInfo.port,
            protocol = localInfo.protocol,
            download = localInfo.download
        )
        val payload = json.encodeToString(MulticastMessageDto.serializer(), message).toByteArray()
        send(
            DatagramPacket(
                payload,
                payload.size,
                InetAddress.getByName(LocalSendProtocol.MULTICAST_GROUP),
                multicastPort
            )
        )
    }

    private companion object {
        const val MULTICAST_LOCK_TAG = "koishi:lan_transfer"
        const val RECEIVE_BUFFER_SIZE = 64 * 1024
        val ANNOUNCE_DELAYS = longArrayOf(0, 100, 500, 2000)
        const val SUBNET_SCAN_DELAY_MS = 3_000L
        const val SUBNET_SCAN_CONCURRENCY = 50
        const val NEIGHBOR_SCAN_RADIUS = 1
        const val DISCOVERY_TIMEOUT_MS = 500L
    }
}
