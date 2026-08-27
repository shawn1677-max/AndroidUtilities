package com.utilitybox.app

import com.utilitybox.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeModeTest {

    @Test
    fun `system mode follows the device setting`() {
        assertTrue(ThemeMode.SYSTEM.resolveDark(systemInDarkTheme = true))
        assertFalse(ThemeMode.SYSTEM.resolveDark(systemInDarkTheme = false))
    }

    @Test
    fun `light mode stays light even when the device is dark`() {
        assertFalse(ThemeMode.LIGHT.resolveDark(systemInDarkTheme = true))
        assertFalse(ThemeMode.LIGHT.resolveDark(systemInDarkTheme = false))
    }

    @Test
    fun `dark mode stays dark even when the device is light`() {
        assertTrue(ThemeMode.DARK.resolveDark(systemInDarkTheme = true))
        assertTrue(ThemeMode.DARK.resolveDark(systemInDarkTheme = false))
    }

    @Test
    fun `stored values round trip`() {
        ThemeMode.entries.forEach { mode ->
            assertEquals(mode, ThemeMode.fromStoredValue(mode.name))
        }
    }

    @Test
    fun `missing or unrecognised stored values fall back to following the system`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStoredValue(null))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStoredValue(""))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStoredValue("AMOLED"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStoredValue("dark"))
    }

    @Test
    fun `every mode has a distinct label and a description`() {
        val labels = ThemeMode.entries.map { it.label }
        assertEquals(labels.size, labels.distinct().size)
        ThemeMode.entries.forEach { mode ->
            assertTrue(mode.label.isNotBlank())
            assertTrue(mode.description.isNotBlank())
        }
    }
}
