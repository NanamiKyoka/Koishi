package com.nanami.koishi.feature.tools.lan_transfer.engine.security

import android.content.Context
import java.io.ByteArrayInputStream
import java.io.File
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Base64

class LocalSendIdentityStore(context: Context) {
    private val directory = File(context.noBackupFilesDir, DIRECTORY_NAME)
    private val certificateFile = File(directory, CERTIFICATE_FILE)
    private val privateKeyFile = File(directory, PRIVATE_KEY_FILE)

    @Synchronized
    fun get(): LocalSendIdentityMaterial {
        readExisting()?.let { return it }
        val created = LocalSendCertificateFactory.create()
        persist(created)
        return created
    }

    private fun readExisting(): LocalSendIdentityMaterial? = runCatching {
        val certificate = decodeCertificate(certificateFile.readText())
        val privateKey = decodePrivateKey(privateKeyFile.readText())
        certificate.checkValidity()
        LocalSendIdentityMaterial(privateKey, certificate)
    }.getOrNull()

    private fun persist(identity: LocalSendIdentityMaterial) {
        directory.mkdirs()
        val encoder = Base64.getEncoder()
        certificateFile.writeText(encoder.encodeToString(identity.certificate.encoded))
        privateKeyFile.writeText(encoder.encodeToString(identity.privateKey.encoded))
    }

    private fun decodeCertificate(value: String): X509Certificate =
        CertificateFactory.getInstance("X.509")
            .generateCertificate(ByteArrayInputStream(Base64.getDecoder().decode(value))) as X509Certificate

    private fun decodePrivateKey(value: String) =
        KeyFactory.getInstance("RSA").generatePrivate(
            PKCS8EncodedKeySpec(Base64.getDecoder().decode(value))
        )

    private companion object {
        const val DIRECTORY_NAME = "lan_transfer_identity"
        const val CERTIFICATE_FILE = "certificate.der"
        const val PRIVATE_KEY_FILE = "private_key.pk8"
    }
}
