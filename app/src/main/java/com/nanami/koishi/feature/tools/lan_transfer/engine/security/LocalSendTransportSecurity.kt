package com.nanami.koishi.feature.tools.lan_transfer.engine.security

import okhttp3.OkHttpClient
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

internal data class LocalSendHttpClient(
    val client: OkHttpClient,
    val trustManager: LocalSendFingerprintTrustManager
)

internal object LocalSendTransportSecurity {
    private val keyStorePassword = "localsend".toCharArray()
    private val privateKeyPassword = "localsend".toCharArray()

    fun pinClient(
        identity: LocalSendIdentityMaterial,
        baseClient: OkHttpClient,
        expectedFingerprint: String?
    ): LocalSendHttpClient {
        val trustManager = LocalSendFingerprintTrustManager(expectedFingerprint)
        val keyStore = identity.keyStore(keyStorePassword.copyOf())
        val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
            .apply { init(keyStore, privateKeyPassword.copyOf()) }
        val sslContext = SSLContext.getInstance("TLS").apply {
            init(keyManagerFactory.keyManagers, arrayOf<TrustManager>(trustManager), SecureRandom())
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
