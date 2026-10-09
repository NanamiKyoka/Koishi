package com.nanami.koishi.feature.tools.lan_transfer.engine.protocol

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class LocalSendDtoTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun decodesRegisterRequestFromLocalSend() {
        val payload = """
            {
              "alias": "Secret Banana",
              "version": "2.0",
              "deviceModel": "Windows",
              "deviceType": "desktop",
              "fingerprint": "random string",
              "port": 53317,
              "protocol": "https",
              "download": true
            }
        """.trimIndent()

        val result = json.decodeFromString<RegisterRequestDto>(payload)

        assertEquals("Secret Banana", result.alias)
        assertEquals("2.0", result.version)
        assertEquals("Windows", result.deviceModel)
        assertEquals("desktop", result.deviceType)
        assertEquals("https", result.protocol)
        assertEquals(53317, result.port)
    }

    @Test
    fun decodesRegisterResponseWithoutOptionalFields() {
        val payload = """
            {
              "alias": "Nice Orange",
              "version": "2.2",
              "fingerprint": "ABC123"
            }
        """.trimIndent()

        val result = json.decodeFromString<DeviceInfoResponseDto>(payload)

        assertEquals("Nice Orange", result.alias)
        assertNull(result.deviceModel)
        assertNull(result.deviceType)
        assertFalse(result.download)
    }

    @Test
    fun encodesPrepareUploadSessionFields() {
        val response = PrepareUploadResponseDto(
            sessionId = "session-1",
            files = mapOf("file-1" to "token-1")
        )

        val payload = json.encodeToString(PrepareUploadResponseDto.serializer(), response)

        assertEquals("""{"sessionId":"session-1","files":{"file-1":"token-1"}}""", payload)
    }

    @Test
    fun buildsEndpointUrlForIpv4AndIpv6() {
        assertEquals(
            "https://192.168.1.20:53317/api/localsend/v2/prepare-upload",
            LocalSendEndpoint.PREPARE_UPLOAD.url("192.168.1.20", 53317, "https").toString()
        )
        assertEquals(
            "http://[fe80::1]:53317/api/localsend/v2/info",
            LocalSendEndpoint.INFO.url("fe80::1", 53317, "http").toString()
        )
    }
}
