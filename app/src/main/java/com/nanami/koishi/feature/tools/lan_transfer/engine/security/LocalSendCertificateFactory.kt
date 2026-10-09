package com.nanami.koishi.feature.tools.lan_transfer.engine.security

import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.KeyUsage
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date

data class LocalSendIdentityMaterial(
    val privateKey: PrivateKey,
    val certificate: X509Certificate
) {
    val fingerprint: String
        get() = LocalSendCertificateFactory.fingerprint(certificate)

    fun keyStore(password: CharArray): KeyStore = KeyStore.getInstance("PKCS12").apply {
        load(null, null)
        setKeyEntry(KEY_ALIAS, privateKey, password, arrayOf(certificate))
    }

    companion object {
        const val KEY_ALIAS = "lan_transfer"
    }
}

object LocalSendCertificateFactory {
    private const val SUBJECT_NAME = "CN=LocalSend User"
    private const val SIGNATURE_ALGORITHM = "SHA256withRSA"

    fun create(): LocalSendIdentityMaterial {
        val keyPair = KeyPairGenerator.getInstance("RSA").apply {
            initialize(2048, SecureRandom())
        }.generateKeyPair()
        val subject = X500Name(SUBJECT_NAME)
        val now = Instant.now()
        val builder = JcaX509v3CertificateBuilder(
            subject,
            BigInteger(160, SecureRandom()),
            Date.from(now.minus(1, ChronoUnit.DAYS)),
            Date.from(now.plus(36500, ChronoUnit.DAYS)),
            subject,
            keyPair.public
        )
        builder.addExtension(Extension.basicConstraints, true, BasicConstraints(false))
        builder.addExtension(
            Extension.keyUsage,
            true,
            KeyUsage(KeyUsage.digitalSignature or KeyUsage.keyEncipherment)
        )
        val signer = JcaContentSignerBuilder(SIGNATURE_ALGORITHM)
            .build(keyPair.private)
        val certificate = JcaX509CertificateConverter()
            .getCertificate(builder.build(signer))
        return LocalSendIdentityMaterial(
            privateKey = keyPair.private,
            certificate = certificate
        )
    }

    fun fingerprint(certificate: X509Certificate): String =
        java.security.MessageDigest.getInstance("SHA-256")
            .digest(certificate.encoded)
            .joinToString(separator = "") { byte -> "%02X".format(byte) }
}
