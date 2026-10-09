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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
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

class DeviceDiscovery(
    context: Context,
    private val deviceInfoProvider: LocalDeviceInfoProvider,
    private val client: LocalSendRegisterClient,
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val interfaceProvider: NetworkInterfaceProvider = NetworkInterfaceProvider()
) {
    private val appContext = context.applicationContext

    fun discover(
        alias: String,
        port: Int = LocalSendProtocol.DEFAULT_PORT,
        protocol: LanDeviceProtocol = LanDeviceProtocol.HTTPS
    ): Flow<LanDevice> = callbackFlow {
        val localInfo = deviceInfoProvider.create(alias, port, protocol)
        val multicastLock = acquireMulticastLock()
        val sockets = createSockets(port)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val inFlight = ConcurrentHashMap.newKeySet<String>()

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
                            trySend(
                                response.toLanDevice(
                                    host = host,
                                    port = message.port,
                                    protocol = remoteProtocol,
                                    lastSeenAt = System.currentTimeMillis()
                                )
                            )
                        } finally {
                            inFlight.remove(message.fingerprint)
                        }
                    }
                }
            }
        }

        scope.launch {
            ANNOUNCE_DELAYS.forEach { delayMs ->
                delay(delayMs)
                sockets.forEach { socket -> runCatching { socket.sendAnnouncement(localInfo, port) } }
            }
        }

        awaitClose {
            sockets.forEach { runCatching { it.close() } }
            scope.cancel()
            if (multicastLock.isHeld) multicastLock.release()
        }
    }

    private suspend fun acquireMulticastLock(): WifiManager.MulticastLock =
        withContext(Dispatchers.IO) {
            val wifiManager = appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            wifiManager.createMulticastLock(MULTICAST_LOCK_TAG).apply {
                setReferenceCounted(false)
                acquire()
            }
        }

    private fun createSockets(port: Int): List<MulticastSocket> {
        val interfaces = interfaceProvider.activeIpv4Interfaces()
        val sockets = interfaces.mapNotNull { localInterface ->
            runCatching {
                MulticastSocket(null).apply {
                    reuseAddress = true
                    bind(InetSocketAddress(port))
                    networkInterface = localInterface.networkInterface
                    joinGroup(
                        InetSocketAddress(LocalSendProtocol.MULTICAST_GROUP, port),
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
                    bind(InetSocketAddress(port))
                    joinGroup(InetAddress.getByName(LocalSendProtocol.MULTICAST_GROUP))
                    timeToLive = 1
                }
            )
        }.getOrDefault(emptyList())
    }

    private fun MulticastSocket.sendAnnouncement(
        localInfo: RegisterRequestDto,
        port: Int
    ) {
        val message = MulticastMessageDto(
            alias = localInfo.alias,
            version = localInfo.version,
            deviceModel = localInfo.deviceModel,
            deviceType = localInfo.deviceType,
            fingerprint = localInfo.fingerprint,
            port = port,
            protocol = localInfo.protocol,
            download = localInfo.download
        )
        val payload = json.encodeToString(MulticastMessageDto.serializer(), message).toByteArray()
        send(
            DatagramPacket(
                payload,
                payload.size,
                InetAddress.getByName(LocalSendProtocol.MULTICAST_GROUP),
                port
            )
        )
    }

    private companion object {
        const val MULTICAST_LOCK_TAG = "koishi:lan_transfer"
        const val RECEIVE_BUFFER_SIZE = 64 * 1024
        val ANNOUNCE_DELAYS = longArrayOf(0, 100, 500, 2000)
    }
}
