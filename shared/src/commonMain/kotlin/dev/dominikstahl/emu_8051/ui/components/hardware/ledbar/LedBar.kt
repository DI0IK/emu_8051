package dev.dominikstahl.emu_8051.ui.components.hardware.ledbar

import dev.dominikstahl.emu_8051.ui.components.hardware.led.LedIndicator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun LedBar(
    portValue: Int,
    color: Color = Color(0xFFFF4444),
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (pin in 7 downTo 0) {
            LedIndicator(on = (portValue and (1 shl pin)) != 0, color = color)
        }
    }
}
