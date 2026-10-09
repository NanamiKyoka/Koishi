package com.nanami.koishi.feature.tools.lan_transfer.engine.protocol

import com.nanami.koishi.feature.tools.lan_transfer.engine.model.LanDeviceProtocol
import com.nanami.koishi.feature.tools.lan_transfer.engine.security.LocalSendFingerprintTrustManager
import com.nanami.koishi.feature.tools.lan_transfer.engine.security.LocalSendHttpClient
import com.nanami.koishi.feature.tools.lan_transfer.engine.security.LocalSendIdentityMaterial
import com.nanami.koishi.feature.tools.lan_transfer.engine.security.LocalSendTransportSecurity
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
import okio.BufferedSink
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

interface LocalSendRegisterClient {
    suspend fun register(
        host: String,
        port: Int,
        protocol: LanDeviceProtocol,
        payload: RegisterRequestDto,
        expectedFingerprint: String? = null
    ): DeviceInfoResponseDto
}

class LocalSendClient(
    private val identity: LocalSendIdentityMaterial,
    private val baseHttpClient: OkHttpClient = defaultHttpClient(),
    private val json: Json = protocolJson()
) : LocalSendRegisterClient {

    override suspend fun register(
        host: String,
        port: Int,
        protocol: LanDeviceProtocol,
        payload: RegisterRequestDto,
        expectedFingerprint: String?
    ): DeviceInfoResponseDto {
        val url = LocalSendEndpoint.REGISTER.url(host, port, protocol.wireValue)
            ?: throw LocalSendProtocolException(message = "Invalid device address")
        val body = json.encodeToString(RegisterRequestDto.serializer(), payload)
            .toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()
        val transport = transportFor(protocol, expectedFingerprint)
        val response = executeJson<DeviceInfoResponseDto>(transport.client, request)
        ensureCertificateFingerprint(protocol, transport, response.fingerprint)
        return response
    }

    suspend fun info(
        host: String,
        port: Int,
        protocol: LanDeviceProtocol,
        expectedFingerprint: String? = null
    ): DeviceInfoResponseDto {
        val url = LocalSendEndpoint.INFO.url(host, port, protocol.wireValue)
            ?: throw LocalSendProtocolException(message = "Invalid device address")
        val transport = transportFor(protocol, expectedFingerprint)
        val response = executeJson<DeviceInfoResponseDto>(transport.client, Request.Builder().url(url).get().build())
        ensureCertificateFingerprint(protocol, transport, response.fingerprint)
        return response
    }

    suspend fun prepareUpload(
        host: String,
        port: Int,
        protocol: LanDeviceProtocol,
        payload: PrepareUploadRequestDto,
        pin: String? = null,
        expectedFingerprint: String? = null
    ): PrepareUploadResponseDto? {
        val baseUrl = LocalSendEndpoint.PREPARE_UPLOAD.url(host, port, protocol.wireValue)
            ?: throw LocalSendProtocolException(message = "Invalid device address")
        val url = baseUrl.newBuilder()
            .apply { pin?.takeIf { it.isNotBlank() }?.let { addQueryParameter("pin", it) } }
            .build()
        val body = json.encodeToString(PrepareUploadRequestDto.serializer(), payload)
            .toRequestBody(JSON_MEDIA_TYPE)
        val transport = transportFor(protocol, expectedFingerprint)
        execute(transport.client, Request.Builder().url(url).post(body).build()).use { response ->
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
        expectedFingerprint: String? = null,
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
        val transport = transportFor(protocol, expectedFingerprint)
        execute(transport.client, Request.Builder().url(url).post(body).build()).use { response ->
            ensureSuccessful(response)
        }
    }

    suspend fun cancel(
        host: String,
        port: Int,
        protocol: LanDeviceProtocol,
        sessionId: String,
        expectedFingerprint: String? = null
    ) {
        val baseUrl = LocalSendEndpoint.CANCEL.url(host, port, protocol.wireValue)
            ?: throw LocalSendProtocolException(message = "Invalid device address")
        val url = baseUrl.newBuilder()
            .addQueryParameter("sessionId", sessionId)
            .build()
        val transport = transportFor(protocol, expectedFingerprint)
        execute(transport.client, Request.Builder().url(url).post(EMPTY_BODY).build()).use { response ->
            ensureSuccessful(response)
        }
    }

    private fun transportFor(
        protocol: LanDeviceProtocol,
        expectedFingerprint: String?
    ): LocalSendHttpClient {
        if (protocol == LanDeviceProtocol.HTTP) {
            return LocalSendHttpClient(
                client = baseHttpClient,
                trustManager = LocalSendFingerprintTrustManager(null)
            )
        }
        return LocalSendTransportSecurity.pinClient(identity, baseHttpClient, expectedFingerprint)
    }

    private fun ensureCertificateFingerprint(
        protocol: LanDeviceProtocol,
        transport: LocalSendHttpClient,
        claimedFingerprint: String
    ) {
        if (protocol != LanDeviceProtocol.HTTPS) return
        val observed = transport.trustManager.observedFingerprint
            ?: throw LocalSendProtocolException(message = "Missing server certificate")
        if (!observed.equals(claimedFingerprint, ignoreCase = true)) {
            throw LocalSendProtocolException(message = "Server fingerprint does not match its certificate")
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

    private suspend inline fun <reified T> executeJson(
        client: OkHttpClient,
        request: Request
    ): T = execute(client, request).use { response ->
        val responseBody = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
            throw LocalSendProtocolException(
                statusCode = response.code,
                message = decodeError(responseBody) ?: response.message
            )
        }
        json.decodeFromString<T>(responseBody)
    }

    private suspend fun execute(client: OkHttpClient, request: Request): Response =
        suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
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
