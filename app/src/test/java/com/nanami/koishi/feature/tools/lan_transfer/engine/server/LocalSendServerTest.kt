package com.nanami.koishi.feature.tools.lan_transfer.engine.server

import com.nanami.koishi.feature.tools.lan_transfer.engine.model.LanDevice
import com.nanami.koishi.feature.tools.lan_transfer.engine.protocol.DeviceInfoResponseDto
import com.nanami.koishi.feature.tools.lan_transfer.engine.protocol.PrepareUploadRequestDto
import com.nanami.koishi.feature.tools.lan_transfer.engine.protocol.PrepareUploadResponseDto
import com.nanami.koishi.feature.tools.lan_transfer.engine.protocol.RegisterRequestDto
import com.nanami.koishi.feature.tools.lan_transfer.engine.protocol.TransferFileDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

class LocalSendServerTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun servesInfoRegisterPrepareAndUpload() = runTest {
        val callbacks = RecordingCallbacks()
        val server = LocalSendServer(
            config = LocalSendServerConfig(
                device = DeviceInfoResponseDto(
                    alias = "Koishi",
                    version = "2.2",
                    fingerprint = "SERVER123",
                    download = false
                ),
                verifyChecksums = true
            ),
            callbacks = callbacks,
            json = json
        )
        val port = server.start(host = "127.0.0.1", port = 0)
        try {
            val info = get("http://127.0.0.1:$port/api/localsend/v2/info")
            assertEquals(200, info.first)
            assertEquals("Koishi", json.decodeFromString<DeviceInfoResponseDto>(info.second).alias)

            val registerRequest = RegisterRequestDto(
                alias = "Remote",
                version = "2.2",
                fingerprint = "REMOTE123",
                port = port,
                protocol = "http"
            )
            val register = post(
                "http://127.0.0.1:$port/api/localsend/v2/register",
                json.encodeToString(RegisterRequestDto.serializer(), registerRequest)
            )
            assertEquals(200, register.first)
            assertEquals("REMOTE123", callbacks.registered.single().fingerprint)

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
            val prepare = post(
                "http://127.0.0.1:$port/api/localsend/v2/prepare-upload",
                json.encodeToString(PrepareUploadRequestDto.serializer(), prepareRequest)
            )
            assertEquals(200, prepare.first)
            val prepareResponse = json.decodeFromString<PrepareUploadResponseDto>(prepare.second)
            val token = prepareResponse.files.getValue(file.id)

            val upload = post(
                "http://127.0.0.1:$port/api/localsend/v2/upload" +
                    "?sessionId=${prepareResponse.sessionId}&fileId=${file.id}&token=$token",
                content
            )
            assertEquals(200, upload.first)
            assertEquals("local send payload", callbacks.outputs.getValue(file.id).toString(Charsets.UTF_8))
            assertTrue(callbacks.results.single().success)
        } finally {
            server.stop()
        }
    }

    @Test
    fun rejectsPrepareWhenPinDoesNotMatch() = runTest {
        val callbacks = RecordingCallbacks()
        val server = LocalSendServer(
            config = LocalSendServerConfig(
                device = DeviceInfoResponseDto(
                    alias = "Koishi",
                    version = "2.2",
                    fingerprint = "SERVER123"
                ),
                pin = "2468"
            ),
            callbacks = callbacks,
            json = json
        )
        val port = server.start(host = "127.0.0.1", port = 0)
        try {
            val request = PrepareUploadRequestDto(
                info = RegisterRequestDto(
                    alias = "Remote",
                    version = "2.2",
                    fingerprint = "REMOTE123",
                    port = port,
                    protocol = "http"
                ),
                files = emptyMap()
            )
            val response = post(
                "http://127.0.0.1:$port/api/localsend/v2/prepare-upload?pin=0000",
                json.encodeToString(PrepareUploadRequestDto.serializer(), request)
            )

            assertEquals(401, response.first)
        } finally {
            server.stop()
        }
    }

    private suspend fun get(url: String): Pair<Int, String> = execute(
        Request.Builder().url(url).get().build()
    )

    private suspend fun post(url: String, body: String): Pair<Int, String> = execute(
        Request.Builder()
            .url(url)
            .post(body.toByteArray().toRequestBody("application/json".toMediaType()))
            .build()
    )

    private suspend fun post(url: String, body: ByteArray): Pair<Int, String> = execute(
        Request.Builder()
            .url(url)
            .post(body.toRequestBody("application/octet-stream".toMediaType()))
            .build()
    )

    private suspend fun execute(request: Request): Pair<Int, String> =
        withContext(Dispatchers.IO) {
            OkHttpClient().newCall(request).execute().use { response ->
                response.code to response.body.orEmpty()
            }
        }

    private fun okhttp3.ResponseBody?.orEmpty(): String = this?.string().orEmpty()

    private fun ByteArray.sha256(): String =
        MessageDigest.getInstance("SHA-256")
            .digest(this)
            .joinToString("") { byte -> "%02x".format(byte) }

    private class RecordingCallbacks : LocalSendServerCallbacks {
        val registered = mutableListOf<LanDevice>()
        val outputs = ConcurrentHashMap<String, ByteArrayOutputStream>()
        val results = mutableListOf<IncomingFileResult>()

        override suspend fun onRegistered(peer: LanDevice) {
            registered += peer
        }

        override suspend fun onUploadRequested(request: IncomingUploadRequest): IncomingUploadDecision =
            IncomingUploadDecision.Accept(request.files.keys)

        override suspend fun openFileTarget(request: IncomingFileUpload): OutputStream =
            ByteArrayOutputStream().also { outputs[request.file.id] = it }

        override suspend fun onFileFinished(result: IncomingFileResult) {
            results += result
        }
    }
}
