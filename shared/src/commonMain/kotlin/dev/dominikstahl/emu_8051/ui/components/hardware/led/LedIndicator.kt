package dev.dominikstahl.emu_8051.ui.components.hardware.led

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun LedIndicator(
    on: Boolean,
    color: Color = Color(0xFFFF4444),
    modifier: Modifier = Modifier,
) {
    val glowColor = if (on) color else color.copy(alpha = 0.15f)

    Canvas(
        modifier = modifier.size(16.dp),
        onDraw = {
            val glowRadius = size.minDimension * 0.6f
            val coreRadius = size.minDimension * 0.35f
            val center = Offset(size.width / 2f, size.height / 2f)

            if (on) {
                drawCircle(glowColor.copy(alpha = 0.3f), glowRadius, center)
            }
            drawCircle(
                color = glowColor,
                radius = coreRadius,
                center = center,
            )
        }
    )
}
