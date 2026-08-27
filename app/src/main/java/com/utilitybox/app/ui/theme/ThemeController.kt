package com.utilitybox.app.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit

private const val PREFS = "utilitybox_settings"
private const val KEY_THEME_MODE = "theme_mode"

/** How the app picks between the light and dark colour schemes. */
enum class ThemeMode(val label: String, val description: String) {
    SYSTEM("Follow system", "Match the device's light or dark setting"),
    LIGHT("Light", "Always use the light theme"),
    DARK("Dark", "Always use the dark theme"),
    ;

    /** Resolves this preference against the device's current setting. */
    fun resolveDark(systemInDarkTheme: Boolean): Boolean = when (this) {
        SYSTEM -> systemInDarkTheme
        LIGHT -> false
        DARK -> true
    }

    companion object {
        /** Unknown or missing stored values fall back to following the system. */
        fun fromStoredValue(value: String?): ThemeMode =
            entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}

/**
 * Holds the chosen [ThemeMode] as Compose state and mirrors it into shared
 * preferences, so the choice survives a restart and applies immediately.
 */
@Stable
class ThemeController internal constructor(private val context: Context) {

    private var current: ThemeMode by mutableStateOf(
        ThemeMode.fromStoredValue(prefs(context).getString(KEY_THEME_MODE, null))
    )

    /** Reading this in a composable subscribes it to theme changes. */
    var mode: ThemeMode
        get() = current
        set(value) {
            if (value == current) return
            current = value
            prefs(context).edit { putString(KEY_THEME_MODE, value.name) }
        }

    private companion object {
        fun prefs(context: Context) =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }
}

val LocalThemeController = staticCompositionLocalOf<ThemeController> {
    error("No ThemeController provided. Wrap the content in a CompositionLocalProvider.")
}

@Composable
fun rememberThemeController(): ThemeController {
    val context = LocalContext.current.applicationContext
    return remember(context) { ThemeController(context) }
}
