package com.bishal.adaptivecamera.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AppThemeModeTest {

    @Test
    fun testAppThemeModeEnumValues() {
        val modes = AppThemeMode.values()
        assertEquals(3, modes.size)
        assertEquals(AppThemeMode.SYSTEM, AppThemeMode.valueOf("SYSTEM"))
        assertEquals(AppThemeMode.DARK, AppThemeMode.valueOf("DARK"))
        assertEquals(AppThemeMode.LIGHT, AppThemeMode.valueOf("LIGHT"))
    }

    @Test
    fun testAppThemeModeDisplayNames() {
        assertEquals("System Default", AppThemeMode.SYSTEM.displayName)
        assertEquals("Dark", AppThemeMode.DARK.displayName)
        assertEquals("Light", AppThemeMode.LIGHT.displayName)
    }

    @Test
    fun testLightModeColorTokensNonNull() {
        assertNotNull(LightYellowPrimary)
        assertNotNull(LightYellowDeep)
        assertNotNull(LightYellowContainer)
        assertNotNull(LightYellowBorder)
        assertNotNull(LightPureBlack)
        assertNotNull(LightPureWhite)
        assertNotNull(LightWarmWhite)
        assertNotNull(LightSurfaceVariant)
        assertNotNull(LightTextPrimary)
        assertNotNull(LightTextSecondary)
        assertNotNull(LightTextMuted)
        assertNotNull(LightDivider)
        assertNotNull(LightBorder)
    }
}
