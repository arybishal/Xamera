package com.bishal.adaptivecamera.camera.lens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectLensSelectorTest {

    @Test
    fun testPhysicalLensModelProperties() {
        val mainLens = DirectPhysicalLens(
            cameraId = "2",
            type = PhysicalLensType.WIDE_MAIN,
            label = "Main 5.6mm",
            focalLengthMm = 5.59f,
            sensorSize = "Physical Sub-Sensor",
            isPhysicalSubCamera = true
        )

        val ultraWideLens = DirectPhysicalLens(
            cameraId = "3",
            type = PhysicalLensType.ULTRA_WIDE,
            label = "UW 1.7mm",
            focalLengthMm = 1.68f,
            sensorSize = "Physical Sub-Sensor",
            isPhysicalSubCamera = true
        )

        assertEquals("2", mainLens.cameraId)
        assertEquals(PhysicalLensType.WIDE_MAIN, mainLens.type)
        assertTrue(mainLens.isPhysicalSubCamera)

        assertEquals("3", ultraWideLens.cameraId)
        assertEquals(PhysicalLensType.ULTRA_WIDE, ultraWideLens.type)
        assertEquals(1.68f, ultraWideLens.focalLengthMm, 0.01f)
    }
}
