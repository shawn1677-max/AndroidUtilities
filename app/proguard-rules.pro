# ZXing core is pure Java and reflection-free; default rules suffice.
-dontwarn com.google.zxing.**

# Keep Compose runtime metadata that R8 may otherwise strip in aggressive modes.
-keepclassmembers class ** {
    @androidx.compose.runtime.Composable <methods>;
}
