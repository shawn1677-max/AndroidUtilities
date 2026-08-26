package com.utilitybox.app.tools.hardware

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.utilitybox.app.ui.common.HintText
import com.utilitybox.app.ui.common.ToolScaffold

private const val GRID_COLUMNS = 8
private const val GRID_ROWS = 14

private val POINTER_COLOURS = listOf(
    Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFF43A047), Color(0xFFFB8C00),
    Color(0xFF8E24AA), Color(0xFF00ACC1), Color(0xFFFDD835), Color(0xFF6D4C41),
    Color(0xFF3949AB), Color(0xFFD81B60),
)

@Composable
fun TouchTestScreen(onBack: () -> Unit) {
    val pointers = remember { mutableStateMapOf<Long, Offset>() }
    val visited = remember { mutableStateMapOf<Int, Boolean>() }
    var maxPointers by remember { mutableIntStateOf(0) }

    ToolScaffold(title = "Touch Test", onBack = onBack, scrollable = false) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        "Active points: ${pointers.size}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "Most at once: $maxPointers · Cells covered: ${visited.size}/${GRID_COLUMNS * GRID_ROWS}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedButton(onClick = {
                    visited.clear()
                    maxPointers = 0
                }) { Text("Reset") }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp)
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                pointers.clear()
                                event.changes
                                    .filter { it.pressed }
                                    .forEach { change ->
                                        pointers[change.id.value] = change.position
                                        val column = (change.position.x / size.width * GRID_COLUMNS)
                                            .toInt().coerceIn(0, GRID_COLUMNS - 1)
                                        val row = (change.position.y / size.height * GRID_ROWS)
                                            .toInt().coerceIn(0, GRID_ROWS - 1)
                                        visited[row * GRID_COLUMNS + column] = true
                                        change.consume()
                                    }
                                if (pointers.size > maxPointers) maxPointers = pointers.size
                            }
                        }
                    },
            ) {
                TouchCanvas(pointers = pointers.toMap(), visited = visited.keys.toSet())
            }

            HintText(
                "Drag a finger over every square to check for dead zones, then place several " +
                    "fingers at once to see how many simultaneous touches the digitiser reports.",
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Composable
private fun TouchCanvas(pointers: Map<Long, Offset>, visited: Set<Int>) {
    val gridLine = MaterialTheme.colorScheme.outline
    val covered = MaterialTheme.colorScheme.primary
    val crosshair = MaterialTheme.colorScheme.onSurface

    Canvas(Modifier.fillMaxSize()) {
        val cellWidth = size.width / GRID_COLUMNS
        val cellHeight = size.height / GRID_ROWS

        visited.forEach { index ->
            val column = index % GRID_COLUMNS
            val row = index / GRID_COLUMNS
            drawRect(
                color = covered.copy(alpha = 0.18f),
                topLeft = Offset(column * cellWidth, row * cellHeight),
                size = androidx.compose.ui.geometry.Size(cellWidth, cellHeight),
            )
        }

        for (column in 0..GRID_COLUMNS) {
            val x = column * cellWidth
            drawLine(gridLine.copy(alpha = 0.4f), Offset(x, 0f), Offset(x, size.height), 1f)
        }
        for (row in 0..GRID_ROWS) {
            val y = row * cellHeight
            drawLine(gridLine.copy(alpha = 0.4f), Offset(0f, y), Offset(size.width, y), 1f)
        }

        pointers.values.forEachIndexed { index, position ->
            val colour = POINTER_COLOURS[index % POINTER_COLOURS.size]
            drawCircle(colour.copy(alpha = 0.25f), radius = 70f, center = position)
            drawCircle(colour, radius = 70f, center = position, style = Stroke(4f))
            drawLine(
                crosshair,
                Offset(position.x - 90f, position.y),
                Offset(position.x + 90f, position.y),
                2f,
            )
            drawLine(
                crosshair,
                Offset(position.x, position.y - 90f),
                Offset(position.x, position.y + 90f),
                2f,
            )
        }
    }
}
