package com.bishal.adaptivecamera.core.diagnostics

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceInfoTest {

    @Test
    fun testDetectsOnePlusAce5ChinaVariant() {
        // Typical OnePlus Ace 5 (China) identifiers
        assertTrue(DeviceInfo.isLikelyOnePlusAce5("PKG110", "PKG110", "PKG110"))
        assertTrue(DeviceInfo.isLikelyOnePlusAce5("OnePlus Ace 5", "OP5D1BL1", "Ace5"))
        assertTrue(DeviceInfo.isLikelyOnePlusAce5("PJR110", "ace5", "OnePlus"))
    }

    @Test
    fun testDistinguishesFromGlobalOnePlus13R() {
        // Global OnePlus 13R identifiers (CPH2645, CPH2647)
        assertFalse(DeviceInfo.isLikelyOnePlusAce5("CPH2645", "CPH2645", "OnePlus13R"))
        assertFalse(DeviceInfo.isLikelyOnePlusAce5("OnePlus 13R", "OP595DL1", "OnePlus13R"))
    }

    @Test
    fun testGenericDevicesAreNotAce5() {
        assertFalse(DeviceInfo.isLikelyOnePlusAce5("Pixel 8 Pro", "husky", "husky"))
        assertFalse(DeviceInfo.isLikelyOnePlusAce5("Galaxy S24", "e3q", "e3q"))
    }
}
