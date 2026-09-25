package com.nanami.koishi.feature.tools.ruler.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanami.koishi.R
import com.nanami.koishi.feature.tools.ruler.engine.RulerPreferences
import kotlin.math.roundToInt

@Composable
fun StraightRulerView(
    onOpenCalibration: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    var pxPerMm by remember { mutableFloatStateOf(RulerPreferences.getCalibratedPxPerMm(context)) }
    var measurementLineX by remember { mutableFloatStateOf(10f * pxPerMm) }

    LaunchedEffect(Unit) {
        pxPerMm = RulerPreferences.getCalibratedPxPerMm(context)
        measurementLineX = 10f * pxPerMm
    }

    val lengthMm = remember(measurementLineX, pxPerMm) {
        (measurementLineX / pxPerMm).coerceAtLeast(0f)
    }
    val integerCm = remember(lengthMm) {
        (lengthMm / 10f).toInt()
    }
    val remainderMm = remember(lengthMm) {
        (lengthMm.roundToInt() % 10)
    }

    val mintColor = Color(0xFF66BFA8)
    val tickColor = Color(0xFFD6D6D6)
    val minorTickColor = Color(0xFF888888)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    measurementLineX = offset.x.coerceIn(0f, size.width.toFloat())
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { startOffset ->
                        measurementLineX = startOffset.x.coerceIn(0f, size.width.toFloat())
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        measurementLineX = (measurementLineX + dragAmount.x).coerceIn(0f, size.width.toFloat())
                    }
                )
            }
    ) {
        val paintNumber = remember(density) {
            android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#66BFA8")
                textSize = with(density) { 15.sp.toPx() }
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val cm10TickHeight = 32.dp.toPx()
            val mm5TickHeight = 22.dp.toPx()
            val mm1TickHeight = 14.dp.toPx()
            val textBaselineY = 48.dp.toPx()

            var mm = 0
            while (true) {
                val tickX = mm * pxPerMm
                if (tickX > size.width) break

                when {
                    mm % 10 == 0 -> {
                        drawLine(
                            color = tickColor,
                            start = Offset(tickX, 0f),
                            end = Offset(tickX, cm10TickHeight),
                            strokeWidth = 1.5.dp.toPx(),
                            cap = StrokeCap.Square
                        )
                        val cmValue = mm / 10
                        drawContext.canvas.nativeCanvas.drawText(
                            cmValue.toString(),
                            tickX,
                            textBaselineY,
                            paintNumber
                        )
                    }
                    mm % 5 == 0 -> {
                        drawLine(
                            color = tickColor,
                            start = Offset(tickX, 0f),
                            end = Offset(tickX, mm5TickHeight),
                            strokeWidth = 1.2.dp.toPx(),
                            cap = StrokeCap.Square
                        )
                    }
                    else -> {
                        drawLine(
                            color = minorTickColor,
                            start = Offset(tickX, 0f),
                            end = Offset(tickX, mm1TickHeight),
                            strokeWidth = 0.8.dp.toPx(),
                            cap = StrokeCap.Square
                        )
                    }
                }
                mm++
            }

            drawLine(
                color = Color.White,
                start = Offset(measurementLineX, 0f),
                end = Offset(measurementLineX, size.height),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Square
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-10).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(92.dp),
                contentAlignment = Alignment.TopStart
            ) {
                Box(
                    modifier = Modifier
                        .size(74.dp)
                        .background(Color(0xFFEDECE8), CircleShape)
                        .border(2.dp, Color(0xFFDEDCD5), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = integerCm.toString(),
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF222222)
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(34.dp)
                        .offset(x = 2.dp, y = 2.dp)
                        .background(Color(0xFFFAF7EE), CircleShape)
                        .border(2.dp, Color(0xFFE5E0D4), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = remainderMm.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF222222)
                    )
                }
            }

            Surface(
                modifier = Modifier.padding(top = 8.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f)
            ) {
                Text(
                    text = "${String.format("%.1f", lengthMm / 10f)} cm (${lengthMm.roundToInt()} mm)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Button(
            onClick = onOpenCalibration,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 16.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF293B35),
                contentColor = mintColor
            )
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.ruler_calibrate),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
