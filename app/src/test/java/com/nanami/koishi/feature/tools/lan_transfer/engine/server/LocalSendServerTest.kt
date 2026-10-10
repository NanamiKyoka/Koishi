package com.nanami.koishi.feature.tools.lan_transfer.engine.server

import com.nanami.koishi.feature.tools.lan_transfer.engine.model.LanDevice
import com.nanami.koishi.feature.tools.lan_transfer.engine.model.LanDeviceProtocol
import com.nanami.koishi.feature.tools.lan_transfer.engine.protocol.DeviceInfoResponseDto
import com.nanami.koishi.feature.tools.lan_transfer.engine.protocol.LocalSendClient
import com.nanami.koishi.feature.tools.lan_transfer.engine.protocol.LocalSendProtocolException
import com.nanami.koishi.feature.tools.lan_transfer.engine.protocol.LocalSendUploadSource
import com.nanami.koishi.feature.tools.lan_transfer.engine.protocol.PrepareUploadRequestDto
import com.nanami.koishi.feature.tools.lan_transfer.engine.protocol.RegisterRequestDto
import com.nanami.koishi.feature.tools.lan_transfer.engine.protocol.TransferFileDto
import com.nanami.koishi.feature.tools.lan_transfer.engine.security.LocalSendCertificateFactory
import com.nanami.koishi.feature.tools.lan_transfer.engine.security.LocalSendIdentityMaterial
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

class LocalSendServerTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val serverIdentity = LocalSendCertificateFactory.create()
    private val clientIdentity = LocalSendCertificateFactory.create()

    @Test
    fun servesInfoRegisterPrepareAndUploadOverMutualTls() = runTest {
        val callbacks = RecordingCallbacks()
        val server = createServer(callbacks, verifyChecksums = true)
        val port = server.start(host = "127.0.0.1", port = 0)
        try {
            val client = LocalSendClient(clientIdentity)
            val info = client.info(
                host = "127.0.0.1",
                port = port,
                protocol = LanDeviceProtocol.HTTPS,
                expectedFingerprint = serverIdentity.fingerprint
            )
            assertEquals("Koishi", info.alias)
            assertEquals(serverIdentity.fingerprint, info.fingerprint)

            val registerRequest = clientRegisterRequest(port)
            val register = client.register(
                host = "127.0.0.1",
                port = port,
                protocol = LanDeviceProtocol.HTTPS,
                payload = registerRequest,
                expectedFingerprint = serverIdentity.fingerprint
            )
            assertEquals("Koishi", register.alias)
            assertEquals(clientIdentity.fingerprint, callbacks.registered.single().fingerprint)

            val content = "local send payload".toByteArray()
            val file = TransferFileDto(
                id = "file-1",
                fileName = "hello.txt",
                size = content.size.toLong(),
                fileType = "text/plain",
                sha256 = content.sha256()
            )
            val prepareRequest = PrepareUploadRequestDto(
                info = registerRequest,
                files = mapOf(file.id to file)
            )
            val prepareResponse = client.prepareUpload(
                host = "127.0.0.1",
                port = port,
                protocol = LanDeviceProtocol.HTTPS,
                payload = prepareRequest,
                expectedFingerprint = serverIdentity.fingerprint
            ) ?: error("Upload was not accepted")
            val token = prepareResponse.files.getValue(file.id)

            client.upload(
                host = "127.0.0.1",
                port = port,
                protocol = LanDeviceProtocol.HTTPS,
                sessionId = prepareResponse.sessionId,
                fileId = file.id,
                token = token,
                source = ByteArrayUploadSource(content),
                expectedFingerprint = serverIdentity.fingerprint
            )
            assertEquals("local send payload", callbacks.outputs.getValue(file.id).toString(Charsets.UTF_8))
            assertTrue(callbacks.results.single().success)
        } finally {
            server.stop()
        }
    }

    @Test
    fun acceptsTextMessageWithoutUploadingAFile() = runTest {
        val callbacks = RecordingCallbacks().apply {
            decision = { IncomingUploadDecision.Accept(emptySet()) }
        }
        val server = createServer(callbacks)
        val port = server.start(host = "127.0.0.1", port = 0)
        try {
            val client = LocalSendClient(clientIdentity)
            val message = TransferFileDto(
                id = "message",
                fileName = "message.txt",
                size = 5,
                fileType = "text/plain",
                preview = "hello"
            )

            val response = client.prepareUpload(
                host = "127.0.0.1",
                port = port,
                protocol = LanDeviceProtocol.HTTPS,
                payload = PrepareUploadRequestDto(
                    info = clientRegisterRequest(port),
                    files = mapOf(message.id to message)
                ),
                expectedFingerprint = serverIdentity.fingerprint
            )

            assertNull(response)
            assertTrue(callbacks.outputs.isEmpty())
        } finally {
            server.stop()
        }
    }

    @Test
    fun retriesFailedFileInSameSession() = runTest {
        val callbacks = RecordingCallbacks()
        val server = createServer(callbacks, verifyChecksums = true)
        val port = server.start(host = "127.0.0.1", port = 0)
        try {
            val client = LocalSendClient(clientIdentity)
            val expectedContent = "correct payload".toByteArray()
            val file = TransferFileDto(
                id = "file-retry",
                fileName = "retry.txt",
                size = expectedContent.size.toLong(),
                fileType = "text/plain",
                sha256 = expectedContent.sha256()
            )
            val response = client.prepareUpload(
                host = "127.0.0.1",
                port = port,
                protocol = LanDeviceProtocol.HTTPS,
                payload = PrepareUploadRequestDto(
                    info = clientRegisterRequest(port),
                    files = mapOf(file.id to file)
                ),
                expectedFingerprint = serverIdentity.fingerprint
            ) ?: error("Upload was not accepted")
            val token = response.files.getValue(file.id)

            val firstAttempt = runCatching {
                client.upload(
                    host = "127.0.0.1",
                    port = port,
                    protocol = LanDeviceProtocol.HTTPS,
                    sessionId = response.sessionId,
                    fileId = file.id,
                    token = token,
                    source = ByteArrayUploadSource("wrong payload!!".toByteArray()),
                    expectedFingerprint = serverIdentity.fingerprint
                )
            }
            assertEquals(500, (firstAttempt.exceptionOrNull() as LocalSendProtocolException).statusCode)

            client.upload(
                host = "127.0.0.1",
                port = port,
                protocol = LanDeviceProtocol.HTTPS,
                sessionId = response.sessionId,
                fileId = file.id,
                token = token,
                source = ByteArrayUploadSource(expectedContent),
                expectedFingerprint = serverIdentity.fingerprint
            )
            assertEquals("correct payload", callbacks.outputs.getValue(file.id).toString(Charsets.UTF_8))
            assertTrue(callbacks.results.last().success)
        } finally {
            server.stop()
        }
    }

    @Test
    fun rejectsPrepareWhenPinDoesNotMatch() = runTest {
        val callbacks = RecordingCallbacks()
        val server = createServer(callbacks, pin = "2468")
        val port = server.start(host = "127.0.0.1", port = 0)
        try {
            val client = LocalSendClient(clientIdentity)
            val response = runCatching {
                client.prepareUpload(
                    host = "127.0.0.1",
                    port = port,
                    protocol = LanDeviceProtocol.HTTPS,
                    payload = PrepareUploadRequestDto(
                        info = clientRegisterRequest(port),
                        files = emptyMap()
                    ),
                    pin = "0000",
                    expectedFingerprint = serverIdentity.fingerprint
                )
            }

            val error = response.exceptionOrNull() as LocalSendProtocolException
            assertEquals(401, error.statusCode)
        } finally {
            server.stop()
        }
    }

    @Test
    fun rejectsClaimedFingerprintThatDoesNotMatchClientCertificate() = runTest {
        val callbacks = RecordingCallbacks()
        val server = createServer(callbacks)
        val port = server.start(host = "127.0.0.1", port = 0)
        try {
            val client = LocalSendClient(clientIdentity)
            val response = runCatching {
                client.register(
                    host = "127.0.0.1",
                    port = port,
                    protocol = LanDeviceProtocol.HTTPS,
                    payload = clientRegisterRequest(port).copy(fingerprint = "WRONG"),
                    expectedFingerprint = serverIdentity.fingerprint
                )
            }

            val error = response.exceptionOrNull() as LocalSendProtocolException
            assertEquals(401, error.statusCode)
            assertTrue(callbacks.registered.isEmpty())
        } finally {
            server.stop()
        }
    }

    @Test
    fun rejectsServerCertificateWithWrongPinnedFingerprint() = runTest {
        val server = createServer(RecordingCallbacks())
        val port = server.start(host = "127.0.0.1", port = 0)
        try {
            val client = LocalSendClient(clientIdentity)
            val response = runCatching {
                client.info(
                    host = "127.0.0.1",
                    port = port,
                    protocol = LanDeviceProtocol.HTTPS,
                    expectedFingerprint = "00".repeat(32)
                )
            }

            assertTrue(response.exceptionOrNull() is IOException)
        } finally {
            server.stop()
        }
    }

    private fun createServer(
        callbacks: RecordingCallbacks,
        pin: String? = null,
        verifyChecksums: Boolean = false
    ): LocalSendServer = LocalSendServer(
        config = LocalSendServerConfig(
            device = DeviceInfoResponseDto(
                alias = "Koishi",
                version = "2.2",
                fingerprint = serverIdentity.fingerprint,
                download = false
            ),
            pin = pin,
            verifyChecksums = verifyChecksums
        ),
        identity = serverIdentity,
        callbacks = callbacks,
        json = json
    )

    private fun clientRegisterRequest(port: Int): RegisterRequestDto = RegisterRequestDto(
        alias = "Remote",
        version = "2.2",
        fingerprint = clientIdentity.fingerprint,
        port = port,
        protocol = "https"
    )

    private fun ByteArray.sha256(): String =
        MessageDigest.getInstance("SHA-256")
            .digest(this)
            .joinToString("") { byte -> "%02x".format(byte) }

    private class ByteArrayUploadSource(
        private val data: ByteArray,
        override val contentType: String = "application/octet-stream"
    ) : LocalSendUploadSource {
        override val size: Long = data.size.toLong()

        override fun openStream() = ByteArrayInputStream(data)
    }

    private class RecordingCallbacks : LocalSendServerCallbacks {
        val registered = mutableListOf<LanDevice>()
        val outputs = ConcurrentHashMap<String, ByteArrayOutputStream>()
        val results = mutableListOf<IncomingFileResult>()
        var decision: (IncomingUploadRequest) -> IncomingUploadDecision =
            { request -> IncomingUploadDecision.Accept(request.files.keys) }

        override suspend fun onRegistered(peer: LanDevice) {
            registered += peer
        }

        override suspend fun onUploadRequested(request: IncomingUploadRequest): IncomingUploadDecision =
            decision(request)

        override suspend fun openFileTarget(request: IncomingFileUpload): OutputStream =
            ByteArrayOutputStream().also { outputs[request.file.id] = it }

        override suspend fun onFileFinished(result: IncomingFileResult) {
            results += result
        }
    }
}
