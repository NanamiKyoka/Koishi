package com.nanami.koishi.feature.tools.qr_tool.scan

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.nanami.koishi.R
import com.nanami.koishi.core.designsystem.theme.KoishiTheme
import com.nanami.koishi.core.util.LocaleHelper
import java.util.concurrent.Executors

class QrScanActivity : ComponentActivity() {

    companion object {
        const val EXTRA_RESULT_TEXT = "extra_qr_result_text"
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            KoishiTheme {
                QrScanScreen(
                    onBack = { finish() },
                    onResultFilled = { scannedText ->
                        val intent = Intent().apply {
                            putExtra(EXTRA_RESULT_TEXT, scannedText)
                        }
                        setResult(RESULT_OK, intent)
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScanScreen(
    onBack: () -> Unit,
    onResultFilled: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            Toast.makeText(context, context.getString(R.string.qr_scan_permission_denied), Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var isTorchEnabled by remember { mutableStateOf(false) }
    var cameraControl: Camera? by remember { mutableStateOf(null) }
    var scanResult by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    val barcodeScanner = remember {
        try {
            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
            BarcodeScanning.getClient(options)
        } catch (e: Throwable) {
            cameraError = "扫描组件初始化异常: ${e.localizedMessage ?: e.javaClass.simpleName}"
            null
        }
    }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // 相册图片选择识别
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val scanner = barcodeScanner
            if (scanner == null) {
                Toast.makeText(context, "扫描引擎未就绪", Toast.LENGTH_SHORT).show()
                return@rememberLauncherForActivityResult
            }
            try {
                val inputImage = InputImage.fromFilePath(context, uri)
                scanner.process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        val first = barcodes.firstOrNull()?.rawValue
                        if (!first.isNullOrBlank()) {
                            triggerVibration(context)
                            scanResult = first
                        } else {
                            Toast.makeText(context, context.getString(R.string.qr_scan_no_qr_in_image), Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, context.getString(R.string.qr_scan_no_qr_in_image), Toast.LENGTH_SHORT).show()
                    }
            } catch (e: Exception) {
                Toast.makeText(context, context.getString(R.string.qr_scan_no_qr_in_image), Toast.LENGTH_SHORT).show()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                barcodeScanner?.close()
            } catch (_: Throwable) {}
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (hasCameraPermission && cameraError == null) {
            // 1. CameraX 预览
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            if (barcodeScanner != null) {
                                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                    processImageProxy(imageProxy, barcodeScanner) { result ->
                                        if (!isProcessing && scanResult == null) {
                                            isProcessing = true
                                            triggerVibration(ctx)
                                            scanResult = result
                                        }
                                    }
                                }
                            }

                            val selector = when {
                                cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
                                cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
                                else -> null
                            }

                            if (selector != null) {
                                cameraProvider.unbindAll()
                                val cam = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    selector,
                                    preview,
                                    imageAnalysis
                                )
                                cameraControl = cam
                            } else {
                                cameraError = "未检测到可用的相机镜头"
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            cameraError = "相机初始化异常: ${e.localizedMessage}"
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                }
            )

            // 2. 扫码取景框与扫描线动画
            ScanOverlayView(modifier = Modifier.fillMaxSize())
        } else {
            // 权限不足或硬件异常提示
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = cameraError ?: stringResource(R.string.qr_scan_permission_denied),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (cameraError == null) {
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text(stringResource(R.string.qr_scan_grant_permission))
                    }
                }
            }
        }

        // 3. 顶部操作栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.btn_back),
                    tint = Color.White
                )
            }

            Text(
                text = stringResource(R.string.qr_scan_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )

            IconButton(
                onClick = {
                    val target = !isTorchEnabled
                    cameraControl?.cameraControl?.enableTorch(target)
                    isTorchEnabled = target
                },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                Icon(
                    imageVector = if (isTorchEnabled) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
                    contentDescription = stringResource(R.string.qr_scan_flashlight),
                    tint = if (isTorchEnabled) Color(0xFFFFD54F) else Color.White
                )
            }
        }

        // 4. 底部相册导入按钮
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            androidx.compose.material3.Button(
                onClick = { galleryLauncher.launch("image/*") },
                shape = RoundedCornerShape(24.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                elevation = androidx.compose.material3.ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(imageVector = Icons.Rounded.Photo, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.qr_scan_gallery),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }

        // 5. 扫描结果弹窗
        scanResult?.let { text ->
            ScanResultBottomSheet(
                resultText = text,
                onDismiss = {
                    scanResult = null
                    isProcessing = false
                },
                onFillTool = {
                    onResultFilled(text)
                }
            )
        }
    }
}

/**
 * 带有高亮角标与平滑激光扫描线的取景框
 * 纯 dp 布局与尺寸计算，杜绝屏幕约束异常
 */
@Composable
fun ScanOverlayView(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserFraction by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "laserProgress"
    )
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(bottom = 40.dp)
        ) {
            val boxSizeDp = 260.dp

            // 方形取景框
            Box(
                modifier = Modifier
                    .size(boxSizeDp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val boxSize = size.width
                    val cornerLen = 28.dp.toPx()
                    val cornerStroke = 4.dp.toPx()

                    // 取景框微透明主题色轮廓线
                    drawRect(
                        color = primaryColor.copy(alpha = 0.35f),
                        topLeft = Offset.Zero,
                        size = Size(boxSize, boxSize),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                    )

                    // 4 个高亮角标
                    // 左上
                    drawLine(primaryColor, Offset(0f, 0f), Offset(cornerLen, 0f), cornerStroke)
                    drawLine(primaryColor, Offset(0f, 0f), Offset(0f, cornerLen), cornerStroke)
                    // 右上
                    drawLine(primaryColor, Offset(boxSize, 0f), Offset(boxSize - cornerLen, 0f), cornerStroke)
                    drawLine(primaryColor, Offset(boxSize, 0f), Offset(boxSize, cornerLen), cornerStroke)
                    // 左下
                    drawLine(primaryColor, Offset(0f, boxSize), Offset(cornerLen, boxSize), cornerStroke)
                    drawLine(primaryColor, Offset(0f, boxSize), Offset(0f, boxSize - cornerLen), cornerStroke)
                    // 右下
                    drawLine(primaryColor, Offset(boxSize, boxSize), Offset(boxSize - cornerLen, boxSize), cornerStroke)
                    drawLine(primaryColor, Offset(boxSize, boxSize), Offset(boxSize, boxSize - cornerLen), cornerStroke)

                    // 动态激光扫描线
                    val laserY = boxSize * laserFraction
                    val gradientBrush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            primaryColor.copy(alpha = 0.85f),
                            primaryColor,
                            primaryColor.copy(alpha = 0.85f),
                            Color.Transparent
                        )
                    )
                    drawLine(
                        brush = gradientBrush,
                        start = Offset(6.dp.toPx(), laserY),
                        end = Offset(boxSize - 6.dp.toPx(), laserY),
                        strokeWidth = 3.dp.toPx()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 提示文案
            Text(
                text = stringResource(R.string.qr_scan_tip),
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            )
        }
    }
}

/**
 * 扫码成功底部卡片
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanResultBottomSheet(
    resultText: String,
    onDismiss: () -> Unit,
    onFillTool: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(
                text = stringResource(R.string.qr_scan_result_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = resultText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 复制按钮
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("QR", resultText))
                        Toast.makeText(context, context.getString(R.string.qr_scan_copied), Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.qr_scan_btn_copy))
                }

                // 如果是网址，支持直接在浏览器打开
                if (resultText.startsWith("http://", ignoreCase = true) || resultText.startsWith("https://", ignoreCase = true)) {
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(resultText))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Rounded.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.qr_scan_btn_open_url))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 填入二维码生成器
            Button(
                onClick = onFillTool,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.qr_scan_btn_fill_tool))
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun processImageProxy(
    imageProxy: ImageProxy,
    barcodeScanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    onSuccess: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        try {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    val first = barcodes.firstOrNull()?.rawValue
                    if (!first.isNullOrBlank()) {
                        onSuccess(first)
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } catch (_: Exception) {
            imageProxy.close()
        }
    } else {
        imageProxy.close()
    }
}

private fun triggerVibration(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            @Suppress("DEPRECATION")
            vibrator?.vibrate(50)
        }
    } catch (_: Exception) {}
}
