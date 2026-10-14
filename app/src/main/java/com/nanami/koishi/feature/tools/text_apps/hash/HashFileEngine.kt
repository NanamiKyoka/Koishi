package com.nanami.koishi.feature.tools.text_apps.hash

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

object HashFileEngine {

    suspend fun digestFile(context: Context, uri: Uri): Result<Map<HashAlgorithm, String>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val digests = HashAlgorithm.entries.associateWith {
                    MessageDigest.getInstance(it.jcaName)
                }
                val input = context.contentResolver.openInputStream(uri)
                    ?: error("Unable to open input stream")
                input.use { stream ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val read = stream.read(buffer)
                        if (read < 0) break
                        digests.values.forEach { digest -> digest.update(buffer, 0, read) }
                    }
                }
                digests.mapValues { (_, digest) -> digest.digest().toHex() }
            }
        }
}
