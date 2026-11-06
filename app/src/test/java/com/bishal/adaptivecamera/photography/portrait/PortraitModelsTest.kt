package com.bishal.adaptivecamera.photography.portrait

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PortraitModelsTest {

    @Test
    fun testPortraitApertureValues() {
        val apertures = PortraitAperture.values()
        assertEquals(5, apertures.size)

        assertEquals("f/1.4", PortraitAperture.F1_4.label)
        assertEquals(1.4f, PortraitAperture.F1_4.fNumber, 0.001f)
        assertEquals(26, PortraitAperture.F1_4.blurRadius)

        assertEquals("f/2.0", PortraitAperture.F2_0.label)
        assertEquals(2.0f, PortraitAperture.F2_0.fNumber, 0.001f)
        assertEquals(20, PortraitAperture.F2_0.blurRadius)

        assertEquals("f/2.8", PortraitAperture.F2_8.label)
        assertEquals(2.8f, PortraitAperture.F2_8.fNumber, 0.001f)
        assertEquals(14, PortraitAperture.F2_8.blurRadius)

        assertEquals("f/4.0", PortraitAperture.F4_0.label)
        assertEquals(4.0f, PortraitAperture.F4_0.fNumber, 0.001f)
        assertEquals(8, PortraitAperture.F4_0.blurRadius)

        assertEquals("f/5.6", PortraitAperture.F5_6.label)
        assertEquals(5.6f, PortraitAperture.F5_6.fNumber, 0.001f)
        assertEquals(4, PortraitAperture.F5_6.blurRadius)
    }

    @Test
    fun testPortraitApertureCycling() {
        assertEquals(PortraitAperture.F2_0, PortraitAperture.F1_4.next())
        assertEquals(PortraitAperture.F2_8, PortraitAperture.F2_0.next())
        assertEquals(PortraitAperture.F4_0, PortraitAperture.F2_8.next())
        assertEquals(PortraitAperture.F5_6, PortraitAperture.F4_0.next())
        assertEquals(PortraitAperture.F1_4, PortraitAperture.F5_6.next())
    }

    @Test
    fun testPortraitLightingStyles() {
        val styles = PortraitLightingStyle.values()
        assertEquals(4, styles.size)

        assertEquals("Natural", PortraitLightingStyle.NATURAL.label)
        assertEquals("Studio", PortraitLightingStyle.STUDIO.label)
        assertEquals("Soft", PortraitLightingStyle.SOFT.label)
        assertEquals("Dramatic", PortraitLightingStyle.DRAMATIC.label)

        assertEquals(PortraitLightingStyle.STUDIO, PortraitLightingStyle.NATURAL.next())
        assertEquals(PortraitLightingStyle.SOFT, PortraitLightingStyle.STUDIO.next())
        assertEquals(PortraitLightingStyle.DRAMATIC, PortraitLightingStyle.SOFT.next())
        assertEquals(PortraitLightingStyle.NATURAL, PortraitLightingStyle.DRAMATIC.next())
    }

    @Test
    fun testPortraitConfigDefaults() {
        val config = PortraitConfig()
        assertEquals(PortraitAperture.F2_8, config.aperture)
        assertEquals(PortraitLightingStyle.NATURAL, config.lightingStyle)
        assertEquals(0.5f, config.depthFalloff, 0.001f)
        assertTrue(config.boostHighlights)
    }

    @Test
    fun testSubjectSegmentationResultEquality() {
        val res1 = SubjectSegmentationResult(
            width = 10,
            height = 10,
            mask = FloatArray(100) { 1.0f },
            confidence = 0.9f,
            hasFaceDetected = true
        )
        val res2 = SubjectSegmentationResult(
            width = 10,
            height = 10,
            mask = FloatArray(100) { 1.0f },
            confidence = 0.9f,
            hasFaceDetected = true
        )
        val res3 = SubjectSegmentationResult(
            width = 10,
            height = 10,
            mask = FloatArray(100) { 0.0f },
            confidence = 0.0f,
            hasFaceDetected = false
        )

        assertEquals(res1, res2)
        assertEquals(res1.hashCode(), res2.hashCode())
        assertNotEquals(res1, res3)
    }
}
