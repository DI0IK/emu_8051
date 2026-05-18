package dev.dominikstahl.emu_8051.ui.components.hardware.ledmatrix

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LedMatrixDisplay(
    data: List<List<Boolean>>,
    color: Color = Color(0xFFFF4444),
    modifier: Modifier = Modifier,
) {
    val onColor = color
    val offColor = Color(0x333333)
    val ledSize = 20.dp
    val spacing = 2.dp

    Column(
        modifier = modifier.padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(spacing),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Render 8x8 grid
        for (r in 0 until 8) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                for (c in 0 until 8) {
                    val isOn = data.getOrNull(r)?.getOrNull(c) ?: false
                    val bgColor = if (isOn) onColor else offColor

                    Box(
                        modifier = Modifier
                            .size(ledSize)
                            .clip(CircleShape)
                            .background(bgColor)
                    )
                }
            }
        }
    }
}
