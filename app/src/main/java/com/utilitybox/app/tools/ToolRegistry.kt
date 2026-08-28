package com.utilitybox.app.tools

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Exposure
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.Gradient
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.PlusOne
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.ui.graphics.vector.ImageVector

enum class ToolCategory(val label: String) {
    DEVICE("Device"),
    MEASURE("Measure"),
    HARDWARE_TEST("Hardware tests"),
    CONVERT("Convert & encode"),
    CALCULATE("Calculate"),
}

data class Tool(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val category: ToolCategory,
    /** Extra search terms so "ip" finds Network Info, "torch" finds Flashlight, etc. */
    val keywords: List<String> = emptyList(),
) {
    fun matches(query: String): Boolean {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return true
        return title.lowercase().contains(q) ||
            subtitle.lowercase().contains(q) ||
            category.label.lowercase().contains(q) ||
            keywords.any { it.contains(q) }
    }
}

object ToolIds {
    const val DEVICE_INFO = "device_info"
    const val BATTERY = "battery"
    const val STORAGE = "storage"
    const val NETWORK = "network"
    const val SENSORS = "sensors"
    const val APPS = "apps"

    const val COMPASS = "compass"
    const val LEVEL = "level"
    const val RULER = "ruler"
    const val SOUND_METER = "sound_meter"
    const val BAROMETER = "barometer"
    const val LIGHT_METER = "light_meter"
    const val MAGNIFIER = "magnifier"
    const val FLASHLIGHT = "flashlight"
    const val STOPWATCH = "stopwatch"
    const val TIMER = "timer"
    const val METRONOME = "metronome"

    const val SCREEN_TEST = "screen_test"
    const val TOUCH_TEST = "touch_test"
    const val VIBRATION_TEST = "vibration_test"
    const val TONE_GENERATOR = "tone_generator"

    const val UNIT_CONVERTER = "unit_converter"
    const val BASE_CONVERTER = "base_converter"
    const val COLOR_CONVERTER = "color_converter"
    const val TEXT_TOOLS = "text_tools"
    const val HASH = "hash"
    const val BASE64 = "base64"
    const val MORSE = "morse"
    const val QR_GENERATE = "qr_generate"
    const val QR_SCAN = "qr_scan"
    const val WORLD_CLOCK = "world_clock"
    const val TEXT_READER = "text_reader"

    const val TIP = "tip"
    const val PERCENTAGE = "percentage"
    const val DATE_CALC = "date_calc"
    const val BMI = "bmi"
    const val PASSWORD = "password"
    const val RANDOM = "random"
    const val CALCULATOR = "calculator"
    const val TALLY = "tally"
}

object ToolRegistry {

    val all: List<Tool> = listOf(
        Tool(
            ToolIds.DEVICE_INFO, "Device Info",
            "Model, SoC, RAM, display and build details",
            Icons.Outlined.PhoneAndroid, ToolCategory.DEVICE,
            listOf("cpu", "soc", "ram", "android version", "sdk", "model", "hardware", "abi"),
        ),
        Tool(
            ToolIds.BATTERY, "Battery",
            "Level, health, temperature, voltage and charging state",
            Icons.Outlined.BatteryFull, ToolCategory.DEVICE,
            listOf("charge", "power", "mah", "temperature", "health"),
        ),
        Tool(
            ToolIds.STORAGE, "Storage",
            "Internal and shared storage usage",
            Icons.Outlined.Storage, ToolCategory.DEVICE,
            listOf("disk", "space", "free", "gb", "memory"),
        ),
        Tool(
            ToolIds.NETWORK, "Network Info",
            "Connection type, IP addresses and interface details",
            Icons.Outlined.Wifi, ToolCategory.DEVICE,
            listOf("ip", "wifi", "mobile", "cellular", "mac", "interface", "dns"),
        ),
        Tool(
            ToolIds.SENSORS, "Sensor Explorer",
            "List every sensor and watch live values",
            Icons.Outlined.Sensors, ToolCategory.DEVICE,
            listOf("accelerometer", "gyroscope", "magnetometer", "proximity", "light", "barometer"),
        ),
        Tool(
            ToolIds.APPS, "App Inventory",
            "Installed apps with versions, sizes and install dates",
            Icons.Outlined.Apps, ToolCategory.DEVICE,
            listOf("packages", "installed", "apk", "version", "uninstall"),
        ),

        Tool(
            ToolIds.COMPASS, "Compass",
            "Magnetic heading with cardinal direction",
            Icons.Outlined.Explore, ToolCategory.MEASURE,
            listOf("north", "bearing", "heading", "direction", "magnetic"),
        ),
        Tool(
            ToolIds.LEVEL, "Bubble Level",
            "Check whether a surface is flat or plumb",
            Icons.Outlined.Gradient, ToolCategory.MEASURE,
            listOf("spirit level", "angle", "tilt", "inclinometer", "flat", "plumb"),
        ),
        Tool(
            ToolIds.RULER, "Screen Ruler",
            "On-screen ruler calibrated to your display",
            Icons.Outlined.Straighten, ToolCategory.MEASURE,
            listOf("measure", "cm", "inch", "length", "millimetre"),
        ),
        Tool(
            ToolIds.SOUND_METER, "Sound Meter",
            "Approximate ambient noise level in decibels",
            Icons.Outlined.Mic, ToolCategory.MEASURE,
            listOf("db", "decibel", "noise", "loud", "spl", "microphone"),
        ),
        Tool(
            ToolIds.BAROMETER, "Barometer",
            "Air pressure, altitude estimate and a weather hint",
            Icons.Outlined.Air, ToolCategory.MEASURE,
            listOf("pressure", "hpa", "altitude", "altimeter", "weather", "elevation"),
        ),
        Tool(
            ToolIds.LIGHT_METER, "Light Meter",
            "Illuminance in lux, with an exposure value",
            Icons.Outlined.LightMode, ToolCategory.MEASURE,
            listOf("lux", "brightness", "illuminance", "ev", "photography", "exposure"),
        ),
        Tool(
            ToolIds.MAGNIFIER, "Magnifier",
            "Zoom in on small print with the camera",
            Icons.Outlined.Search, ToolCategory.MEASURE,
            listOf("zoom", "magnify", "reading", "small print", "loupe", "camera"),
        ),
        Tool(
            ToolIds.FLASHLIGHT, "Flashlight",
            "Torch, strobe and SOS beacon",
            Icons.Outlined.FlashlightOn, ToolCategory.MEASURE,
            listOf("torch", "light", "led", "strobe", "sos", "morse"),
        ),
        Tool(
            ToolIds.STOPWATCH, "Stopwatch",
            "Precise timing with laps",
            Icons.Outlined.Timer, ToolCategory.MEASURE,
            listOf("lap", "time", "chronometer", "split"),
        ),
        Tool(
            ToolIds.TIMER, "Countdown Timer",
            "Count down with an audible alert",
            Icons.Outlined.HourglassEmpty, ToolCategory.MEASURE,
            listOf("alarm", "egg timer", "countdown", "minutes"),
        ),
        Tool(
            ToolIds.METRONOME, "Metronome",
            "Steady beat from 30 to 260 BPM",
            Icons.Outlined.MusicNote, ToolCategory.MEASURE,
            listOf("bpm", "tempo", "beat", "music", "practice"),
        ),

        Tool(
            ToolIds.SCREEN_TEST, "Screen Test",
            "Full-screen colours for dead and stuck pixels",
            Icons.Outlined.Gradient, ToolCategory.HARDWARE_TEST,
            listOf("dead pixel", "stuck pixel", "display", "burn in", "lcd", "oled"),
        ),
        Tool(
            ToolIds.TOUCH_TEST, "Touch Test",
            "Check digitiser response and multi-touch points",
            Icons.Outlined.TouchApp, ToolCategory.HARDWARE_TEST,
            listOf("multitouch", "digitizer", "screen", "finger", "dead zone"),
        ),
        Tool(
            ToolIds.VIBRATION_TEST, "Vibration Test",
            "Trigger patterns to verify the haptic motor",
            Icons.Outlined.Vibration, ToolCategory.HARDWARE_TEST,
            listOf("haptic", "motor", "buzz", "rumble"),
        ),
        Tool(
            ToolIds.TONE_GENERATOR, "Tone Generator",
            "Test speakers and earphones across the audible range",
            Icons.AutoMirrored.Outlined.VolumeUp, ToolCategory.HARDWARE_TEST,
            listOf("speaker", "frequency", "hz", "sine", "audio", "hearing", "sweep"),
        ),

        Tool(
            ToolIds.UNIT_CONVERTER, "Unit Converter",
            "Length, mass, temperature, data, speed and more",
            Icons.Outlined.SwapHoriz, ToolCategory.CONVERT,
            listOf("metric", "imperial", "km", "miles", "kg", "celsius", "fahrenheit", "bytes"),
        ),
        Tool(
            ToolIds.BASE_CONVERTER, "Number Base",
            "Binary, octal, decimal, hex and any base 2-36",
            Icons.Outlined.Numbers, ToolCategory.CONVERT,
            listOf("binary", "hex", "hexadecimal", "octal", "radix", "bits"),
        ),
        Tool(
            ToolIds.COLOR_CONVERTER, "Colour Tool",
            "Pick a colour and read HEX, RGB and HSL",
            Icons.Outlined.ColorLens, ToolCategory.CONVERT,
            listOf("color", "hex", "rgb", "hsl", "picker", "palette", "design"),
        ),
        Tool(
            ToolIds.TEXT_TOOLS, "Text Tools",
            "Case conversion, counting, trimming and reversing",
            Icons.Outlined.TextFields, ToolCategory.CONVERT,
            listOf("uppercase", "lowercase", "word count", "slug", "title case", "reverse"),
        ),
        Tool(
            ToolIds.HASH, "Hash Generator",
            "MD5, SHA-1, SHA-256 and SHA-512 checksums",
            Icons.Outlined.Tag, ToolCategory.CONVERT,
            listOf("md5", "sha", "checksum", "digest", "fingerprint"),
        ),
        Tool(
            ToolIds.BASE64, "Base64",
            "Encode and decode Base64 text",
            Icons.Outlined.Code, ToolCategory.CONVERT,
            listOf("encode", "decode", "url safe", "developer"),
        ),
        Tool(
            ToolIds.MORSE, "Morse Code",
            "Translate text to Morse and back",
            Icons.Outlined.GraphicEq, ToolCategory.CONVERT,
            listOf("dot", "dash", "telegraph", "sos", "signal"),
        ),
        Tool(
            ToolIds.QR_GENERATE, "QR Generator",
            "Turn text, links or Wi-Fi details into a QR code",
            Icons.Outlined.QrCode2, ToolCategory.CONVERT,
            listOf("barcode", "share", "link", "url", "wifi"),
        ),
        Tool(
            ToolIds.WORLD_CLOCK, "World Clock",
            "Times around the world, and what they will be later",
            Icons.Outlined.Public, ToolCategory.CONVERT,
            listOf("time zone", "timezone", "utc", "gmt", "city", "abroad", "meeting"),
        ),
        Tool(
            ToolIds.TEXT_READER, "Text Reader",
            "Read text aloud with the on-device voice",
            Icons.Outlined.RecordVoiceOver, ToolCategory.CONVERT,
            listOf("speech", "tts", "read aloud", "voice", "speak", "accessibility"),
        ),
        Tool(
            ToolIds.QR_SCAN, "QR Scanner",
            "Read QR codes and barcodes with the camera",
            Icons.Outlined.QrCodeScanner, ToolCategory.CONVERT,
            listOf("barcode", "scan", "camera", "read", "ean", "upc"),
        ),

        Tool(
            ToolIds.CALCULATOR, "Calculator",
            "Type an expression with brackets and powers",
            Icons.Outlined.Calculate, ToolCategory.CALCULATE,
            listOf("maths", "math", "arithmetic", "sum", "multiply", "divide", "brackets"),
        ),
        Tool(
            ToolIds.TALLY, "Tally Counter",
            "Named counters that remember their totals",
            Icons.Outlined.PlusOne, ToolCategory.CALCULATE,
            listOf("count", "counter", "clicker", "score", "stock take", "tally"),
        ),
        Tool(
            ToolIds.TIP, "Tip Calculator",
            "Split a bill and work out the tip",
            Icons.Outlined.Restaurant, ToolCategory.CALCULATE,
            listOf("bill", "gratuity", "split", "restaurant", "service"),
        ),
        Tool(
            ToolIds.PERCENTAGE, "Percentages",
            "Percent of, change, and discount calculations",
            Icons.Outlined.Percent, ToolCategory.CALCULATE,
            listOf("discount", "increase", "decrease", "vat", "markup", "percent"),
        ),
        Tool(
            ToolIds.DATE_CALC, "Date Calculator",
            "Days between dates and date arithmetic",
            Icons.Outlined.CalendarMonth, ToolCategory.CALCULATE,
            listOf("days between", "age", "deadline", "weeks", "calendar"),
        ),
        Tool(
            ToolIds.BMI, "BMI Calculator",
            "Body mass index in metric or imperial units",
            Icons.Outlined.MonitorHeart, ToolCategory.CALCULATE,
            listOf("weight", "height", "body mass index", "health"),
        ),
        Tool(
            ToolIds.PASSWORD, "Password Generator",
            "Strong random passwords and passphrases",
            Icons.Outlined.Key, ToolCategory.CALCULATE,
            listOf("random", "secure", "passphrase", "entropy", "generate"),
        ),
        Tool(
            ToolIds.RANDOM, "Random Picker",
            "Dice, coin flips, numbers and list picking",
            Icons.Outlined.Casino, ToolCategory.CALCULATE,
            listOf("dice", "coin", "flip", "shuffle", "lottery", "choose"),
        ),
    )

    private val byId: Map<String, Tool> = all.associateBy { it.id }

    fun find(id: String): Tool? = byId[id]

    fun byCategory(): List<Pair<ToolCategory, List<Tool>>> =
        ToolCategory.entries.map { category -> category to all.filter { it.category == category } }
}
