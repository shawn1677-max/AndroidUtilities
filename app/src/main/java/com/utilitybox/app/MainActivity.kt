package com.utilitybox.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.utilitybox.app.nav.UtilityBoxNavHost
import com.utilitybox.app.ui.theme.LocalThemeController
import com.utilitybox.app.ui.theme.UtilityBoxTheme
import com.utilitybox.app.ui.theme.rememberThemeController

/** Scrims Android applies behind the navigation bar on releases without gesture navigation. */
private val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
private val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)

class MainActivity : ComponentActivity() {

    /** Tool id a widget asked us to open, cleared once it has been navigated to. */
    private var pendingTool by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        pendingTool = intent?.getStringExtra(EXTRA_OPEN_TOOL)
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
                    UtilityBoxNavHost(
                        openTool = pendingTool,
                        onToolOpened = { pendingTool = null },
                    )
                }
            }
        }
    }

    // launchMode is singleTop, so a widget tap on an already-open app arrives here.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingTool = intent.getStringExtra(EXTRA_OPEN_TOOL)
    }

    companion object {
        const val EXTRA_OPEN_TOOL = "com.utilitybox.app.OPEN_TOOL"
    }
}
