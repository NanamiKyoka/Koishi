package com.nanami.koishi.feature.tools.bmi_calculator.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import com.nanami.koishi.feature.tools.bmi_calculator.BmiRecordUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BmiTrendChartTest {

    private val paddings = ChartPaddings(
        left = 44f,
        top = 14f,
        right = 12f,
        bottom = 26f
    )

    private val plot = PlotRect(
        left = paddings.left,
        top = paddings.top,
        width = (400f - paddings.left - paddings.right),
        height = (196f - paddings.top - paddings.bottom)
    )

    private fun record(bmi: Double, id: String = "r-$bmi"): BmiRecordUi =
        BmiRecordUi(
            id = id,
            bmi = bmi,
            bmiText = bmi.toString(),
            heightText = "170",
            weightText = "65",
            timeText = "9月27日",
            axisLabel = "9/27",
            category = com.nanami.koishi.feature.tools.bmi_calculator.engine.BmiCategory.NORMAL,
            deltaDirection = com.nanami.koishi.feature.tools.bmi_calculator.BmiTrendDirection.FLAT,
            deltaText = "",
            isLatest = false
        )

    @Test
    fun `single record is centered horizontally`() {
        val points = computePointOffsets(
            records = listOf(record(22.0)),
            plot = plot,
            lowerBound = 18.0,
            upperBound = 24.0
        )

        assertEquals(1, points.size)
        val expectedX = plot.left + plot.width / 2f
        assertEquals(expectedX, points.first().x, 0.001f)
    }

    @Test
    fun `points spread evenly across plot width for multiple records`() {
        val records = listOf(record(18.0), record(22.0), record(24.0))

        val points = computePointOffsets(
            records = records,
            plot = plot,
            lowerBound = 17.0,
            upperBound = 25.0
        )

        assertEquals(3, points.size)
        val step = plot.width / 2f
        assertEquals(plot.left, points[0].x, 0.001f)
        assertEquals(plot.left + step, points[1].x, 0.001f)
        assertEquals(plot.left + plot.width, points[2].x, 0.001f)
    }

    @Test
    fun `higher bmi maps to smaller y coordinate`() {
        val points = computePointOffsets(
            records = listOf(record(18.0), record(24.0)),
            plot = plot,
            lowerBound = 17.0,
            upperBound = 25.0
        )

        assertTrue(points[0].y > points[1].y)
        assertTrue(points.all { it.y in plot.top..plot.bottom })
    }

    @Test
    fun `out of range bmi is clamped onto the plot`() {
        val points = computePointOffsets(
            records = listOf(record(10.0), record(40.0)),
            plot = plot,
            lowerBound = 18.0,
            upperBound = 24.0
        )

        assertEquals(plot.bottom, points[0].y, 0.001f)
        assertEquals(plot.top, points[1].y, 0.001f)
    }

    @Test
    fun `resolvePlotRect clamps to positive dimensions`() {
        val resolved = resolvePlotRect(
            width = 50f,
            height = 40f,
            paddings = paddings
        )

        assertEquals(1f, resolved.width, 0.001f)
        assertEquals(1f, resolved.height, 0.001f)
    }

    @Test
    fun `resolveChartPaddings converts dp to px`() {
        val density = object : Density {
            override val density: Float = 2f
            override val fontScale: Float = 1f
        }

        val resolved = density.resolveChartPaddings()

        assertEquals(88f, resolved.left, 0.001f)
        assertEquals(28f, resolved.top, 0.001f)
        assertEquals(24f, resolved.right, 0.001f)
        assertEquals(52f, resolved.bottom, 0.001f)
    }

    @Test
    fun `empty record list produces empty offsets without touching the plot`() {
        val points = computePointOffsets(
            records = emptyList(),
            plot = plot,
            lowerBound = 18.0,
            upperBound = 24.0
        )

        assertTrue(points.isEmpty())
    }
}
