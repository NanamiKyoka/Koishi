package com.nanami.koishi.feature.tools.lan_transfer.engine.protocol

import com.nanami.koishi.feature.tools.lan_transfer.engine.model.LanDeviceProtocol
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit
import okio.BufferedSink
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

interface LocalSendRegisterClient {
    suspend fun register(
        host: String,
        port: Int,
        protocol: LanDeviceProtocol,
        payload: RegisterRequestDto
    ): DeviceInfoResponseDto
}

class LocalSendClient(
    private val httpClient: OkHttpClient = defaultHttpClient(),
    private val json: Json = protocolJson()
) : LocalSendRegisterClient {

    override suspend fun register(
        host: String,
        port: Int,
        protocol: LanDeviceProtocol,
        payload: RegisterRequestDto
    ): DeviceInfoResponseDto {
        val url = LocalSendEndpoint.REGISTER.url(host, port, protocol.wireValue)
            ?: throw LocalSendProtocolException(message = "Invalid device address")
        val body = json.encodeToString(RegisterRequestDto.serializer(), payload)
            .toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()
        return executeJson(request)
    }

    suspend fun info(
        host: String,
        port: Int,
        protocol: LanDeviceProtocol
    ): DeviceInfoResponseDto {
        val url = LocalSendEndpoint.INFO.url(host, port, protocol.wireValue)
            ?: throw LocalSendProtocolException(message = "Invalid device address")
        return executeJson(Request.Builder().url(url).get().build())
    }

    suspend fun prepareUpload(
        host: String,
        port: Int,
        protocol: LanDeviceProtocol,
        payload: PrepareUploadRequestDto,
        pin: String? = null
    ): PrepareUploadResponseDto? {
        val baseUrl = LocalSendEndpoint.PREPARE_UPLOAD.url(host, port, protocol.wireValue)
            ?: throw LocalSendProtocolException(message = "Invalid device address")
        val url = baseUrl.newBuilder()
            .apply { pin?.takeIf { it.isNotBlank() }?.let { addQueryParameter("pin", it) } }
            .build()
        val body = json.encodeToString(PrepareUploadRequestDto.serializer(), payload)
            .toRequestBody(JSON_MEDIA_TYPE)
        execute(Request.Builder().url(url).post(body).build()).use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (response.code == 204) return null
            if (!response.isSuccessful) {
                throw LocalSendProtocolException(
                    statusCode = response.code,
                    message = decodeError(responseBody) ?: response.message
                )
            }
            return json.decodeFromString<PrepareUploadResponseDto>(responseBody)
        }
    }

    suspend fun upload(
        host: String,
        port: Int,
        protocol: LanDeviceProtocol,
        sessionId: String,
        fileId: String,
        token: String,
        source: LocalSendUploadSource,
        onProgress: (sentBytes: Long) -> Unit = {}
    ) {
        val baseUrl = LocalSendEndpoint.UPLOAD.url(host, port, protocol.wireValue)
            ?: throw LocalSendProtocolException(message = "Invalid device address")
        val url = baseUrl.newBuilder()
            .addQueryParameter("sessionId", sessionId)
            .addQueryParameter("fileId", fileId)
            .addQueryParameter("token", token)
            .build()
        val body = object : RequestBody() {
            override fun contentType() = source.contentType.toMediaType()

            override fun contentLength(): Long = source.size

            override fun writeTo(sink: BufferedSink) {
                source.openStream().use { input ->
                    val buffer = ByteArray(LocalSendProtocol.UPLOAD_CHUNK_SIZE)
                    var sent = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        if (read == 0) continue
                        sink.write(buffer, 0, read)
                        sent += read
                        onProgress(sent)
                    }
                }
            }
        }
        execute(Request.Builder().url(url).post(body).build()).use { response ->
            ensureSuccessful(response)
        }
    }

    suspend fun cancel(
        host: String,
        port: Int,
        protocol: LanDeviceProtocol,
        sessionId: String
    ) {
        val baseUrl = LocalSendEndpoint.CANCEL.url(host, port, protocol.wireValue)
            ?: throw LocalSendProtocolException(message = "Invalid device address")
        val url = baseUrl.newBuilder()
            .addQueryParameter("sessionId", sessionId)
            .build()
        execute(Request.Builder().url(url).post(EMPTY_BODY).build()).use { response ->
            ensureSuccessful(response)
        }
    }

    private suspend fun ensureSuccessful(response: Response) {
        if (response.isSuccessful) return
        val responseBody = response.body?.string().orEmpty()
        throw LocalSendProtocolException(
            statusCode = response.code,
            message = decodeError(responseBody) ?: response.message
        )
    }

    private suspend inline fun <reified T> executeJson(request: Request): T =
        execute(request).use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw LocalSendProtocolException(
                    statusCode = response.code,
                    message = decodeError(responseBody) ?: response.message
                )
            }
            json.decodeFromString<T>(responseBody)
        }

    private suspend fun execute(request: Request): Response =
        suspendCancellableCoroutine { continuation ->
            val call = httpClient.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (continuation.isActive) {
                        continuation.resume(response)
                    } else {
                        response.close()
                    }
                }
            })
        }

    private fun decodeError(body: String): String? = runCatching {
        json.decodeFromString<ErrorResponseDto>(body).message
    }.getOrNull()

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private val EMPTY_BODY = ByteArray(0).toRequestBody(null)

        fun protocolJson(): Json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

        fun defaultHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}

class LocalSendProtocolException(
    val statusCode: Int? = null,
    override val message: String
) : IOException(message)

interface LocalSendUploadSource {
    val size: Long
    val contentType: String
    fun openStream(): InputStream
}
