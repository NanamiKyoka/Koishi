package com.nanami.koishi.feature.tools.lan_transfer.engine.protocol

import com.nanami.koishi.feature.tools.lan_transfer.engine.model.LanDeviceProtocol
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
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
