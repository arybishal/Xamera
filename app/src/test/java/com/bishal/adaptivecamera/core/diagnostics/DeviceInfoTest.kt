package com.bishal.adaptivecamera.core.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceInfoTest {

    @Test
    fun testCPH2691ReportsUnconfirmedPhysicalIdentityDueToConvertedFirmware() {
        val info = DeviceInfo.analyze(
            manufacturer = "OnePlus",
            model = "CPH2691",
            device = "OP5D3BL1",
            product = "CPH2691IN",
            brand = "OnePlus",
            hardware = "qcom",
            fingerprint = "OnePlus/CPH2691IN/OP5D3BL1:17/UKQ1.231108.001/V.R4T3:user/release-keys",
            display = "CPH2691_17.0.0.105(EX01)",
            androidVersion = "17",
            apiLevel = 37
        )

        // Firmware is Indian 13R, but hardware board OP5D3BL1 is shared with Ace 5
        assertEquals("OnePlus 13R (India / Regional CPH2691IN)", info.firmwareIdentity)
        assertTrue(info.probableHardwareFamily.contains("OP5D3BL1"))
        assertEquals(PhysicalIdentityConfidence.UNCONFIRMED, info.physicalIdentityConfidence)
        assertTrue(info.physicalHardwareIdentity.contains("Unconfirmed"))
        assertTrue(info.identityEvidence.any { it.factor.contains("Cross-Flash") || it.factor.contains("Converted") })
    }

    @Test
    fun testPKG110ReportsChinaAce5ProbableIdentity() {
        val info = DeviceInfo.analyze(
            manufacturer = "OnePlus",
            model = "PKG110",
            device = "PKG110",
            product = "PKG110",
            brand = "OnePlus",
            hardware = "qcom",
            fingerprint = "OnePlus/PKG110/PKG110:15/...",
            display = "PKG110_15.0.0.100",
            androidVersion = "15",
            apiLevel = 35
        )

        assertTrue(info.firmwareIdentity.contains("OnePlus Ace 5"))
        assertEquals("OnePlus Ace 5 (China)", info.physicalHardwareIdentity)
        assertEquals(PhysicalIdentityConfidence.PROBABLE, info.physicalIdentityConfidence)
    }

    @Test
    fun testCPH2645ReportsGlobal13RProbableIdentity() {
        val info = DeviceInfo.analyze(
            manufacturer = "OnePlus",
            model = "CPH2645",
            device = "OP595DL1",
            product = "CPH2645",
            brand = "OnePlus",
            hardware = "qcom",
            fingerprint = "OnePlus/CPH2645/OP595DL1:15/...",
            display = "CPH2645_15.0.0.100",
            androidVersion = "15",
            apiLevel = 35
        )

        assertTrue(info.firmwareIdentity.contains("OnePlus 13R (Global"))
        assertEquals("OnePlus 13R (Global)", info.physicalHardwareIdentity)
        assertEquals(PhysicalIdentityConfidence.PROBABLE, info.physicalIdentityConfidence)
    }

    @Test
    fun testGenericDeviceHasUnconfirmedIdentity() {
        val info = DeviceInfo.analyze(
            manufacturer = "Google",
            model = "Pixel 8 Pro",
            device = "husky",
            product = "husky",
            brand = "google",
            hardware = "zuma",
            fingerprint = "google/husky/husky:15/...",
            display = "AP2A.240805.005",
            androidVersion = "15",
            apiLevel = 35
        )

        assertEquals("google Pixel 8 Pro", info.firmwareIdentity)
        assertEquals(PhysicalIdentityConfidence.UNCONFIRMED, info.physicalIdentityConfidence)
        assertFalse(info.probableHardwareFamily.contains("Ace 5"))
    }
}
