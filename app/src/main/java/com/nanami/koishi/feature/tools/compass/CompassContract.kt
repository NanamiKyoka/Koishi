package com.nanami.koishi.feature.tools.compass

import androidx.annotation.StringRes
import com.nanami.koishi.R
import com.nanami.koishi.feature.tools.compass.engine.CompassFrame
import kotlin.math.floor

enum class CardinalDirection(@StringRes val labelRes: Int) {
    NORTH(R.string.compass_direction_north),
    NORTHEAST(R.string.compass_direction_northeast),
    EAST(R.string.compass_direction_east),
    SOUTHEAST(R.string.compass_direction_southeast),
    SOUTH(R.string.compass_direction_south),
    SOUTHWEST(R.string.compass_direction_southwest),
    WEST(R.string.compass_direction_west),
    NORTHWEST(R.string.compass_direction_northwest)
}

data class CompassUiState(
    val isSupported: Boolean = true,
    val isHeld: Boolean = false,
    val frame: CompassFrame = CompassFrame(),
    @StringRes val errorRes: Int? = null
) {
    val direction: CardinalDirection
        get() {
            val index = floor((frame.headingDeg + 22.5) / 45.0).toInt() % CardinalDirection.entries.size
            return CardinalDirection.entries[index]
        }
}

sealed interface CompassUiEvent {
    data object OnStart : CompassUiEvent
    data object OnStop : CompassUiEvent
    data object OnToggleHold : CompassUiEvent
    data object OnDismissError : CompassUiEvent
}
