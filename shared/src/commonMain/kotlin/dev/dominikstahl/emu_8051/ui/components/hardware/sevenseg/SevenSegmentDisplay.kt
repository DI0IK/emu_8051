package dev.dominikstahl.emu_8051.ui.components.hardware.sevenseg

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val SegmentDefs = listOf(
    listOf(8, 6, 44, 8),     // a  (bit 0)
    listOf(52, 10, 8, 36),   // b  (bit 1)
    listOf(52, 54, 8, 36),   // c  (bit 2)
    listOf(8, 86, 44, 8),    // d  (bit 3)
    listOf(0, 54, 8, 36),    // e  (bit 4)
    listOf(0, 10, 8, 36),    // f  (bit 5)
    listOf(8, 48, 44, 8),    // g  (bit 6)
)

@Composable
fun SevenSegmentDisplay(
    value: Int,
    color: Color = Color(0xFFFF4444),
    modifier: Modifier = Modifier,
) {
    val onColor = color
    val offColor = color.copy(alpha = 0.18f)
    val dpOn = (value and 0x80) != 0

    Canvas(
        modifier = modifier.size(width = 32.dp, height = 48.dp),
        onDraw = {
            val sx = size.width / 66f
            val sy = size.height / 100f

            for (i in 0..6) {
                val on = (value and (1 shl i)) != 0
                val s = SegmentDefs[i]
                drawRoundRect(
                    color = if (on) onColor else offColor,
                    topLeft = Offset(s[0] * sx, s[1] * sy),
                    size = Size(s[2] * sx, s[3] * sy),
                    cornerRadius = CornerRadius(2f * sx, 2f * sy),
                )
            }

            // DP (decimal point) — to the right of segment c (digit ends at x=60)
            val dpCenter = Offset(65f * sx, 90f * sy)
            val dpRadius = 3f * sx.coerceAtMost(sy)
            drawCircle(
                color = if (dpOn) onColor else offColor,
                radius = dpRadius,
                center = dpCenter,
            )
        },
    )
}
