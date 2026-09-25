package com.nanami.koishi.feature.tools.video_to_gif.engine

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.nanami.koishi.feature.tools.video_to_gif.ResolutionScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.math.max

class VideoToGifEngine(private val context: Context) {

    @OptIn(UnstableApi::class)
    suspend fun convertVideoToGif(
        sourceUri: Uri,
        startTrimMs: Long,
        endTrimMs: Long,
        resolutionScale: ResolutionScale,
        targetFps: Int,
        srcWidth: Int,
        srcHeight: Int,
        outputGifFile: File,
        onProgress: (percent: Float, stage: String) -> Unit
    ): Result<File> = withContext(Dispatchers.Default) {
        val (targetWidth, targetHeight) = resolutionScale.calculateTargetDimensions(srcWidth, srcHeight)
        val trimmedDurationMs = max(100L, endTrimMs - startTrimMs)
        val tempTranscodeFile = File(context.cacheDir, "transcode_${System.currentTimeMillis()}.mp4")

        try {
            onProgress(0.05f, "正在使用 Media3 转码处理视频…")
            val media3Success = transcodeWithMedia3(
                sourceUri = sourceUri,
                startTrimMs = startTrimMs,
                endTrimMs = endTrimMs,
                targetWidth = targetWidth,
                targetHeight = targetHeight,
                outputFile = tempTranscodeFile,
                onProgress = { p ->
                    onProgress(0.05f + p * 0.45f, "正在使用 Media3 转码处理视频… (${(p * 100).toInt()}%)")
                }
            )

            val videoToRead = if (media3Success && tempTranscodeFile.exists() && tempTranscodeFile.length() > 0) {
                tempTranscodeFile
            } else {
                null
            }

            onProgress(0.55f, "正在提取视频帧并编码 GIF…")
            extractFramesAndEncodeGif(
                sourceFile = videoToRead,
                fallbackUri = if (videoToRead == null) sourceUri else null,
                clipStartMs = if (videoToRead == null) startTrimMs else 0L,
                clipDurationMs = trimmedDurationMs,
                targetWidth = targetWidth,
                targetHeight = targetHeight,
                targetFps = targetFps,
                outputGifFile = outputGifFile,
                onProgress = { p, stage ->
                    onProgress(0.55f + p * 0.45f, stage)
                }
            )

            Result.success(outputGifFile)
        } catch (e: Exception) {
            outputGifFile.delete()
            Result.failure(e)
        } finally {
            if (tempTranscodeFile.exists()) {
                tempTranscodeFile.delete()
            }
        }
    }

    @OptIn(UnstableApi::class)
    private suspend fun transcodeWithMedia3(
        sourceUri: Uri,
        startTrimMs: Long,
        endTrimMs: Long,
        targetWidth: Int,
        targetHeight: Int,
        outputFile: File,
        onProgress: (Float) -> Unit
    ): Boolean = withContext(Dispatchers.Main) {
        try {
            val clippingConfig = MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(startTrimMs)
                .setEndPositionMs(endTrimMs)
                .build()

            val mediaItem = MediaItem.Builder()
                .setUri(sourceUri)
                .setClippingConfiguration(clippingConfig)
                .build()

            val videoEffects = mutableListOf<Effect>()
            if (targetWidth > 0 && targetHeight > 0) {
                videoEffects.add(
                    Presentation.createForWidthAndHeight(
                        targetWidth,
                        targetHeight,
                        Presentation.LAYOUT_SCALE_TO_FIT
                    )
                )
            }

            val editedMediaItem = EditedMediaItem.Builder(mediaItem)
                .setRemoveAudio(true)
                .setEffects(Effects(emptyList(), videoEffects))
                .build()

            var transformerRef: Transformer? = null

            val pollingJob = launch(Dispatchers.Default) {
                val progressHolder = ProgressHolder()
                while (isActive) {
                    delay(200)
                    withContext(Dispatchers.Main) {
                        val transformer = transformerRef ?: return@withContext
                        val state = transformer.getProgress(progressHolder)
                        if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                            onProgress(progressHolder.progress / 100f)
                        }
                    }
                }
            }

            try {
                suspendCancellableCoroutine { continuation ->
                    val transformer = Transformer.Builder(context)
                        .addListener(object : Transformer.Listener {
                            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                                if (continuation.isActive) continuation.resume(true)
                            }

                            override fun onError(
                                composition: Composition,
                                exportResult: ExportResult,
                                exportException: ExportException
                            ) {
                                if (continuation.isActive) continuation.resume(false)
                            }
                        })
                        .build()

                    transformerRef = transformer
                    continuation.invokeOnCancellation {
                        try {
                            transformer.cancel()
                        } catch (_: Throwable) {}
                    }

                    transformer.start(editedMediaItem, outputFile.absolutePath)
                }
            } finally {
                pollingJob.cancel()
            }
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun extractFramesAndEncodeGif(
        sourceFile: File?,
        fallbackUri: Uri?,
        clipStartMs: Long,
        clipDurationMs: Long,
        targetWidth: Int,
        targetHeight: Int,
        targetFps: Int,
        outputGifFile: File,
        onProgress: (percent: Float, stage: String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            if (sourceFile != null) {
                retriever.setDataSource(sourceFile.absolutePath)
            } else if (fallbackUri != null) {
                retriever.setDataSource(context, fallbackUri)
            } else {
                throw IllegalStateException("No video source")
            }

            val frameIntervalMs = 1000L / targetFps
            val totalFrames = max(1, (clipDurationMs / frameIntervalMs).toInt())
            val delayPerFrameMs = (1000 / targetFps)

            FileOutputStream(outputGifFile).use { fos ->
                val encoder = AnimatedGifEncoder()
                if (!encoder.start(fos)) {
                    throw IllegalStateException("Failed to start GIF encoder")
                }
                encoder.setDelay(delayPerFrameMs)
                encoder.setRepeat(0)
                encoder.setQuality(10)

                for (frameIndex in 0 until totalFrames) {
                    if (!currentCoroutineContext().isActive) {
                        throw kotlinx.coroutines.CancellationException("GIF encoding canceled")
                    }

                    val timeUs = (clipStartMs + frameIndex * frameIntervalMs) * 1000L
                    val frameBitmap: Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 && targetWidth > 0 && targetHeight > 0) {
                        retriever.getScaledFrameAtTime(
                            timeUs,
                            MediaMetadataRetriever.OPTION_CLOSEST,
                            targetWidth,
                            targetHeight
                        )
                    } else {
                        val original = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                        if (original != null && targetWidth > 0 && targetHeight > 0 && (original.width != targetWidth || original.height != targetHeight)) {
                            val scaled = Bitmap.createScaledBitmap(original, targetWidth, targetHeight, true)
                            if (scaled != original) original.recycle()
                            scaled
                        } else {
                            original
                        }
                    }

                    if (frameBitmap != null) {
                        encoder.addFrame(frameBitmap)
                        frameBitmap.recycle()
                    }

                    val percent = (frameIndex + 1).toFloat() / totalFrames
                    onProgress(percent, "正在编码第 ${frameIndex + 1}/$totalFrames 帧 GIF")
                }

                encoder.finish()
            }
        } finally {
            try {
                retriever.release()
            } catch (_: Throwable) {}
        }
    }
}
