package com.bishal.adaptivecamera.camera.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CompositionGridTypeTest {

    @Test
    fun testCycleOrder() {
        assertEquals(CompositionGridType.RULE_OF_THIRDS, CompositionGridType.OFF.next())
        assertEquals(CompositionGridType.GOLDEN_RATIO, CompositionGridType.RULE_OF_THIRDS.next())
        assertEquals(CompositionGridType.GOLDEN_SPIRAL, CompositionGridType.GOLDEN_RATIO.next())
        assertEquals(CompositionGridType.CENTER_CROSSHAIRS, CompositionGridType.GOLDEN_SPIRAL.next())
        assertEquals(CompositionGridType.OFF, CompositionGridType.CENTER_CROSSHAIRS.next())
    }

    @Test
    fun testDisplayNamesAndDescriptions() {
        for (grid in CompositionGridType.values()) {
            assertNotNull(grid.displayName)
            assertNotNull(grid.description)
            assert(grid.displayName.isNotBlank())
            assert(grid.description.isNotBlank())
        }
    }
}
