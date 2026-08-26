package com.utilitybox.app.tools.hardware

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.utilitybox.app.ui.common.HintText
import com.utilitybox.app.ui.common.SectionCard
import com.utilitybox.app.ui.common.ToolScaffold
import com.utilitybox.app.util.KeepScreenOn

private data class TestPattern(val name: String, val description: String)

private val PATTERNS = listOf(
    TestPattern("Black", "Stuck pixels show as bright dots"),
    TestPattern("White", "Dead pixels show as dark dots"),
    TestPattern("Red", "Checks the red sub-pixels"),
    TestPattern("Green", "Checks the green sub-pixels"),
    TestPattern("Blue", "Checks the blue sub-pixels"),
    TestPattern("Grey", "Reveals uneven backlight and tinting"),
    TestPattern("Grey ramp", "Checks banding across the grey range"),
    TestPattern("Colour ramp", "Checks colour gradient smoothness"),
)

@Composable
fun ScreenTestScreen(onBack: () -> Unit) {
    var fullScreenIndex by remember { mutableStateOf<Int?>(null) }
    KeepScreenOn(active = fullScreenIndex != null)

    val index = fullScreenIndex
    if (index != null) {
        FullScreenPattern(
            index = index,
            onNext = { fullScreenIndex = (index + 1) % PATTERNS.size },
            onExit = { fullScreenIndex = null },
        )
        return
    }

    ToolScaffold(title = "Screen Test", onBack = onBack) {
        SectionCard {
            Text(
                "Each pattern fills the whole screen. Tap to move to the next one, " +
                    "press and hold or use Back to return here.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        PATTERNS.forEachIndexed { patternIndex, pattern ->
            Button(
                onClick = { fullScreenIndex = patternIndex },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Text(pattern.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                        pattern.description,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        HintText(
            "A stuck pixel is permanently lit in one colour; a dead pixel stays black. " +
                "Look at the screen from straight on in a dim room for the clearest result."
        )
    }
}

@Composable
private fun FullScreenPattern(index: Int, onNext: () -> Unit, onExit: () -> Unit) {
    BackHandler(onBack = onExit)
    var showHint by remember { mutableIntStateOf(3) }

    val modifier = Modifier
        .fillMaxSize()
        .clickable(onClick = {
            if (showHint > 0) showHint = 0
            onNext()
        })

    Box(
        modifier = when (index) {
            0 -> modifier.background(Color.Black)
            1 -> modifier.background(Color.White)
            2 -> modifier.background(Color.Red)
            3 -> modifier.background(Color.Green)
            4 -> modifier.background(Color.Blue)
            5 -> modifier.background(Color(0xFF808080))
            6 -> modifier.background(
                Brush.verticalGradient(listOf(Color.Black, Color.White))
            )

            else -> modifier.background(
                Brush.verticalGradient(
                    listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta)
                )
            )
        },
        contentAlignment = Alignment.BottomCenter,
    ) {
        if (showHint > 0) {
            Text(
                text = "${PATTERNS[index].name} — tap for next, Back to exit",
                color = if (index == 1 || index == 5) Color.Black else Color.White,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            )
        }
    }
}
