package dev.dominikstahl.emu_8051.ui.components.hardware.keypad

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Default4x4Labels = listOf(
    listOf("1", "2", "3", "A"),
    listOf("4", "5", "6", "B"),
    listOf("7", "8", "9", "C"),
    listOf("*", "0", "#", "D"),
)

@Composable
fun MatrixKeypad(
    rows: Int,
    cols: Int,
    pressedKeys: Set<String>,
    onKey: (Int, Int, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val labels = if (rows == 4 && cols == 4) Default4x4Labels
    else List(rows) { r -> List(cols) { c -> "${r * cols + c}" } }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        for (r in 0 until rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                for (c in 0 until cols) {
                    val key = "$r,$c"
                    val pressed = key in pressedKeys
                    KeyButton(
                        label = labels.getOrNull(r)?.getOrNull(c) ?: "$r$c",
                        pressed = pressed,
                        onClick = { onKey(r, c, !pressed) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyButton(
    label: String,
    pressed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (pressed) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (pressed) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 14.sp,
            fontWeight = if (pressed) FontWeight.Bold else FontWeight.Normal,
            color = fg,
            textAlign = TextAlign.Center,
        )
    }
}
