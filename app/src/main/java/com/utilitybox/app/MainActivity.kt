package com.utilitybox.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.utilitybox.app.nav.UtilityBoxNavHost
import com.utilitybox.app.ui.theme.UtilityBoxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            UtilityBoxTheme {
                UtilityBoxNavHost()
            }
        }
    }
}
