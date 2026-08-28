package com.utilitybox.app.nav

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.utilitybox.app.tools.ToolIds
import com.utilitybox.app.tools.ToolRegistry
import com.utilitybox.app.tools.calculate.BmiScreen
import com.utilitybox.app.tools.calculate.CalculatorScreen
import com.utilitybox.app.tools.calculate.DateCalculatorScreen
import com.utilitybox.app.tools.calculate.PasswordScreen
import com.utilitybox.app.tools.calculate.PercentageScreen
import com.utilitybox.app.tools.calculate.RandomScreen
import com.utilitybox.app.tools.calculate.TallyScreen
import com.utilitybox.app.tools.calculate.TipScreen
import com.utilitybox.app.tools.convert.Base64Screen
import com.utilitybox.app.tools.convert.BaseConverterScreen
import com.utilitybox.app.tools.convert.ColorToolScreen
import com.utilitybox.app.tools.convert.HashScreen
import com.utilitybox.app.tools.convert.MorseScreen
import com.utilitybox.app.tools.convert.QrGeneratorScreen
import com.utilitybox.app.tools.convert.QrScannerScreen
import com.utilitybox.app.tools.convert.TextReaderScreen
import com.utilitybox.app.tools.convert.TextToolsScreen
import com.utilitybox.app.tools.convert.UnitConverterScreen
import com.utilitybox.app.tools.convert.WorldClockScreen
import com.utilitybox.app.tools.device.AppInventoryScreen
import com.utilitybox.app.tools.device.BatteryScreen
import com.utilitybox.app.tools.device.DeviceInfoScreen
import com.utilitybox.app.tools.device.NetworkScreen
import com.utilitybox.app.tools.device.SensorScreen
import com.utilitybox.app.tools.device.StorageScreen
import com.utilitybox.app.tools.hardware.ScreenTestScreen
import com.utilitybox.app.tools.hardware.ToneGeneratorScreen
import com.utilitybox.app.tools.hardware.TouchTestScreen
import com.utilitybox.app.tools.hardware.VibrationTestScreen
import com.utilitybox.app.tools.measure.BarometerScreen
import com.utilitybox.app.tools.measure.CompassScreen
import com.utilitybox.app.tools.measure.FlashlightScreen
import com.utilitybox.app.tools.measure.LevelScreen
import com.utilitybox.app.tools.measure.LightMeterScreen
import com.utilitybox.app.tools.measure.MagnifierScreen
import com.utilitybox.app.tools.measure.MetronomeScreen
import com.utilitybox.app.tools.measure.RulerScreen
import com.utilitybox.app.tools.measure.SoundMeterScreen
import com.utilitybox.app.tools.measure.StopwatchScreen
import com.utilitybox.app.tools.measure.TimerScreen
import com.utilitybox.app.ui.settings.SettingsScreen

private const val HOME = "home"
private const val SETTINGS = "settings"
private const val TRANSITION_MS = 220

@Composable
fun UtilityBoxNavHost(
    openTool: String? = null,
    onToolOpened: () -> Unit = {},
) {
    val navController = rememberNavController()

    // A widget can ask for a specific tool. The id is validated against the
    // registry so an unknown or malformed extra simply does nothing.
    LaunchedEffect(openTool) {
        val tool = openTool?.let { ToolRegistry.find(it) }
        if (tool != null) {
            navController.navigateToTool(tool.id)
            onToolOpened()
        }
    }

    NavHost(
        navController = navController,
        startDestination = HOME,
        enterTransition = {
            slideInHorizontally(tween(TRANSITION_MS)) { it / 6 } + fadeIn(tween(TRANSITION_MS))
        },
        exitTransition = { fadeOut(tween(TRANSITION_MS / 2)) },
        popEnterTransition = { fadeIn(tween(TRANSITION_MS)) },
        popExitTransition = {
            slideOutHorizontally(tween(TRANSITION_MS)) { it / 6 } + fadeOut(tween(TRANSITION_MS))
        },
    ) {
        composable(HOME) {
            HomeScreen(
                onOpenTool = { toolId -> navController.navigateToTool(toolId) },
                onOpenSettings = { navController.navigateToTool(SETTINGS) },
            )
        }

        val back: () -> Unit = { navController.popBackStack() }

        composable(SETTINGS) { SettingsScreen(back) }

        composable(ToolIds.DEVICE_INFO) { DeviceInfoScreen(back) }
        composable(ToolIds.BATTERY) { BatteryScreen(back) }
        composable(ToolIds.STORAGE) { StorageScreen(back) }
        composable(ToolIds.NETWORK) { NetworkScreen(back) }
        composable(ToolIds.SENSORS) { SensorScreen(back) }
        composable(ToolIds.APPS) { AppInventoryScreen(back) }

        composable(ToolIds.COMPASS) { CompassScreen(back) }
        composable(ToolIds.LEVEL) { LevelScreen(back) }
        composable(ToolIds.RULER) { RulerScreen(back) }
        composable(ToolIds.SOUND_METER) { SoundMeterScreen(back) }
        composable(ToolIds.BAROMETER) { BarometerScreen(back) }
        composable(ToolIds.LIGHT_METER) { LightMeterScreen(back) }
        composable(ToolIds.MAGNIFIER) { MagnifierScreen(back) }
        composable(ToolIds.FLASHLIGHT) { FlashlightScreen(back) }
        composable(ToolIds.STOPWATCH) { StopwatchScreen(back) }
        composable(ToolIds.TIMER) { TimerScreen(back) }
        composable(ToolIds.METRONOME) { MetronomeScreen(back) }

        composable(ToolIds.SCREEN_TEST) { ScreenTestScreen(back) }
        composable(ToolIds.TOUCH_TEST) { TouchTestScreen(back) }
        composable(ToolIds.VIBRATION_TEST) { VibrationTestScreen(back) }
        composable(ToolIds.TONE_GENERATOR) { ToneGeneratorScreen(back) }

        composable(ToolIds.UNIT_CONVERTER) { UnitConverterScreen(back) }
        composable(ToolIds.BASE_CONVERTER) { BaseConverterScreen(back) }
        composable(ToolIds.COLOR_CONVERTER) { ColorToolScreen(back) }
        composable(ToolIds.TEXT_TOOLS) { TextToolsScreen(back) }
        composable(ToolIds.HASH) { HashScreen(back) }
        composable(ToolIds.BASE64) { Base64Screen(back) }
        composable(ToolIds.MORSE) { MorseScreen(back) }
        composable(ToolIds.QR_GENERATE) { QrGeneratorScreen(back) }
        composable(ToolIds.QR_SCAN) { QrScannerScreen(back) }
        composable(ToolIds.WORLD_CLOCK) { WorldClockScreen(back) }
        composable(ToolIds.TEXT_READER) { TextReaderScreen(back) }

        composable(ToolIds.TIP) { TipScreen(back) }
        composable(ToolIds.PERCENTAGE) { PercentageScreen(back) }
        composable(ToolIds.DATE_CALC) { DateCalculatorScreen(back) }
        composable(ToolIds.BMI) { BmiScreen(back) }
        composable(ToolIds.PASSWORD) { PasswordScreen(back) }
        composable(ToolIds.RANDOM) { RandomScreen(back) }
        composable(ToolIds.CALCULATOR) { CalculatorScreen(back) }
        composable(ToolIds.TALLY) { TallyScreen(back) }
    }
}

/** Guards against double taps queuing the same destination twice. */
private fun NavHostController.navigateToTool(toolId: String) {
    if (currentDestination?.route == toolId) return
    navigate(toolId) { launchSingleTop = true }
}
