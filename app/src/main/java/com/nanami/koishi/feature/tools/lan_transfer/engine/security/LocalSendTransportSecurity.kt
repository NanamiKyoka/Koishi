package com.nanami.koishi.feature.tools.lan_transfer.engine.security

import okhttp3.OkHttpClient
import java.net.Socket
import java.security.Principal
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLEngine
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509ExtendedKeyManager
import javax.net.ssl.X509TrustManager

internal data class LocalSendHttpClient(
    val client: OkHttpClient,
    val trustManager: LocalSendFingerprintTrustManager
)

internal object LocalSendTransportSecurity {
    fun pinClient(
        identity: LocalSendIdentityMaterial,
        baseClient: OkHttpClient,
        expectedFingerprint: String?
    ): LocalSendHttpClient {
        val trustManager = LocalSendFingerprintTrustManager(expectedFingerprint)
        val keyManager = LocalSendIdentityKeyManager(identity)
        val sslContext = SSLContext.getInstance("TLS").apply {
            init(arrayOf(keyManager), arrayOf<TrustManager>(trustManager), SecureRandom())
        }
        return LocalSendHttpClient(
            client = baseClient.newBuilder()
                .sslSocketFactory(sslContext.socketFactory, trustManager)
                .hostnameVerifier { _, _ -> true }
                .build(),
            trustManager = trustManager
        )
    }
}

private class LocalSendIdentityKeyManager(
    private val identity: LocalSendIdentityMaterial
) : X509ExtendedKeyManager() {
    override fun getClientAliases(keyType: String?, issuers: Array<out Principal>?): Array<String> =
        if (supports(keyType)) arrayOf(ALIAS) else emptyArray()

    override fun chooseClientAlias(
        keyType: Array<out String>?,
        issuers: Array<out Principal>?,
        socket: Socket?
    ): String? = if (keyType?.any(::supports) == true) ALIAS else null

    override fun chooseEngineClientAlias(
        keyType: Array<out String>?,
        issuers: Array<out Principal>?,
        engine: SSLEngine?
    ): String? = if (keyType?.any(::supports) == true) ALIAS else null

    override fun getServerAliases(keyType: String?, issuers: Array<out Principal>?): Array<String>? = null

    override fun chooseServerAlias(
        keyType: String?,
        issuers: Array<out Principal>?,
        socket: Socket?
    ): String? = null

    override fun getCertificateChain(alias: String?): Array<X509Certificate>? =
        if (alias == ALIAS) arrayOf(identity.certificate) else null

    override fun getPrivateKey(alias: String?): java.security.PrivateKey? =
        if (alias == ALIAS) identity.privateKey else null

    private fun supports(keyType: String?): Boolean =
        keyType == "RSA" || keyType == "RSA-PSS" || keyType == "RSASSA-PSS"

    private companion object {
        const val ALIAS = "localsend-client"
    }
}

internal class LocalSendFingerprintTrustManager(
    private val expectedFingerprint: String?
) : X509TrustManager {
    @Volatile
    var observedFingerprint: String? = null
        private set

    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        val leaf = chain?.firstOrNull() ?: throw CertificateException("Missing server certificate")
        val actual = LocalSendCertificateFactory.fingerprint(leaf)
        if (!expectedFingerprint.isNullOrBlank() &&
            !actual.equals(expectedFingerprint, ignoreCase = true)
        ) {
            throw CertificateException("Server certificate fingerprint mismatch")
        }
        observedFingerprint = actual
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
}
