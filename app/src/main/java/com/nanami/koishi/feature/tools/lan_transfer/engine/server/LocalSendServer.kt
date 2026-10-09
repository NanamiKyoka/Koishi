package com.nanami.koishi.feature.tools.lan_transfer.engine.server

import com.nanami.koishi.feature.tools.lan_transfer.engine.model.LanDevice
import com.nanami.koishi.feature.tools.lan_transfer.engine.model.LanDeviceProtocol
import com.nanami.koishi.feature.tools.lan_transfer.engine.model.toLanDevice
import com.nanami.koishi.feature.tools.lan_transfer.engine.protocol.DeviceInfoResponseDto
import com.nanami.koishi.feature.tools.lan_transfer.engine.protocol.ErrorResponseDto
import com.nanami.koishi.feature.tools.lan_transfer.engine.protocol.LocalSendProtocol
import com.nanami.koishi.feature.tools.lan_transfer.engine.protocol.PrepareUploadRequestDto
import com.nanami.koishi.feature.tools.lan_transfer.engine.protocol.PrepareUploadResponseDto
import com.nanami.koishi.feature.tools.lan_transfer.engine.protocol.RegisterRequestDto
import com.nanami.koishi.feature.tools.lan_transfer.engine.protocol.TransferFileDto
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import com.nanami.koishi.feature.tools.lan_transfer.engine.security.LocalSendCertificateFactory
import com.nanami.koishi.feature.tools.lan_transfer.engine.security.LocalSendIdentityMaterial
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.sslConnector
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.plugins.origin
import io.ktor.server.request.receiveChannel
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

data class LocalSendServerConfig(
    val device: DeviceInfoResponseDto,
    val pin: String? = null,
    val verifyChecksums: Boolean = false
)

class LocalSendServer(
    private val config: LocalSendServerConfig,
    private val identity: LocalSendIdentityMaterial,
    private val callbacks: LocalSendServerCallbacks,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
) {
    private val sessionMutex = Mutex()
    private val clientCertificates = LocalSendClientCertificateRegistry()
    private var session: ActiveSession? = null
    private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null
    private var boundPort: Int = 0

    suspend fun start(
        host: String = "0.0.0.0",
        port: Int = LocalSendProtocol.DEFAULT_PORT
    ): Int {
        check(server == null) { "Server is already running" }
        require(config.device.fingerprint.equals(identity.fingerprint, ignoreCase = true)) {
            "Server fingerprint must match the TLS identity"
        }
        val engine = embeddedServer(
            factory = Netty,
            configure = {
                sslConnector(
                    keyStore = identity.keyStore(TLS_PASSWORD.copyOf()),
                    keyAlias = LocalSendIdentityMaterial.KEY_ALIAS,
                    keyStorePassword = { TLS_PASSWORD.copyOf() },
                    privateKeyPassword = { TLS_PASSWORD.copyOf() }
                ) {
                    this.host = host
                    this.port = port
                }
                enableHttp2 = false
                channelPipelineConfig = localSendTlsPipelineConfig(identity, clientCertificates)
            },
            module = { installRoutes() }
        )
        return try {
            engine.start(wait = false)
            server = engine
            boundPort = engine.engine.resolvedConnectors().firstOrNull()?.port ?: port
            boundPort
        } catch (error: Exception) {
            runCatching { engine.stop(gracePeriodMillis = 0, timeoutMillis = 500) }
            server = null
            boundPort = 0
            throw error
        }
    }

    suspend fun stop() {
        val engine = server ?: return
        server = null
        boundPort = 0
        sessionMutex.withLock { session = null }
        engine.stop(gracePeriodMillis = 500, timeoutMillis = 2_000)
    }

    val port: Int
        get() = boundPort

    private fun Application.installRoutes() {
        routing {
            get("${LocalSendProtocol.API_PREFIX}/info") {
                call.respondJson(config.device)
            }

            post("${LocalSendProtocol.API_PREFIX}/register") {
                try {
                    val request = call.receiveProtocolJson<RegisterRequestDto>()
                    val protocol = LanDeviceProtocol.fromWire(request.protocol)
                        ?: return@post call.respondError(HttpStatusCode.BadRequest, "Invalid protocol")
                    call.requireVerifiedFingerprint(request.fingerprint)
                    val peer = request.toPeer(remoteHost = call.requestOriginHost(), protocol = protocol)
                    callbacks.onRegistered(peer)
                    call.respondJson(config.device)
                } catch (error: ClientCertificateException) {
                    call.respondError(HttpStatusCode.Unauthorized, error.message ?: "Invalid client certificate")
                } catch (error: Exception) {
                    call.respondError(HttpStatusCode.InternalServerError, error.message ?: "Register failed")
                }
            }

            post("${LocalSendProtocol.API_PREFIX}/prepare-upload") {
                try {
                    call.respondPrepareUpload()
                } catch (error: ClientCertificateException) {
                    call.respondError(HttpStatusCode.Unauthorized, error.message ?: "Invalid client certificate")
                } catch (error: Exception) {
                    call.respondError(HttpStatusCode.InternalServerError, error.message ?: "Prepare upload failed")
                }
            }

            post("${LocalSendProtocol.API_PREFIX}/upload") {
                try {
                    call.respondUpload()
                } catch (error: ClientCertificateException) {
                    call.respondError(HttpStatusCode.Unauthorized, error.message ?: "Invalid client certificate")
                } catch (error: Exception) {
                    call.respondError(HttpStatusCode.InternalServerError, error.message ?: "Upload failed")
                }
            }

            post("${LocalSendProtocol.API_PREFIX}/cancel") {
                try {
                    call.cancelSession()
                } catch (error: ClientCertificateException) {
                    call.respondError(HttpStatusCode.Unauthorized, error.message ?: "Invalid client certificate")
                }
            }
        }
    }

    private suspend fun ApplicationCall.respondPrepareUpload() {
        if (!isPinValid()) {
            return respondError(HttpStatusCode.Unauthorized, "Invalid PIN")
        }
        val request = receiveProtocolJson<PrepareUploadRequestDto>()
        val protocol = LanDeviceProtocol.fromWire(request.info.protocol)
            ?: return respondError(HttpStatusCode.BadRequest, "Invalid protocol")
        requireVerifiedFingerprint(request.info.fingerprint)
        val peer = request.info.toPeer(requestOriginHost(), protocol)
        val sessionId = UUID.randomUUID().toString()

        val alreadyActive = sessionMutex.withLock { session != null }
        if (alreadyActive) {
            return respondError(HttpStatusCode.Conflict, "Another session is active")
        }

        val decision = callbacks.onUploadRequested(
            IncomingUploadRequest(
                sessionId = sessionId,
                peer = peer,
                files = request.files
            )
        )
        val acceptedIds = when (decision) {
            IncomingUploadDecision.Reject -> return respondError(HttpStatusCode.Forbidden, "Rejected")
            is IncomingUploadDecision.Accept -> decision.acceptedFileIds.intersect(request.files.keys)
        }
        if (acceptedIds.isEmpty()) {
            respond(HttpStatusCode.NoContent)
            return
        }

        val tokens = acceptedIds.associateWith { UUID.randomUUID().toString() }
        val active = ActiveSession(
            sessionId = sessionId,
            peerHost = peer.host,
            peer = peer,
            files = request.files
                .filterKeys { acceptedIds.contains(it) }
                .mapValues { (_, file) -> SessionFile(file, tokens.getValue(file.id), FileStatus.PENDING) }
        )
        val reserved = sessionMutex.withLock {
            if (session != null) false else {
                session = active
                true
            }
        }
        if (!reserved) {
            return respondError(HttpStatusCode.Conflict, "Another session is active")
        }

        respondJson(
            PrepareUploadResponseDto(
                sessionId = sessionId,
                files = tokens
            )
        )
    }

    private suspend fun ApplicationCall.respondUpload() {
        val sessionId = request.queryParameters["sessionId"]
        val fileId = request.queryParameters["fileId"]
        val token = request.queryParameters["token"]
        if (sessionId == null || fileId == null || token == null) {
            return respondError(HttpStatusCode.BadRequest, "Missing upload parameters")
        }

        val fileState = sessionMutex.withLock {
            val active = session
            if (active == null || active.sessionId != sessionId || active.peerHost != requestOriginHost()) {
                return@withLock null
            }
            val current = active.files[fileId]
            if (current == null || current.token != token || current.status == FileStatus.FINISHED) {
                return@withLock null
            }
            val updated = current.copy(status = FileStatus.IN_PROGRESS)
            session = active.copy(files = active.files + (fileId to updated))
            UploadState(updated, active.cancellation)
        } ?: return respondError(HttpStatusCode.Forbidden, "Invalid upload token")

        val peer = activePeer(sessionId) ?: return respondError(HttpStatusCode.Conflict, "Session ended")
        requireVerifiedFingerprint(peer.fingerprint)
        if (fileState.cancellation.cancelled.get()) {
            return respondError(HttpStatusCode.Conflict, "Upload cancelled")
        }
        val output = try {
            callbacks.openFileTarget(
                IncomingFileUpload(
                    sessionId = sessionId,
                    peer = peer,
                    file = fileState.file.file
                )
            )
        } catch (error: Exception) {
            val message = error.message ?: "Unable to open destination"
            markFileFinished(sessionId, fileId, message)
            callbacks.onFileFinished(
                IncomingFileResult(
                    sessionId = sessionId,
                    peer = peer,
                    file = fileState.file.file,
                    bytesWritten = 0,
                    success = false,
                    errorMessage = message
                )
            )
            return respondError(HttpStatusCode.InternalServerError, message)
        }

        var written = 0L
        var errorMessage: String? = null
        val digest = MessageDigest.getInstance("SHA-256")
        val channel = this.receiveChannel()
        try {
            output.use { target ->
                val buffer = ByteArray(LocalSendProtocol.UPLOAD_CHUNK_SIZE)
                while (true) {
                    if (fileState.cancellation.cancelled.get()) {
                        errorMessage = "Upload cancelled"
                        break
                    }
                    val read = channel.readAvailable(buffer, 0, buffer.size)
                    if (read < 0) break
                    if (read == 0) continue
                    target.write(buffer, 0, read)
                    digest.update(buffer, 0, read)
                    written += read
                    if (written > fileState.file.file.size) {
                        errorMessage = "Received more bytes than expected"
                        break
                    }
                }
                target.flush()
            }
            if (errorMessage == null && written != fileState.file.file.size) {
                errorMessage = "Expected ${fileState.file.file.size} bytes, received $written"
            }
            val expectedHash = fileState.file.file.sha256
            if (errorMessage == null && config.verifyChecksums && !expectedHash.isNullOrBlank()) {
                val actualHash = digest.digest().joinToString("") { byte -> "%02x".format(byte) }
                if (!actualHash.equals(expectedHash, ignoreCase = true)) {
                    errorMessage = "Checksum mismatch"
                }
            }
        } catch (error: Exception) {
            errorMessage = error.message ?: "Failed to save file"
        }

        markFileFinished(sessionId, fileId, errorMessage)
        callbacks.onFileFinished(
            IncomingFileResult(
                sessionId = sessionId,
                peer = peer,
                file = fileState.file.file,
                bytesWritten = written,
                success = errorMessage == null,
                errorMessage = errorMessage
            )
        )
        if (errorMessage == null) {
            respond(HttpStatusCode.OK)
        } else {
            respondError(HttpStatusCode.InternalServerError, errorMessage)
        }
    }

    private suspend fun ApplicationCall.cancelSession() {
        val sessionId = request.queryParameters["sessionId"] ?: return respondError(
            HttpStatusCode.BadRequest,
            "Missing session ID"
        )
        val sourceHost = requestOriginHost()
        val sourceFingerprint = clientCertificateFingerprint()
        cancelSessionInternal(sessionId, sourceHost, sourceFingerprint)
        respond(HttpStatusCode.OK)
    }

    suspend fun cancelSession(sessionId: String): LanDevice? =
        cancelSessionInternal(sessionId, sourceHost = null, sourceFingerprint = null)

    private suspend fun cancelSessionInternal(
        sessionId: String,
        sourceHost: String?,
        sourceFingerprint: String?
    ): LanDevice? {
        var peer: LanDevice? = null
        sessionMutex.withLock {
            val active = session
            val hostMatches = sourceHost == null || active?.peerHost == sourceHost
            val fingerprintMatches = sourceFingerprint == null ||
                active?.peer?.fingerprint.equals(sourceFingerprint, ignoreCase = true)
            if (active != null && active.sessionId == sessionId && hostMatches && fingerprintMatches) {
                active.cancellation.cancelled.set(true)
                session = null
                peer = active.peer
            }
        }
        if (peer != null) callbacks.onSessionCancelled(sessionId)
        return peer
    }

    private suspend fun ApplicationCall.markFileFinished(
        sessionId: String,
        fileId: String,
        errorMessage: String?
    ) {
        sessionMutex.withLock {
            val active = session ?: return@withLock
            if (active.sessionId != sessionId) return@withLock
            val updatedFiles = active.files.mapValues { (id, file) ->
                if (id == fileId) {
                    file.copy(status = if (errorMessage == null) FileStatus.FINISHED else FileStatus.FAILED)
                } else {
                    file
                }
            }
            session = if (updatedFiles.values.all { it.status == FileStatus.FINISHED }) {
                null
            } else {
                active.copy(files = updatedFiles)
            }
        }
    }

    private suspend fun activePeer(sessionId: String): LanDevice? =
        sessionMutex.withLock { session?.takeIf { it.sessionId == sessionId }?.peer }

    private suspend fun ApplicationCall.respondJson(value: Any? = null, status: HttpStatusCode = HttpStatusCode.OK) {
        if (value == null) return respond(status)
        val payload = when (value) {
            is DeviceInfoResponseDto -> json.encodeToString(DeviceInfoResponseDto.serializer(), value)
            is PrepareUploadResponseDto -> json.encodeToString(PrepareUploadResponseDto.serializer(), value)
            else -> error("Unsupported response type")
        }
        respondText(payload, ContentType.Application.Json, status)
    }

    private suspend fun ApplicationCall.respondError(status: HttpStatusCode, message: String) {
        val payload = json.encodeToString(ErrorResponseDto.serializer(), ErrorResponseDto(message))
        respondText(payload, ContentType.Application.Json, status)
    }

    private suspend inline fun <reified T> ApplicationCall.receiveProtocolJson(): T =
        json.decodeFromString<T>(receiveText())

    private fun ApplicationCall.requestOriginHost(): String = request.origin.remoteHost

    private fun ApplicationCall.clientCertificateFingerprint(): String {
        val origin = request.origin
        val certificate = clientCertificates.find(origin.remoteHost, origin.remotePort)
            ?: throw ClientCertificateException("Client certificate is missing")
        return LocalSendCertificateFactory.fingerprint(certificate)
    }

    private fun ApplicationCall.requireVerifiedFingerprint(claimedFingerprint: String): String {
        val actual = clientCertificateFingerprint()
        if (!actual.equals(claimedFingerprint, ignoreCase = true)) {
            throw ClientCertificateException("Client fingerprint does not match its certificate")
        }
        return actual
    }

    private fun ApplicationCall.isPinValid(): Boolean = config.pin.isNullOrBlank() || request.queryParameters["pin"] == config.pin

    private fun RegisterRequestDto.toPeer(remoteHost: String, protocol: LanDeviceProtocol): LanDevice =
        DeviceInfoResponseDto(
            alias = alias,
            version = version,
            deviceModel = deviceModel,
            deviceType = deviceType,
            fingerprint = fingerprint,
            download = download
        ).toLanDevice(
            host = remoteHost,
            port = port,
            protocol = protocol,
            lastSeenAt = System.currentTimeMillis()
        )

    private data class ActiveSession(
        val sessionId: String,
        val peerHost: String,
        val peer: LanDevice,
        val files: Map<String, SessionFile>,
        val cancellation: SessionCancellation = SessionCancellation()
    )

    private data class SessionFile(
        val file: TransferFileDto,
        val token: String,
        val status: FileStatus
    )

    private data class UploadState(
        val file: SessionFile,
        val cancellation: SessionCancellation
    )

    private class SessionCancellation {
        val cancelled = AtomicBoolean(false)
    }

    private enum class FileStatus {
        PENDING,
        IN_PROGRESS,
        FINISHED,
        FAILED
    }

    private companion object {
        val TLS_PASSWORD = "localsend".toCharArray()
    }
}

private class ClientCertificateException(message: String) : IllegalStateException(message)
