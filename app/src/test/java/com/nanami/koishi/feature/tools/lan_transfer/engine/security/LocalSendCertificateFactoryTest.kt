package com.nanami.koishi.feature.tools.lan_transfer.engine.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalSendCertificateFactoryTest {

    @Test
    fun createsSelfSignedRsaIdentity() {
        val identity = LocalSendCertificateFactory.create()

        identity.certificate.verify(identity.certificate.publicKey)
        assertEquals("RSA", identity.privateKey.algorithm)
        assertTrue(identity.certificate.subjectX500Principal.name.contains("CN=LocalSend User"))
        assertEquals(64, identity.fingerprint.length)
        assertTrue(identity.fingerprint.all { it in "0123456789ABCDEF" })
    }

    @Test
    fun createsKeyStoreWithIdentity() {
        val identity = LocalSendCertificateFactory.create()
        val password = "test-password".toCharArray()
        val keyStore = identity.keyStore(password)

        assertTrue(keyStore.containsAlias(LocalSendIdentityMaterial.KEY_ALIAS))
        assertNotNull(keyStore.getKey(LocalSendIdentityMaterial.KEY_ALIAS, password))
    }
}
