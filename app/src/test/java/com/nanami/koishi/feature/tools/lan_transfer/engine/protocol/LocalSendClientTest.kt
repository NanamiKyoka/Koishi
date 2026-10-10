package com.nanami.koishi.feature.tools.lan_transfer.engine.protocol

import com.nanami.koishi.feature.tools.lan_transfer.engine.model.LanDeviceProtocol
import com.nanami.koishi.feature.tools.lan_transfer.engine.security.LocalSendCertificateFactory
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalSendClientTest {

    @Test
    fun sendsRegisterRequestAndDecodesResponse() = runTest {
        val server = MockWebServer()
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"alias":"Remote","version":"2.2","fingerprint":"ABC123","download":true}""")
        )
        server.start()
        try {
            val client = LocalSendClient(LocalSendCertificateFactory.create(), OkHttpClient())
            val response = client.register(
                host = server.hostName,
                port = server.port,
                protocol = LanDeviceProtocol.HTTP,
                payload = RegisterRequestDto(
                    alias = "Koishi",
                    version = "2.2",
                    fingerprint = "LOCAL123",
                    port = 53317,
                    protocol = "http"
                )
            )

            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/api/localsend/v2/register", request.path)
            assertTrue(request.body.readUtf8().contains(""""alias":"Koishi""""))
            assertEquals("Remote", response.alias)
            assertTrue(response.download)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun sendsPinWithPrepareUploadRequest() = runTest {
        val server = MockWebServer()
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"sessionId":"session-1","files":{}}""")
        )
        server.start()
        try {
            val client = LocalSendClient(LocalSendCertificateFactory.create(), OkHttpClient())
            client.prepareUpload(
                host = server.hostName,
                port = server.port,
                protocol = LanDeviceProtocol.HTTP,
                payload = PrepareUploadRequestDto(
                    info = RegisterRequestDto(
                        alias = "Koishi",
                        version = "2.2",
                        fingerprint = "LOCAL123",
                        port = 53317,
                        protocol = "http"
                    ),
                    files = emptyMap()
                ),
                pin = "12 34"
            )

            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/api/localsend/v2/prepare-upload?pin=12%2034", request.path)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun exposesServerErrorMessage() = runTest {
        val server = MockWebServer()
        server.enqueue(
            MockResponse()
                .setResponseCode(403)
                .setBody("""{"message":"Rejected"}""")
        )
        server.start()
        try {
            val client = LocalSendClient(LocalSendCertificateFactory.create(), OkHttpClient())
            val result = runCatching {
                client.register(
                    host = server.hostName,
                    port = server.port,
                    protocol = LanDeviceProtocol.HTTP,
                    payload = RegisterRequestDto(
                        alias = "Koishi",
                        version = "2.2",
                        fingerprint = "LOCAL123",
                        port = 53317,
                        protocol = "http"
                    )
                )
            }

            val error = result.exceptionOrNull() as LocalSendProtocolException
            assertEquals(403, error.statusCode)
            assertEquals("Rejected", error.message)
        } finally {
            server.shutdown()
        }
    }
}
