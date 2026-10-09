package com.nanami.koishi.feature.tools.lan_transfer

import android.content.ContentResolver
import android.net.Uri
import com.nanami.koishi.feature.tools.lan_transfer.engine.transfer.ContentUriFileSource
import com.nanami.koishi.feature.tools.lan_transfer.engine.transfer.PickedFile
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object LanTransferShareStore {
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    @Volatile
    private var pendingFiles: List<PickedFile> = emptyList()

    fun offerUris(uris: List<Uri>, resolver: ContentResolver) {
        val files = uris.mapNotNull { ContentUriFileSource.describe(resolver, it) }
        if (files.isEmpty()) return
        pendingFiles = files
        _events.tryEmit(Unit)
    }

    fun hasPending(): Boolean = pendingFiles.isNotEmpty()

    fun consume(): List<PickedFile> {
        val files = pendingFiles
        pendingFiles = emptyList()
        return files
    }
}
