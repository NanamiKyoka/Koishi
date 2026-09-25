package com.nanami.koishi.feature.tools.decision_maker.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

internal data class TubePalette(
    val facetFront: Color,
    val facetLeft: Color,
    val facetRight: Color,
    val plinthFront: Color,
    val plinthLeft: Color,
    val plinthRight: Color,
    val chamfer: Color,
    val capTop: Color,
    val capBottom: Color,
    val capEdge: Color,
    val edgeLine: Color,
    val bandFill: Color,
    val bandStroke: Color,
    val cavityTop: Color,
    val cavityBottom: Color,
    val cavityRim: Color,
    val stickBody: Color,
    val stickEdge: Color,
    val stickTip: Color,
    val stickTipEdge: Color,
    val stickInk: Color
)

@Composable
internal fun rememberTubePalette(): TubePalette {
    val scheme = MaterialTheme.colorScheme
    return remember(scheme) {
        val dark = scheme.surface.luminance() < 0.5f
        TubePalette(
            facetFront = scheme.surfaceContainerHighest,
            facetLeft = scheme.surfaceContainerHigh,
            facetRight = scheme.surfaceContainer,
            plinthFront = scheme.surfaceContainerHigh,
            plinthLeft = scheme.surfaceContainer,
            plinthRight = scheme.surfaceContainerLow,
            chamfer = scheme.surfaceContainer,
            capTop = scheme.surfaceContainerHigh,
            capBottom = scheme.surfaceContainerHighest,
            capEdge = scheme.outlineVariant.copy(alpha = 0.65f),
            edgeLine = scheme.outlineVariant.copy(alpha = 0.55f),
            bandFill = scheme.primaryContainer,
            bandStroke = scheme.primary.copy(alpha = 0.85f),
            cavityTop = Color.Black.copy(alpha = if (dark) 0.68f else 0.36f),
            cavityBottom = Color.Black.copy(alpha = if (dark) 0.46f else 0.18f),
            cavityRim = scheme.outlineVariant.copy(alpha = if (dark) 0.45f else 0.65f),
            stickBody = if (dark) Color(0xFFF2E8D6) else Color(0xFFFCF8EF),
            stickEdge = if (dark) Color(0xFFC8B79A) else Color(0xFFDED1B8),
            stickTip = if (dark) Color(0xFFDE6A55) else Color(0xFFC0392B),
            stickTipEdge = if (dark) Color(0xFFB0492F) else Color(0xFF8E2A1E),
            stickInk = Color(0xFF3B2E23)
        )
    }
}

private const val FACET_SEAM_OVERLAP = 1.2f
private const val BAND_TOP_RATIO = 0.30f
private const val BAND_HEIGHT_RATIO = 0.088f
private const val CHAMFER_RATIO = 0.022f

internal fun DrawScope.drawOmikujiTube(geometry: HexTubeGeometry, palette: TubePalette) {
    val facets = listOf(
        Triple(TubeFacet.LEFT, palette.facetLeft, palette.plinthLeft),
        Triple(TubeFacet.FRONT, palette.facetFront, palette.plinthFront),
        Triple(TubeFacet.RIGHT, palette.facetRight, palette.plinthRight)
    )

    facets.forEach { (facet, bodyColor, plinthColor) ->
        drawPath(
            path = geometry.facetQuad(facet, 0f, geometry.bodyHeight, edgeOverlap = FACET_SEAM_OVERLAP),
            color = bodyColor
        )
    }

    val chamferHeight = geometry.bodyHeight * CHAMFER_RATIO
    facets.forEach { (facet, _, _) ->
        drawPath(
            path = geometry.facetQuad(
                facet = facet,
                from = geometry.bodyHeight - chamferHeight,
                to = geometry.bodyHeight,
                edgeOverlap = FACET_SEAM_OVERLAP
            ),
            color = palette.chamfer
        )
    }

    val plinthFrom = geometry.bodyHeight
    val plinthTo = geometry.bodyHeight + geometry.plinthHeight
    facets.forEach { (facet, _, plinthColor) ->
        drawPath(
            path = geometry.facetQuad(
                facet = facet,
                from = plinthFrom,
                to = plinthTo,
                bottomScale = geometry.plinthScale,
                edgeOverlap = FACET_SEAM_OVERLAP
            ),
            color = plinthColor
        )
    }

    val bandTop = geometry.bodyHeight * BAND_TOP_RATIO
    val bandBottom = bandTop + geometry.bodyHeight * BAND_HEIGHT_RATIO
    facets.forEach { (facet, _, _) ->
        val bandPath = geometry.facetQuad(
            facet = facet,
            from = bandTop,
            to = bandBottom,
            edgeOverlap = FACET_SEAM_OVERLAP
        )
        drawPath(path = bandPath, color = palette.bandFill)
        drawPath(path = bandPath, color = palette.bandStroke, style = Stroke(width = 1.dp.toPx()))
    }

    drawPath(
        path = geometry.silhouettePath(),
        color = palette.edgeLine,
        style = Stroke(width = 1.dp.toPx())
    )
    drawPath(
        path = geometry.plinthBottomPath(),
        color = palette.edgeLine,
        style = Stroke(width = 1.dp.toPx())
    )

    drawLine(
        color = palette.edgeLine,
        start = Offset(geometry.frontLeftX, geometry.capFrontY),
        end = Offset(geometry.frontLeftX, geometry.bodyBottomY),
        strokeWidth = 1.dp.toPx()
    )
    drawLine(
        color = palette.edgeLine,
        start = Offset(geometry.frontRightX, geometry.capFrontY),
        end = Offset(geometry.frontRightX, geometry.bodyBottomY),
        strokeWidth = 1.dp.toPx()
    )

    drawPath(
        path = geometry.capPath(),
        brush = Brush.verticalGradient(
            colors = listOf(palette.capTop, palette.capBottom),
            startY = geometry.capBackY,
            endY = geometry.capFrontY
        )
    )
    drawPath(
        path = geometry.capPath(),
        color = palette.capEdge,
        style = Stroke(width = 1.dp.toPx())
    )

    drawTubeSlit(geometry, palette)
}

private fun DrawScope.drawTubeSlit(geometry: HexTubeGeometry, palette: TubePalette) {
    val corner = CornerRadius(geometry.slitHeight / 2f, geometry.slitHeight / 2f)

    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(palette.cavityTop, palette.cavityBottom),
            startY = geometry.slitTopY,
            endY = geometry.slitBottomY
        ),
        topLeft = geometry.slitTopLeft,
        size = geometry.slitSize,
        cornerRadius = corner
    )

    drawRoundRect(
        color = palette.cavityTop,
        topLeft = geometry.slitTopLeft,
        size = Size(geometry.slitWidth, geometry.slitHeight * 0.34f),
        cornerRadius = corner
    )

    drawRoundRect(
        color = palette.cavityRim,
        topLeft = geometry.slitTopLeft,
        size = geometry.slitSize,
        cornerRadius = corner,
        style = Stroke(width = 1.dp.toPx())
    )
}

internal fun HexTubeGeometry.silhouettePath(): Path = Path().apply {
    moveTo(leftX, rimCenterY)
    lineTo(frontLeftX, capBackY)
    lineTo(frontRightX, capBackY)
    lineTo(rightX, rimCenterY)
    lineTo(rightX, rimCenterY + bodyHeight)
    lineTo(frontRightX, bodyBottomY)
    lineTo(frontLeftX, bodyBottomY)
    lineTo(leftX, rimCenterY + bodyHeight)
    close()
}

internal fun HexTubeGeometry.plinthBottomPath(): Path = Path().apply {
    moveTo(scaledX(leftX, plinthScale), rimCenterY + bodyHeight + plinthHeight)
    lineTo(scaledX(frontLeftX, plinthScale), bodyBottomY + plinthHeight)
    lineTo(scaledX(frontRightX, plinthScale), bodyBottomY + plinthHeight)
    lineTo(scaledX(rightX, plinthScale), rimCenterY + bodyHeight + plinthHeight)
}
