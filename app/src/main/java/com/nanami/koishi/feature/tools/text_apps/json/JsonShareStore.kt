package com.nanami.koishi.feature.tools.text_apps.json

import android.net.Uri
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object JsonShareStore {
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    @Volatile
    private var pendingUri: Uri? = null

    fun offer(uri: Uri) {
        pendingUri = uri
        _events.tryEmit(Unit)
    }

    fun hasPending(): Boolean = pendingUri != null

    fun consume(): Uri? {
        val uri = pendingUri
        pendingUri = null
        return uri
    }
}
