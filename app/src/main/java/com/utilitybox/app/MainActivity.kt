package com.utilitybox.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import com.utilitybox.app.nav.UtilityBoxNavHost
import com.utilitybox.app.ui.theme.LocalThemeController
import com.utilitybox.app.ui.theme.UtilityBoxTheme
import com.utilitybox.app.ui.theme.rememberThemeController

/** Scrims Android applies behind the navigation bar on releases without gesture navigation. */
private val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
private val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val themeController = rememberThemeController()
            val darkTheme = themeController.mode.resolveDark(isSystemInDarkTheme())

            // Re-apply the system bar styling whenever the resolved theme changes, so
            // status and navigation bar icons stay legible when the user overrides the
            // system setting.
            LaunchedEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        Color.TRANSPARENT,
                        Color.TRANSPARENT,
                    ) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(
                        LIGHT_SCRIM,
                        DARK_SCRIM,
                    ) { darkTheme },
                )
            }

            CompositionLocalProvider(LocalThemeController provides themeController) {
                UtilityBoxTheme(darkTheme = darkTheme) {
                    UtilityBoxNavHost()
                }
            }
        }
    }
}
