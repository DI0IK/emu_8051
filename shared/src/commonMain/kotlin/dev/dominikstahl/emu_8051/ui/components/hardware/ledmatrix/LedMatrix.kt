package dev.dominikstahl.emu_8051.ui.components.hardware.ledmatrix

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun LedMatrix(
    rowPortValue: Int,
    dataPortValue: Int,
    snapshot: LedMatrixSnapshot? = null,
    color: Color = Color(0xFFFF4444),
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier.size(144.dp),
        onDraw = {
            val cellSize = size.width / 8f
            val borderColor = Color.Gray
            val offColor = color.copy(alpha = 0.1f)

            // Use snapshot data if available (includes LED persistence)
            // Otherwise fall back to instantaneous port values
            val ledStates = snapshot?.ledStates
            
            for (row in 0..7) {
                for (col in 0..7) {
                    val ledOn = if (ledStates != null) {
                        // Use persistent LED state from snapshot
                        val wordIdx = row / 4
                        val bitOffset = (row % 4) * 8
                        val rowBits = (ledStates[wordIdx] shr bitOffset) and 0xFF
                        (rowBits and (1 shl col)) != 0
                    } else {
                        // Fallback to instantaneous port values
                        val rowActive = (rowPortValue and (1 shl row)) == 0  // Active LOW
                        rowActive && (dataPortValue and (1 shl col)) != 0
                    }
                    
                    val x = col * cellSize
                    val y = row * cellSize

                    // Draw LED cell
                    drawRect(
                        color = if (ledOn) color else offColor,
                        topLeft = Offset(x + 1, y + 1),
                        size = Size(cellSize - 2, cellSize - 2),
                    )

                    // Draw border
                    drawRect(
                        color = borderColor,
                        topLeft = Offset(x, y),
                        size = Size(cellSize, cellSize),
                        style = Stroke(width = 0.5f),
                    )
                }
            }
        }
    )
}
