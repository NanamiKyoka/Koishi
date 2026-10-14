package com.nanami.koishi.feature.tools.compass

import org.junit.Assert.assertEquals
import org.junit.Test

class CompassContractTest {

    @Test
    fun `maps heading to cardinal directions`() {
        assertEquals(CardinalDirection.NORTH, CompassUiState(frame = frameWithHeading(0.0)).direction)
        assertEquals(CardinalDirection.NORTHEAST, CompassUiState(frame = frameWithHeading(45.0)).direction)
        assertEquals(CardinalDirection.EAST, CompassUiState(frame = frameWithHeading(90.0)).direction)
        assertEquals(CardinalDirection.SOUTHEAST, CompassUiState(frame = frameWithHeading(135.0)).direction)
        assertEquals(CardinalDirection.SOUTH, CompassUiState(frame = frameWithHeading(180.0)).direction)
        assertEquals(CardinalDirection.SOUTHWEST, CompassUiState(frame = frameWithHeading(225.0)).direction)
        assertEquals(CardinalDirection.WEST, CompassUiState(frame = frameWithHeading(270.0)).direction)
        assertEquals(CardinalDirection.NORTHWEST, CompassUiState(frame = frameWithHeading(315.0)).direction)
    }

    private fun frameWithHeading(heading: Double) =
        com.nanami.koishi.feature.tools.compass.engine.CompassFrame(headingDeg = heading)
}
