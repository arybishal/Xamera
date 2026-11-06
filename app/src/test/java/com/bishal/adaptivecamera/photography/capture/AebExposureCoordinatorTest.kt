package com.bishal.adaptivecamera.photography.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AebExposureCoordinatorTest {

    @Test
    fun testEvOffsetsCalculationFor3Brackets() {
        val offsets1Ev = AebExposureCoordinator.calculateEvOffsets(bracketCount = 3, evStep = 1.0f)
        assertEquals(3, offsets1Ev.size)
        assertEquals(-2.0f, offsets1Ev[0], 0.001f)
        assertEquals(0.0f, offsets1Ev[1], 0.001f)
        assertEquals(2.0f, offsets1Ev[2], 0.001f)

        val offsetsHalfEv = AebExposureCoordinator.calculateEvOffsets(bracketCount = 3, evStep = 0.5f)
        assertEquals(-1.0f, offsetsHalfEv[0], 0.001f)
        assertEquals(0.0f, offsetsHalfEv[1], 0.001f)
        assertEquals(1.0f, offsetsHalfEv[2], 0.001f)
    }

    @Test
    fun testEvOffsetsCalculationFor5And7Brackets() {
        val offsets5 = AebExposureCoordinator.calculateEvOffsets(bracketCount = 5, evStep = 1.0f)
        assertEquals(listOf(-2.0f, -1.0f, 0.0f, 1.0f, 2.0f), offsets5)

        val offsets7 = AebExposureCoordinator.calculateEvOffsets(bracketCount = 7, evStep = 1.0f)
        assertEquals(listOf(-3.0f, -2.0f, -1.0f, 0.0f, 1.0f, 2.0f, 3.0f), offsets7)
    }

    @Test
    fun testAutoBracketSequenceCalculatesIndicesCorrectly() {
        val offsets = listOf(-2.0f, 0.0f, 2.0f)
        // stepEv = 0.5f -> indexDelta is ev / 0.5
        // -2.0 / 0.5 = -4, 0.0 / 0.5 = 0, 2.0 / 0.5 = +4
        val baseIndex = 0
        val sequence = AebExposureCoordinator.buildAutoBracketSequence(
            offsets = offsets,
            baseIndex = baseIndex,
            stepEv = 0.5f,
            minIndex = -6,
            maxIndex = 6
        )

        assertEquals(3, sequence.size)
        assertEquals(-4, sequence[0].aeCompensationIndex)
        assertEquals(0, sequence[1].aeCompensationIndex)
        assertEquals(4, sequence[2].aeCompensationIndex)

        // Verify clamping
        val clampedSequence = AebExposureCoordinator.buildAutoBracketSequence(
            offsets = offsets,
            baseIndex = 4,
            stepEv = 0.5f,
            minIndex = -4,
            maxIndex = 4
        )
        assertEquals(4, clampedSequence[2].aeCompensationIndex) // Clamped to maxIndex 4
    }

    @Test
    fun testManualBracketSequenceModulatesShutterAndCompensatesIso() {
        val offsets = listOf(-1.0f, 0.0f, 1.0f)
        val baseShutterNs = 16_666_666L // 1/60s
        val baseIso = 100

        val sequence = AebExposureCoordinator.buildManualBracketSequence(
            offsets = offsets,
            baseShutterNs = baseShutterNs,
            minShutterNs = 100_000L,
            maxShutterNs = 30_000_000_000L,
            baseIso = baseIso,
            minIso = 50,
            maxIso = 6400
        )

        assertEquals(3, sequence.size)

        // -1.0 EV -> shutter speed should be base * 2^(-1) = base / 2
        assertEquals(baseShutterNs / 2, sequence[0].manualShutterNs)
        assertEquals(100, sequence[0].manualIso)

        // 0.0 EV -> base shutter
        assertEquals(baseShutterNs, sequence[1].manualShutterNs)
        assertEquals(100, sequence[1].manualIso)

        // +1.0 EV -> shutter speed should be base * 2^1 = base * 2
        assertEquals(baseShutterNs * 2, sequence[2].manualShutterNs)
        assertEquals(100, sequence[2].manualIso)
    }

    @Test
    fun testManualBracketSequenceCompensatesWithIsoWhenShutterClamped() {
        val offsets = listOf(2.0f) // +2.0 EV factor is 4.0
        val baseShutterNs = 20_000_000_000L // 20s
        val maxShutterNs = 30_000_000_000L // Max 30s (shutter can only expand 1.5x)
        val baseIso = 100

        val sequence = AebExposureCoordinator.buildManualBracketSequence(
            offsets = offsets,
            baseShutterNs = baseShutterNs,
            minShutterNs = 100_000L,
            maxShutterNs = maxShutterNs,
            baseIso = baseIso,
            minIso = 50,
            maxIso = 3200
        )

        val step = sequence[0]
        assertEquals(maxShutterNs, step.manualShutterNs) // Clamped at 30s
        // Remainder factor is 4.0 / 1.5 = 2.666 -> ISO becomes 100 * 2.666 = 266
        assertTrue("ISO must be scaled up to compensate clamped shutter", step.manualIso!! > baseIso)
    }

    @Test
    fun testFormatAebFilename() {
        val name = AebExposureCoordinator.formatAebFilename(
            sessionId = "20261106_173000",
            frameIndex = 1,
            totalFrames = 3,
            evOffset = -2.0f
        )
        assertEquals("IMG_AEB_20261106_173000_1of3_-2.0EV.jpg", name)

        val positiveName = AebExposureCoordinator.formatAebFilename(
            sessionId = "20261106_173000",
            frameIndex = 3,
            totalFrames = 3,
            evOffset = 2.0f
        )
        assertEquals("IMG_AEB_20261106_173000_3of3_+2.0EV.jpg", positiveName)
    }

    @Test
    fun testAebBracketModeTransitions() {
        var mode = AebBracketMode.OFF
        assertEquals(0, mode.count)

        mode = mode.next()
        assertEquals(AebBracketMode.BRACKET_3, mode)
        assertEquals(3, mode.count)

        mode = mode.next()
        assertEquals(AebBracketMode.BRACKET_5, mode)
        assertEquals(5, mode.count)

        mode = mode.next()
        assertEquals(AebBracketMode.BRACKET_7, mode)
        assertEquals(7, mode.count)

        mode = mode.next()
        assertEquals(AebBracketMode.OFF, mode)
    }
}
