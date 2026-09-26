package com.nanami.koishi.feature.tools.mini_apps.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.nanami.koishi.feature.tools.mini_apps.engine.MiniAppEntry
import kotlin.math.roundToInt

/**
 * 支持长按拖动排序的条目列表，拖动过程只做位移动画，松手后才提交新顺序
 */
@Composable
fun MiniAppReorderableList(
    entries: List<MiniAppEntry>,
    onOpenEntry: (MiniAppEntry) -> Unit,
    onRemoveEntry: (MiniAppEntry) -> Unit,
    onMoveEntry: (fromIndex: Int, toIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState()
) {
    val density = LocalDensity.current
    val spacingPx = with(density) { ITEM_SPACING.toPx() }
    val liftShape = MaterialTheme.shapes.large

    var draggingIndex by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var targetIndex by remember { mutableIntStateOf(-1) }

    fun stepPx(): Float {
        val visible = listState.layoutInfo.visibleItemsInfo
        return when {
            visible.size >= 2 -> (visible[1].offset - visible[0].offset).toFloat()
            visible.isNotEmpty() -> visible[0].size + spacingPx
            else -> 0f
        }
    }

    fun visibleSlotRange(): IntRange {
        val visible = listState.layoutInfo.visibleItemsInfo
        val first = visible.firstOrNull()?.index ?: 0
        val last = visible.lastOrNull()?.index ?: entries.lastIndex
        return if (first <= last) first..last else last..first
    }

    fun resolveTarget(from: Int, offset: Float): Int {
        val step = stepPx()
        if (step <= 0f) return from
        val slot = from + (offset / step).roundToInt()
        return slot.coerceIn(visibleSlotRange().first, visibleSlotRange().last)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(ITEM_SPACING)
    ) {
        itemsIndexed(
            items = entries,
            key = { _, entry -> entry.id }
        ) { index, entry ->
            val isDragging = index == draggingIndex
            val slotShift = when {
                isDragging -> 0
                draggingIndex in 0 until targetIndex &&
                        index > draggingIndex && index <= targetIndex -> -1
                targetIndex in 0 until draggingIndex &&
                        index >= targetIndex && index < draggingIndex -> 1
                else -> 0
            }
            val step = stepPx()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(if (isDragging) 1f else 0f)
                    .graphicsLayer {
                        if (isDragging) {
                            translationY = dragOffset
                            scaleX = LIFT_SCALE
                            scaleY = LIFT_SCALE
                            shape = liftShape
                            shadowElevation = LIFT_ELEVATION.toPx()
                        } else {
                            translationY = slotShift * step
                        }
                    }
                    .pointerInput(entry.id, index) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggingIndex = index
                                targetIndex = index
                                dragOffset = 0f
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                dragOffset += amount.y
                                targetIndex = resolveTarget(draggingIndex, dragOffset)
                            },
                            onDragEnd = {
                                if (draggingIndex >= 0) onMoveEntry(draggingIndex, targetIndex)
                                draggingIndex = -1
                                targetIndex = -1
                                dragOffset = 0f
                            },
                            onDragCancel = {
                                draggingIndex = -1
                                targetIndex = -1
                                dragOffset = 0f
                            }
                        )
                    }
            ) {
                MiniAppListItem(
                    entry = entry,
                    onClick = { onOpenEntry(entry) },
                    onRemove = { onRemoveEntry(entry) }
                )
            }
        }
    }
}

private val ITEM_SPACING = 10.dp
private val LIFT_ELEVATION = 10.dp
private const val LIFT_SCALE = 1.02f
