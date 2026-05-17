package dev.dominikstahl.emu_8051.ui.components.hardware.keypad

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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

private val letterKeys = setOf("A", "B", "C", "D")
private val symbolKeys = setOf("*", "#")
private enum class AccentType { SECONDARY, TERTIARY }

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
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        for (r in 0 until rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                for (c in 0 until cols) {
                    val key = "$r,$c"
                    val pressed = key in pressedKeys
                    val label = labels.getOrNull(r)?.getOrNull(c) ?: "$r$c"
                    val accent: Pair<Color, AccentType>? = when {
                        label in letterKeys -> MaterialTheme.colorScheme.tertiary to AccentType.TERTIARY
                        label in symbolKeys -> MaterialTheme.colorScheme.secondary to AccentType.SECONDARY
                        else -> null
                    }
                    KeyButton(
                        label = label,
                        pressed = pressed,
                        accent = accent,
                        onPress = { onKey(r, c, true) },
                        onRelease = { onKey(r, c, false) },
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
    accent: Pair<Color, AccentType>?,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accentColor = accent?.first
    val defaultBg = MaterialTheme.colorScheme.surfaceVariant
    val defaultFg = MaterialTheme.colorScheme.onSurfaceVariant
    val bg = if (pressed) {
        accentColor ?: MaterialTheme.colorScheme.primary
    } else {
        if (accentColor != null) accentColor.copy(alpha = 0.12f) else defaultBg
    }
    val fg = if (pressed) {
        when (accent?.second) {
            AccentType.TERTIARY -> MaterialTheme.colorScheme.onTertiary
            AccentType.SECONDARY -> MaterialTheme.colorScheme.onSecondary
            null -> MaterialTheme.colorScheme.onPrimary
        }
    } else {
        accentColor ?: defaultFg
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(5.dp))
            .background(bg)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(5.dp))
            .pointerInput(onPress) {
                detectTapGestures(
                    onPress = {
                        onPress()
                        tryAwaitRelease()
                        onRelease()
                    }
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 15.sp,
            fontWeight = if (pressed) FontWeight.Bold else FontWeight.Medium,
            color = fg,
            textAlign = TextAlign.Center,
        )
    }
}
