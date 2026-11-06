package com.bishal.adaptivecamera.photography.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AeAfLockStateTransitionTest {

    @Test
    fun testInitialLockStatesAreDisabled() {
        val config = ProConfiguration()
        assertFalse("AE lock should be disabled by default", config.isAeLocked)
        assertFalse("AF lock should be disabled by default", config.isAfLocked)
        assertFalse("AWB lock should be disabled by default", config.isAwbLocked)
    }

    @Test
    fun testIndependentAeLockToggle() {
        var config = ProConfiguration()
        config = config.copy(isAeLocked = true)

        assertTrue(config.isAeLocked)
        assertFalse(config.isAfLocked)
        assertFalse(config.isAwbLocked)

        config = config.copy(isAeLocked = false)
        assertFalse(config.isAeLocked)
    }

    @Test
    fun testIndependentAfLockToggle() {
        var config = ProConfiguration()
        config = config.copy(isAfLocked = true)

        assertFalse(config.isAeLocked)
        assertTrue(config.isAfLocked)
        assertFalse(config.isAwbLocked)

        config = config.copy(isAfLocked = false)
        assertFalse(config.isAfLocked)
    }

    @Test
    fun testUnifiedAeAfLock() {
        val config = ProConfiguration(
            isAeLocked = true,
            isAfLocked = true
        )

        assertTrue("Both AE and AF should be locked", config.isAeLocked && config.isAfLocked)
        assertFalse("AWB should remain unlocked unless explicitly set", config.isAwbLocked)
    }

    @Test
    fun testAwbLockState() {
        var config = ProConfiguration(isAwbLocked = true)
        assertTrue(config.isAwbLocked)

        val appliedState = ProAppliedState(
            isAwbLocked = true,
            appliedKelvin = 5500
        )
        assertTrue(appliedState.isAwbLocked)
        assertEquals("AWB Lock", appliedState.appliedWbDisplay)
    }

    @Test
    fun testPhase6MonitoringConfigurationFlags() {
        val config = ProConfiguration(
            isZebraEnabled = true,
            zebraThresholdIre = 85,
            isFalseColorEnabled = true,
            isFocusMagnifierEnabled = true,
            focusMagnifierScale = 4.0f,
            isManualKelvin = true,
            requestedKelvin = 4200,
            requestedTint = -10
        )

        assertTrue(config.isZebraEnabled)
        assertEquals(85, config.zebraThresholdIre)
        assertTrue(config.isFalseColorEnabled)
        assertTrue(config.isFocusMagnifierEnabled)
        assertEquals(4.0f, config.focusMagnifierScale, 0.001f)
        assertTrue(config.isManualKelvin)
        assertEquals(4200, config.requestedKelvin)
        assertEquals(-10, config.requestedTint)
    }
}
