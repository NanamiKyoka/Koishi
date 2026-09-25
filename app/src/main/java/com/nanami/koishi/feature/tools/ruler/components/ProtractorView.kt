package com.nanami.koishi.feature.tools.ruler.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.FlipCameraAndroid
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nanami.koishi.R
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class ProtractorInteractionMode {
    POINTER_DRAG,
    THREE_POINT
}

@Composable
fun ProtractorView(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current

    var interactionMode by remember { mutableStateOf(ProtractorInteractionMode.POINTER_DRAG) }
    var isCameraActive by remember { mutableStateOf(false) }
    var showCameraPermissionDialog by remember { mutableStateOf(false) }

    var pointer1AngleDeg by remember { mutableFloatStateOf(30f) }
    var pointer2AngleDeg by remember { mutableFloatStateOf(120f) }

    var pointVertex by remember { mutableStateOf<Offset?>(null) }
    var pointRay1 by remember { mutableStateOf<Offset?>(null) }
    var pointRay2 by remember { mutableStateOf<Offset?>(null) }
    var threePointStep by remember { mutableIntStateOf(0) }

    var draggedPointIndex by remember { mutableIntStateOf(-1) }
    var activePointerIndex by remember { mutableIntStateOf(-1) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            isCameraActive = true
        } else {
            isCameraActive = false
            showCameraPermissionDialog = true
        }
    }

    val calculatedAngle = remember(
        interactionMode,
        pointer1AngleDeg,
        pointer2AngleDeg,
        pointVertex,
        pointRay1,
        pointRay2
    ) {
        if (interactionMode == ProtractorInteractionMode.POINTER_DRAG) {
            var diff = abs(pointer1AngleDeg - pointer2AngleDeg)
            if (diff > 180f) diff = 360f - diff
            diff
        } else {
            val v = pointVertex
            val p1 = pointRay1
            val p2 = pointRay2
            if (v != null && p1 != null && p2 != null) {
                val a1 = Math.toDegrees(atan2((v.y - p1.y).toDouble(), (p1.x - v.x).toDouble())).toFloat()
                val a2 = Math.toDegrees(atan2((v.y - p2.y).toDouble(), (p2.x - v.x).toDouble())).toFloat()
                var diff = abs(a1 - a2)
                if (diff > 180f) diff = 360f - diff
                diff
            } else {
                0f
            }
        }
    }

    val primaryAccent = MaterialTheme.colorScheme.primary
    val dialFillColor = primaryAccent.copy(alpha = if (isCameraActive) 0.18f else 0.12f)
    val sectorFillColor = primaryAccent.copy(alpha = if (isCameraActive) 0.35f else 0.22f)
    val pointerLineColor = Color(0xFF66BFA8)
    val handleColor = Color(0xFF53D2B8)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isCameraActive) Color.Transparent else MaterialTheme.colorScheme.surface)
    ) {
        if (isCameraActive) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview
                            )
                        } catch (e: Exception) {
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        val paintTickText = remember(density) {
            android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#E0E0E0")
                textSize = with(density) { 10.sp.toPx() }
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(interactionMode) {
                    detectTapGestures { offset ->
                        if (interactionMode == ProtractorInteractionMode.THREE_POINT) {
                            if (pointVertex == null) {
                                pointVertex = offset
                                threePointStep = 1
                            } else if (pointRay1 == null) {
                                pointRay1 = offset
                                threePointStep = 2
                            } else if (pointRay2 == null) {
                                pointRay2 = offset
                                threePointStep = 3
                            }
                        }
                    }
                }
                .pointerInput(interactionMode) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            if (interactionMode == ProtractorInteractionMode.POINTER_DRAG) {
                                val cx = size.width / 2f
                                val cy = size.height - 40.dp.toPx()
                                val rawAngle = calculateProtractorTouchAngle(startOffset.x, startOffset.y, cx, cy)
                                val diff1 = abs(rawAngle - pointer1AngleDeg)
                                val diff2 = abs(rawAngle - pointer2AngleDeg)
                                activePointerIndex = if (diff1 <= diff2) 1 else 2
                                if (activePointerIndex == 1) {
                                    pointer1AngleDeg = rawAngle
                                } else {
                                    pointer2AngleDeg = rawAngle
                                }
                            } else {
                                val v = pointVertex
                                val p1 = pointRay1
                                val p2 = pointRay2
                                val threshold = 60.dp.toPx()

                                val d0 = v?.let { (startOffset - it).getDistance() } ?: Float.MAX_VALUE
                                val d1 = p1?.let { (startOffset - it).getDistance() } ?: Float.MAX_VALUE
                                val d2 = p2?.let { (startOffset - it).getDistance() } ?: Float.MAX_VALUE
                                val minD = minOf(d0, minOf(d1, d2))

                                if (minD < threshold) {
                                    draggedPointIndex = when (minD) {
                                        d0 -> 0
                                        d1 -> 1
                                        else -> 2
                                    }
                                } else {
                                    when {
                                        pointVertex == null -> {
                                            pointVertex = startOffset
                                            draggedPointIndex = 0
                                            threePointStep = 1
                                        }
                                        pointRay1 == null -> {
                                            pointRay1 = startOffset
                                            draggedPointIndex = 1
                                            threePointStep = 2
                                        }
                                        pointRay2 == null -> {
                                            pointRay2 = startOffset
                                            draggedPointIndex = 2
                                            threePointStep = 3
                                        }
                                        else -> {
                                            draggedPointIndex = -1
                                        }
                                    }
                                }
                            }
                        },
                        onDragEnd = {
                            activePointerIndex = -1
                            draggedPointIndex = -1
                        },
                        onDragCancel = {
                            activePointerIndex = -1
                            draggedPointIndex = -1
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            if (interactionMode == ProtractorInteractionMode.POINTER_DRAG) {
                                val cx = size.width / 2f
                                val cy = size.height - 40.dp.toPx()
                                val currentTouch = change.position
                                val rawAngle = calculateProtractorTouchAngle(currentTouch.x, currentTouch.y, cx, cy)
                                if (activePointerIndex == 1) {
                                    pointer1AngleDeg = rawAngle
                                } else if (activePointerIndex == 2) {
                                    pointer2AngleDeg = rawAngle
                                }
                            } else {
                                val newPos = change.position
                                when (draggedPointIndex) {
                                    0 -> pointVertex = newPos
                                    1 -> pointRay1 = newPos
                                    2 -> pointRay2 = newPos
                                }
                            }
                        }
                    )
                }
        ) {
            val cx = size.width / 2f
            val cy = size.height - 40.dp.toPx()
            val radius = (size.height - 70.dp.toPx()).coerceAtLeast(100f)

            if (interactionMode == ProtractorInteractionMode.POINTER_DRAG) {
                drawArc(
                    color = dialFillColor,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(cx - radius, cy - radius),
                    size = Size(radius * 2, radius * 2)
                )

                drawLine(
                    color = Color.White.copy(alpha = 0.7f),
                    start = Offset(cx - radius, cy),
                    end = Offset(cx + radius, cy),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )

                drawArc(
                    color = Color.White.copy(alpha = 0.8f),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(cx - radius, cy - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = 2.dp.toPx())
                )

                val innerArcRadius = radius * 0.75f
                drawArc(
                    color = Color.White.copy(alpha = 0.35f),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(cx - innerArcRadius, cy - innerArcRadius),
                    size = Size(innerArcRadius * 2, innerArcRadius * 2),
                    style = Stroke(width = 1.dp.toPx())
                )

                for (deg in 0..180) {
                    val rad = Math.toRadians(deg.toDouble())
                    val cosVal = cos(rad).toFloat()
                    val sinVal = sin(rad).toFloat()

                    val tickLength = when {
                        deg % 10 == 0 -> 16.dp.toPx()
                        deg % 5 == 0 -> 10.dp.toPx()
                        else -> 5.dp.toPx()
                    }

                    val startX = cx + (radius - tickLength) * cosVal
                    val startY = cy - (radius - tickLength) * sinVal
                    val endX = cx + radius * cosVal
                    val endY = cy - radius * sinVal

                    val tickStroke = if (deg % 10 == 0) 1.5.dp.toPx() else 1.dp.toPx()
                    val tickColorAlpha = if (deg % 10 == 0) 0.9f else if (deg % 5 == 0) 0.6f else 0.35f

                    drawLine(
                        color = Color.White.copy(alpha = tickColorAlpha),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = tickStroke,
                        cap = StrokeCap.Square
                    )

                    if (deg % 10 == 0) {
                        val textRadius = radius - 24.dp.toPx()
                        val textX = cx + textRadius * cosVal
                        val textY = cy - textRadius * sinVal + 4.dp.toPx()

                        drawContext.canvas.nativeCanvas.drawText(
                            deg.toString(),
                            textX,
                            textY,
                            paintTickText
                        )
                    }
                }

                val minAngle = minOf(pointer1AngleDeg, pointer2AngleDeg)
                val sweep = abs(pointer1AngleDeg - pointer2AngleDeg)
                val startArcAngle = 360f - maxOf(pointer1AngleDeg, pointer2AngleDeg)

                drawArc(
                    color = sectorFillColor,
                    startAngle = startArcAngle,
                    sweepAngle = sweep,
                    useCenter = true,
                    topLeft = Offset(cx - radius * 0.88f, cy - radius * 0.88f),
                    size = Size(radius * 1.76f, radius * 1.76f)
                )

                val rad1 = Math.toRadians(pointer1AngleDeg.toDouble())
                val p1End = Offset(cx + (radius * 1.05f * cos(rad1)).toFloat(), cy - (radius * 1.05f * sin(rad1)).toFloat())

                drawLine(
                    color = pointerLineColor,
                    start = Offset(cx, cy),
                    end = p1End,
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawCircle(color = handleColor, radius = 9.dp.toPx(), center = p1End)
                drawCircle(color = Color.White, radius = 4.dp.toPx(), center = p1End)

                val rad2 = Math.toRadians(pointer2AngleDeg.toDouble())
                val p2End = Offset(cx + (radius * 1.05f * cos(rad2)).toFloat(), cy - (radius * 1.05f * sin(rad2)).toFloat())

                drawLine(
                    color = pointerLineColor,
                    start = Offset(cx, cy),
                    end = p2End,
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawCircle(color = handleColor, radius = 9.dp.toPx(), center = p2End)
                drawCircle(color = Color.White, radius = 4.dp.toPx(), center = p2End)

                drawCircle(color = Color.White, radius = 4.dp.toPx(), center = Offset(cx, cy))
            } else {
                val v = pointVertex
                val p1 = pointRay1
                val p2 = pointRay2

                if (v != null) {
                    drawCircle(color = Color(0xFFFFD54F), radius = 10.dp.toPx(), center = v)
                    drawCircle(color = Color.Black, radius = 4.dp.toPx(), center = v)

                    if (p1 != null) {
                        drawLine(
                            color = pointerLineColor,
                            start = v,
                            end = p1,
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawCircle(color = handleColor, radius = 8.dp.toPx(), center = p1)
                        drawCircle(color = Color.White, radius = 3.5.dp.toPx(), center = p1)
                    }

                    if (p2 != null) {
                        drawLine(
                            color = pointerLineColor,
                            start = v,
                            end = p2,
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawCircle(color = handleColor, radius = 8.dp.toPx(), center = p2)
                        drawCircle(color = Color.White, radius = 3.5.dp.toPx(), center = p2)
                    }

                    if (p1 != null && p2 != null) {
                        val d1 = (p1 - v).getDistance().coerceAtLeast(30f)
                        val d2 = (p2 - v).getDistance().coerceAtLeast(30f)
                        val arcRadius = minOf(d1, d2) * 0.45f

                        val a1 = Math.toDegrees(atan2((v.y - p1.y).toDouble(), (p1.x - v.x).toDouble())).toFloat()
                        val a2 = Math.toDegrees(atan2((v.y - p2.y).toDouble(), (p2.x - v.x).toDouble())).toFloat()

                        val a1Norm = (a1 + 360f) % 360f
                        val a2Norm = (a2 + 360f) % 360f

                        val sweep = ((a2Norm - a1Norm + 360f) % 360f).let {
                            if (it > 180f) it - 360f else it
                        }

                        val startSweep = (360f - a1Norm) % 360f

                        drawArc(
                            color = sectorFillColor,
                            startAngle = startSweep,
                            sweepAngle = -sweep,
                            useCenter = true,
                            topLeft = Offset(v.x - arcRadius, v.y - arcRadius),
                            size = Size(arcRadius * 2, arcRadius * 2)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.widthIn(min = 220.dp)
            ) {
                SegmentedButton(
                    selected = interactionMode == ProtractorInteractionMode.POINTER_DRAG,
                    onClick = { interactionMode = ProtractorInteractionMode.POINTER_DRAG },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text(
                        text = stringResource(R.string.ruler_pointer_mode),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                SegmentedButton(
                    selected = interactionMode == ProtractorInteractionMode.THREE_POINT,
                    onClick = {
                        interactionMode = ProtractorInteractionMode.THREE_POINT
                        if (threePointStep == 0) {
                            threePointStep = 0
                        }
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text(
                        text = stringResource(R.string.ruler_three_point_mode),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (interactionMode == ProtractorInteractionMode.THREE_POINT) {
                    FilledTonalIconButton(
                        onClick = {
                            pointVertex = null
                            pointRay1 = null
                            pointRay2 = null
                            threePointStep = 0
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RestartAlt,
                            contentDescription = stringResource(R.string.ruler_reset_points)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                FilledTonalIconButton(
                    onClick = {
                        if (isCameraActive) {
                            isCameraActive = false
                        } else {
                            val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                                isCameraActive = true
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    },
                    colors = if (isCameraActive) {
                        IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = pointerLineColor,
                            contentColor = Color(0xFF00382E)
                        )
                    } else {
                        IconButtonDefaults.filledTonalIconButtonColors()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CameraAlt,
                        contentDescription = stringResource(R.string.ruler_camera_mode)
                    )
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 54.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f),
            shadowElevation = 3.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = String.format(Locale.getDefault(), "%.1f°", calculatedAngle),
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                    fontWeight = FontWeight.Bold,
                    color = pointerLineColor
                )
            }
        }

        if (interactionMode == ProtractorInteractionMode.THREE_POINT) {
            val stepHint = when (threePointStep) {
                0 -> stringResource(R.string.ruler_three_point_step_vertex)
                1 -> stringResource(R.string.ruler_three_point_step_p1)
                2 -> stringResource(R.string.ruler_three_point_step_p2)
                else -> stringResource(R.string.ruler_three_point_done)
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 58.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.88f)
            ) {
                Text(
                    text = stepHint,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }

        if (showCameraPermissionDialog) {
            AlertDialog(
                onDismissRequest = { showCameraPermissionDialog = false },
                title = { Text(stringResource(R.string.ruler_camera_permission_title)) },
                text = { Text(stringResource(R.string.ruler_camera_permission_desc)) },
                confirmButton = {
                    Button(
                        onClick = {
                            showCameraPermissionDialog = false
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }
                    ) {
                        Text(stringResource(R.string.ruler_camera_permission_settings))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCameraPermissionDialog = false }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                }
            )
        }
    }
}

private fun calculateProtractorTouchAngle(touchX: Float, touchY: Float, cx: Float, cy: Float): Float {
    val dx = touchX - cx
    val dy = cy - touchY
    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    if (angle < 0f) {
        angle = if (dx < 0f) 180f else 0f
    }
    return angle.coerceIn(0f, 180f)
}
