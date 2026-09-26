package com.nanami.koishi.feature.tools.bmi_calculator.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanami.koishi.feature.tools.bmi_calculator.BmiRecordUi
import com.nanami.koishi.feature.tools.bmi_calculator.engine.BmiCalculator
import com.nanami.koishi.feature.tools.bmi_calculator.engine.BmiCategory
import kotlin.math.max
import kotlin.math.min

private val ChartHeight = 196.dp
private val PlotLeftPadding = 44.dp
private val PlotRightPadding = 12.dp
private val PlotTopPadding = 14.dp
private val PlotBottomPadding = 26.dp
private val TapTolerance = 48.dp

internal data class ChartPaddings(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

internal data class PlotRect(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float
) {
    val right: Float get() = left + width
    val bottom: Float get() = top + height
}

@Composable
fun BmiTrendChart(
    records: List<BmiRecordUi>,
    selectedRecordId: String?,
    onSelectRecord: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (records.isEmpty()) return

    val bandColors = rememberBmiBandColors()
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    val primary = MaterialTheme.colorScheme.primary
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surface = MaterialTheme.colorScheme.surface
    val labelStyle = remember(axisColor) {
        TextStyle(color = axisColor, fontSize = 10.sp)
    }
    val dashEffect = remember(density) {
        PathEffect.dashPathEffect(
            floatArrayOf(with(density) { 3.dp.toPx() }, with(density) { 4.dp.toPx() })
        )
    }
    val paddings = remember(density) { density.resolveChartPaddings() }

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    val lowerBound = remember(records) {
        min(records.minOf { it.bmi }, BmiCalculator.UNDERWEIGHT_MAX_BMI) - 0.8
    }
    val upperBound = remember(records) {
        max(records.maxOf { it.bmi }, BmiCalculator.NORMAL_MAX_BMI) + 0.8
    }

    val tapTargets = remember(records, canvasSize, lowerBound, upperBound, paddings) {
        if (canvasSize.width <= 0 || canvasSize.height <= 0) {
            emptyList()
        } else {
            computePointOffsets(
                records = records,
                plot = resolvePlotRect(
                    width = canvasSize.width.toFloat(),
                    height = canvasSize.height.toFloat(),
                    paddings = paddings
                ),
                lowerBound = lowerBound,
                upperBound = upperBound
            )
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(ChartHeight)
            .onSizeChanged { canvasSize = it }
            .pointerInput(tapTargets) {
                val tolerance = TapTolerance.toPx()
                detectTapGestures { position ->
                    val nearestIndex = tapTargets.indices.minByOrNull { index ->
                        (tapTargets[index] - position).getDistance()
                    }
                    val index = nearestIndex ?: return@detectTapGestures
                    if ((tapTargets[index] - position).getDistance() <= tolerance) {
                        records.getOrNull(index)?.let { onSelectRecord(it.id) }
                    }
                }
            }
    ) {
        val plot = resolvePlotRect(size.width, size.height, paddings)
        val points = computePointOffsets(records, plot, lowerBound, upperBound)
        val span = (upperBound - lowerBound).coerceAtLeast(0.1)

        fun yOf(value: Double): Float =
            plot.bottom - (((value - lowerBound) / span).coerceIn(0.0, 1.0)).toFloat() * plot.height

        BmiCategory.entries.forEach { category ->
            val range = BmiCalculator.categoryRangeOf(category)
            val bandTop = yOf(min(range.endInclusive, upperBound))
            val bandBottom = yOf(max(range.start, lowerBound))
            if (bandBottom - bandTop > 1f) {
                drawRect(
                    color = bandColors.of(category).copy(alpha = 0.14f),
                    topLeft = Offset(plot.left, bandTop),
                    size = Size(plot.width, bandBottom - bandTop)
                )
            }
        }

        val boundaries = listOf(
            BmiCalculator.UNDERWEIGHT_MAX_BMI,
            BmiCalculator.NORMAL_MAX_BMI,
            BmiCalculator.OVERWEIGHT_MAX_BMI
        )
        boundaries.filter { it in lowerBound..upperBound }.forEach { value ->
            val y = yOf(value)
            drawLine(
                color = axisColor.copy(alpha = 0.32f),
                start = Offset(plot.left, y),
                end = Offset(plot.right, y),
                strokeWidth = 1f,
                pathEffect = dashEffect
            )
            val layout = textMeasurer.measure(
                text = BmiCalculator.formatValue(value),
                style = labelStyle
            )
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(
                    x = plot.left - 6.dp.toPx() - layout.size.width,
                    y = y - layout.size.height / 2f
                )
            )
        }

        val labelIndices = when {
            records.size == 1 -> listOf(0)
            records.size <= 3 -> records.indices.toList()
            records.size == 4 -> listOf(0, records.lastIndex)
            else -> listOf(0, records.size / 2, records.lastIndex)
        }
        labelIndices.forEach { index ->
            val layout = textMeasurer.measure(text = records[index].axisLabel, style = labelStyle)
            val x = (points[index].x - layout.size.width / 2f)
                .coerceIn(plot.left, plot.right - layout.size.width)
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(x = x, y = plot.bottom + 6.dp.toPx())
            )
        }

        val selectedIndex = records.indexOfFirst { it.id == selectedRecordId }
        if (selectedIndex in points.indices) {
            val point = points[selectedIndex]
            drawLine(
                color = primary.copy(alpha = 0.45f),
                start = Offset(point.x, plot.top),
                end = Offset(point.x, plot.bottom),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
            )
        }

        if (points.size > 1) {
            val linePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
            }
            val areaPath = Path().apply {
                moveTo(points.first().x, plot.bottom)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, plot.bottom)
                close()
            }
            drawPath(
                path = areaPath,
                brush = Brush.verticalGradient(
                    colors = listOf(primary.copy(alpha = 0.28f), primary.copy(alpha = 0f)),
                    startY = plot.top,
                    endY = plot.bottom
                )
            )
            drawPath(
                path = linePath,
                color = primary,
                style = Stroke(
                    width = 2.5.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        points.forEachIndexed { index, point ->
            val color = bandColors.of(records[index].category)
            if (records[index].id == selectedRecordId) {
                drawCircle(color = color.copy(alpha = 0.22f), radius = 12.dp.toPx(), center = point)
            }
            drawCircle(color = surface, radius = 6.5.dp.toPx(), center = point)
            drawCircle(color = color, radius = 4.5.dp.toPx(), center = point)
        }
    }
}

@Composable
fun BmiChartLegend(modifier: Modifier = Modifier) {
    val bandColors = rememberBmiBandColors()

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BmiCategory.entries.forEach { category ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(bandColors.of(category))
                )
                Text(
                    text = stringResource(category.labelRes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
    }
}

internal fun resolvePlotRect(
    width: Float,
    height: Float,
    paddings: ChartPaddings
): PlotRect = PlotRect(
    left = paddings.left,
    top = paddings.top,
    width = (width - paddings.left - paddings.right).coerceAtLeast(1f),
    height = (height - paddings.top - paddings.bottom).coerceAtLeast(1f)
)

internal fun computePointOffsets(
    records: List<BmiRecordUi>,
    plot: PlotRect,
    lowerBound: Double,
    upperBound: Double
): List<Offset> {
    val span = (upperBound - lowerBound).coerceAtLeast(0.1)
    val step = if (records.size > 1) plot.width / (records.size - 1) else 0f

    return records.mapIndexed { index, record ->
        val x = if (records.size > 1) plot.left + step * index else plot.left + plot.width / 2f
        val ratio = ((record.bmi - lowerBound) / span).coerceIn(0.0, 1.0).toFloat()
        Offset(x, plot.bottom - plot.height * ratio)
    }
}

internal fun Density.resolveChartPaddings(): ChartPaddings = ChartPaddings(
    left = PlotLeftPadding.toPx(),
    top = PlotTopPadding.toPx(),
    right = PlotRightPadding.toPx(),
    bottom = PlotBottomPadding.toPx()
)
