package com.utilitybox.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Color(0xFF1E6F5C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFA7F2DA),
    onPrimaryContainer = Color(0xFF00201A),
    secondary = Color(0xFF4B635B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCDE9DD),
    onSecondaryContainer = Color(0xFF072019),
    tertiary = Color(0xFF3F6375),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC3E8FD),
    onTertiaryContainer = Color(0xFF001E2B),
    background = Color(0xFFFBFDFA),
    onBackground = Color(0xFF191C1B),
    surface = Color(0xFFFBFDFA),
    onSurface = Color(0xFF191C1B),
    surfaceVariant = Color(0xFFDBE5DF),
    onSurfaceVariant = Color(0xFF3F4945),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    outline = Color(0xFF6F7975),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8BD5BF),
    onPrimary = Color(0xFF00382D),
    primaryContainer = Color(0xFF005143),
    onPrimaryContainer = Color(0xFFA7F2DA),
    secondary = Color(0xFFB1CCC1),
    onSecondary = Color(0xFF1D352D),
    secondaryContainer = Color(0xFF344C43),
    onSecondaryContainer = Color(0xFFCDE9DD),
    tertiary = Color(0xFFA7CCE1),
    onTertiary = Color(0xFF0B3446),
    tertiaryContainer = Color(0xFF264B5D),
    onTertiaryContainer = Color(0xFFC3E8FD),
    background = Color(0xFF101418),
    onBackground = Color(0xFFE1E3E1),
    surface = Color(0xFF101418),
    onSurface = Color(0xFFE1E3E1),
    surfaceVariant = Color(0xFF3F4945),
    onSurfaceVariant = Color(0xFFBFC9C4),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    outline = Color(0xFF89938E),
)

@Composable
fun UtilityBoxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content,
    )
}
