package com.focusfirst.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeModePolicyTest {
    @Test fun absentAndLegacyValuesDefaultToDark() {
        assertEquals(ThemeModePolicy.DARK, ThemeModePolicy.normalize(null))
        assertEquals(ThemeModePolicy.DARK, ThemeModePolicy.normalize("System"))
        assertEquals(ThemeModePolicy.DARK, ThemeModePolicy.normalize("unexpected"))
        assertTrue(ThemeModePolicy.isDark("System"))
    }

    @Test fun explicitLightAndDarkArePreserved() {
        assertEquals(ThemeModePolicy.LIGHT, ThemeModePolicy.normalize("Light"))
        assertEquals(ThemeModePolicy.DARK, ThemeModePolicy.normalize("Dark"))
        assertFalse(ThemeModePolicy.isDark("Light"))
        assertTrue(ThemeModePolicy.isDark("Dark"))
    }
}
