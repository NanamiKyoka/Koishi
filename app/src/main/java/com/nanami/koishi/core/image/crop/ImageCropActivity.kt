package com.nanami.koishi.core.image.crop

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.nanami.koishi.core.designsystem.theme.KoishiTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import com.nanami.koishi.core.util.LocaleHelper
import kotlin.math.max

class ImageCropActivity : ComponentActivity() {

    companion object {
        const val EXTRA_SOURCE_URI = "extra_source_uri"
        const val EXTRA_IS_SQUARE = "extra_is_square"
        const val EXTRA_TARGET = "extra_target"
        const val EXTRA_OUTPUT_URI = "extra_output_uri"

        fun createIntent(context: Context, sourceUri: Uri, isSquare: Boolean = true, target: String = "image"): Intent {
            return Intent(context, ImageCropActivity::class.java).apply {
                putExtra(EXTRA_SOURCE_URI, sourceUri.toString())
                putExtra(EXTRA_IS_SQUARE, isSquare)
                putExtra(EXTRA_TARGET, target)
            }
        }
    }

    private var sourceBitmap by mutableStateOf<Bitmap?>(null)
    private var isSquare by mutableStateOf(true)
    private var targetType = "image"

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sourceUriStr = intent.getStringExtra(EXTRA_SOURCE_URI)
        isSquare = intent.getBooleanExtra(EXTRA_IS_SQUARE, true)
        targetType = intent.getStringExtra(EXTRA_TARGET) ?: "image"

        if (sourceUriStr.isNullOrBlank()) {
            Toast.makeText(this, getString(com.nanami.koishi.R.string.crop_invalid_source), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val sourceUri = Uri.parse(sourceUriStr)
        loadSourceBitmap(sourceUri)

        setContent {
            KoishiTheme {
                ImageCropScreen(
                    sourceBitmap = sourceBitmap,
                    isSquare = isSquare,
                    onCancel = {
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    },
                    onConfirm = { cropRectOnScreen, viewportWidth, viewportHeight, scale, rotationDegrees, panX, panY, brightness, contrast, saturation, baseScale ->
                        processAndSaveCroppedBitmap(
                            cropRectOnScreen,
                            viewportWidth,
                            viewportHeight,
                            scale,
                            rotationDegrees,
                            panX,
                            panY,
                            brightness,
                            contrast,
                            saturation,
                            baseScale
                        )
                    }
                )
            }
        }
    }

    private fun loadSourceBitmap(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. 获取图片尺寸
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                }

                // 2. 采样率限制在 2048 以内防 OOM
                val maxDim = max(options.outWidth, options.outHeight)
                var sampleSize = 1
                while (maxDim / sampleSize > 2048) {
                    sampleSize *= 2
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }

                val bitmap = contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, decodeOptions)
                }

                withContext(Dispatchers.Main) {
                    if (bitmap != null) {
                        sourceBitmap = bitmap
                    } else {
                        Toast.makeText(this@ImageCropActivity, getString(com.nanami.koishi.R.string.crop_read_failed), Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ImageCropActivity, getString(com.nanami.koishi.R.string.crop_load_error, e.localizedMessage ?: ""), Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun processAndSaveCroppedBitmap(
        cropRectOnScreen: RectF,
        viewportWidth: Float,
        viewportHeight: Float,
        scale: Float,
        rotationDegrees: Float,
        panX: Float,
        panY: Float,
        brightness: Float,
        contrast: Float,
        saturation: Float,
        baseScale: Float
    ) {
        val src = sourceBitmap ?: return
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                val cropped = ImageAdjustmentEngine.renderCroppedBitmap(
                    source = src,
                    cropRectOnScreen = cropRectOnScreen,
                    viewportWidth = viewportWidth,
                    viewportHeight = viewportHeight,
                    scale = scale,
                    rotationDegrees = rotationDegrees,
                    panX = panX,
                    panY = panY,
                    brightness = brightness,
                    contrast = contrast,
                    saturation = saturation,
                    outputMaxDimension = 1024,
                    baseScaleOverride = baseScale
                )

                // 保存至缓存文件
                val cacheFile = File(cacheDir, "cropped_${System.currentTimeMillis()}.png")
                FileOutputStream(cacheFile).use { out ->
                    cropped.compress(Bitmap.CompressFormat.PNG, 100, out)
                }

                val outputUri = try {
                    androidx.core.content.FileProvider.getUriForFile(
                        this@ImageCropActivity,
                        "${packageName}.fileprovider",
                        cacheFile
                    )
                } catch (e: Exception) {
                    Uri.fromFile(cacheFile)
                }

                withContext(Dispatchers.Main) {
                    val resultIntent = Intent().apply {
                        data = outputUri
                        putExtra(EXTRA_OUTPUT_URI, outputUri.toString())
                        putExtra(EXTRA_TARGET, targetType)
                    }
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ImageCropActivity, getString(com.nanami.koishi.R.string.crop_save_failed), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
