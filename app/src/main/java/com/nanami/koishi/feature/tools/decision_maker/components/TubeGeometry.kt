package com.nanami.koishi.feature.tools.decision_maker.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path

internal enum class TubeFacet { LEFT, FRONT, RIGHT }

internal const val TUBE_SIDE_FACET_RATIO = 0.25f

/**
 * 六棱柱签筒的正面投影几何：顶盖为带平前缘的六边形，筒身由左 / 正 / 右三个立面围成
 */
internal data class HexTubeGeometry(
    val centerX: Float,
    val width: Float,
    val rimCenterY: Float,
    val capHalfHeight: Float,
    val bodyHeight: Float,
    val plinthHeight: Float,
    val plinthScale: Float,
    val slitWidth: Float,
    val slitHeight: Float
) {
    val leftX: Float = centerX - width / 2f
    val rightX: Float = centerX + width / 2f
    val frontLeftX: Float = leftX + width * TUBE_SIDE_FACET_RATIO
    val frontRightX: Float = rightX - width * TUBE_SIDE_FACET_RATIO

    val capBackY: Float = rimCenterY - capHalfHeight
    val capFrontY: Float = rimCenterY + capHalfHeight
    val bodyBottomY: Float = capFrontY + bodyHeight
    val plinthBottomY: Float = bodyBottomY + plinthHeight

    val slitLeftX: Float = centerX - slitWidth / 2f
    val slitRightX: Float = centerX + slitWidth / 2f
    val slitTopY: Float = rimCenterY - slitHeight / 2f
    val slitBottomY: Float = rimCenterY + slitHeight / 2f

    val slitSize: Size = Size(slitWidth, slitHeight)
    val slitTopLeft: Offset = Offset(slitLeftX, slitTopY)

    fun scaledX(x: Float, scale: Float): Float = centerX + (x - centerX) * scale
}

internal data class FacetEdge(
    val leftX: Float,
    val leftY: Float,
    val rightX: Float,
    val rightY: Float
)

internal fun HexTubeGeometry.facetEdge(facet: TubeFacet): FacetEdge = when (facet) {
    TubeFacet.LEFT -> FacetEdge(leftX, rimCenterY, frontLeftX, capFrontY)
    TubeFacet.FRONT -> FacetEdge(frontLeftX, capFrontY, frontRightX, capFrontY)
    TubeFacet.RIGHT -> FacetEdge(frontRightX, capFrontY, rightX, rimCenterY)
}

/**
 * 立面沿垂直方向 [from, to] 区间的四边形，[bottomScale] 用于生成外扩的棱台底座
 */
internal fun HexTubeGeometry.facetQuad(
    facet: TubeFacet,
    from: Float,
    to: Float,
    bottomScale: Float = 1f,
    edgeOverlap: Float = 0f
): Path {
    val edge = facetEdge(facet)
    val inset = edgeOverlap / 2f
    val topLeftX = edge.leftX - inset
    val topRightX = edge.rightX + inset
    val bottomLeftX = scaledX(edge.leftX, bottomScale) - inset
    val bottomRightX = scaledX(edge.rightX, bottomScale) + inset

    return Path().apply {
        moveTo(topLeftX, edge.leftY + from)
        lineTo(topRightX, edge.rightY + from)
        lineTo(bottomRightX, edge.rightY + to)
        lineTo(bottomLeftX, edge.leftY + to)
        close()
    }
}

internal fun HexTubeGeometry.capPath(): Path = Path().apply {
    moveTo(leftX, rimCenterY)
    lineTo(frontLeftX, capBackY)
    lineTo(frontRightX, capBackY)
    lineTo(rightX, rimCenterY)
    lineTo(frontRightX, capFrontY)
    lineTo(frontLeftX, capFrontY)
    close()
}
