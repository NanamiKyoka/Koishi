package com.nanami.koishi.core.image.crop

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * 封装的图片选取与裁剪启动器
 */
class CropImageLauncher internal constructor(
    private val onLaunch: (isSquare: Boolean, target: String) -> Unit
) {
    /**
     * 选取图片并在选取后启动裁剪页面
     *
     * @param isSquare 是否强制 1:1 正方形裁剪
     * @param target 裁剪目标标识符（例如 "logo", "bg", "grid_split"）
     */
    fun launch(isSquare: Boolean = true, target: String = "image") {
        onLaunch(isSquare, target)
    }
}

/**
 * 创建并记住一个通用的图片选取与裁剪启动器
 *
 * 流程：
 * 1. 调用 launcher.launch(isSquare, target)
 * 2. 调起系统相册选取图片
 * 3. 选取图片成功后，自动唤起 ImageCropActivity 进行专业裁剪
 * 4. 用户在裁剪页面点击确认后，通过 onImageCropped 回调提供已裁剪的缓存图片 Uri
 *
 * @param onImageCropped 裁剪完成并确认后的回调
 */
@Composable
fun rememberCropImageLauncher(
    onImageCropped: (Uri) -> Unit
): CropImageLauncher {
    val context = LocalContext.current

    val cropActivityLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val outputUriStr = result.data?.getStringExtra(ImageCropActivity.EXTRA_OUTPUT_URI)
            if (!outputUriStr.isNullOrBlank()) {
                onImageCropped(Uri.parse(outputUriStr))
            }
        }
    }

    var pendingIsSquare by remember { mutableStateOf(true) }
    var pendingTarget by remember { mutableStateOf("image") }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val intent = ImageCropActivity.createIntent(
                context = context,
                sourceUri = uri,
                isSquare = pendingIsSquare,
                target = pendingTarget
            )
            cropActivityLauncher.launch(intent)
        }
    }

    return remember {
        CropImageLauncher { isSquare, target ->
            pendingIsSquare = isSquare
            pendingTarget = target
            pickImageLauncher.launch("image/*")
        }
    }
}
