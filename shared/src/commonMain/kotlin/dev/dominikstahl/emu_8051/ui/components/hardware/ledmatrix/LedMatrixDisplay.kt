package dev.dominikstahl.emu_8051.ui.components.hardware.ledmatrix

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

@Composable
fun LedMatrixDisplay(
    brightness: List<List<Float>>,
    color: Color = Color(0xFFFF4444),
    modifier: Modifier = Modifier,
) {
    val offColor = Color(0xFF333333)
    val ledSize = 18.dp
    val spacing = 2.dp

    Column(
        modifier = modifier.padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(spacing),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        for (r in 0 until 8) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                for (c in 0 until 8) {
                    val b = (brightness.getOrNull(r)?.getOrNull(c) ?: 0f).coerceIn(0f, 1f)
                    val bgColor = if (b > 0.02f) lerp(offColor, color, b) else offColor

                    Box(
                        modifier = Modifier
                            .size(ledSize)
                            .clip(CircleShape)
                            .background(bgColor),
                    )
                }
            }
        }
    }
}
