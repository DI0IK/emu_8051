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
    val onColor = color
    val offColor = color.copy(alpha = 0.15f)

    Canvas(
        modifier = modifier.size(20.dp),
        onDraw = {
            val center = Offset(size.width / 2f, size.height / 2f)
            val r = size.minDimension / 2f
            val coreR = r * 0.55f

            if (on) {
                drawCircle(onColor.copy(alpha = 0.2f), r * 0.85f, center)
            }
            drawCircle(
                color = if (on) onColor else offColor,
                radius = coreR,
                center = center,
            )
            if (on) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.35f),
                    radius = coreR * 0.5f,
                    center = Offset(center.x - r * 0.12f, center.y - r * 0.12f),
                )
            }
        }
    )
}
