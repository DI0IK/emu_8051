package dev.dominikstahl.emu_8051.ui.components.hardware.uart

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun UartTerminalView(
    txText: String,
    onSendChar: (Char) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = MaterialTheme.colorScheme.surfaceVariant
    val fg = MaterialTheme.colorScheme.onSurface
    val scrollState = rememberScrollState()

    Column(modifier = modifier) {
        Text(
            "UART Terminal",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(bg, MaterialTheme.shapes.small)
                .padding(4.dp)
                .verticalScroll(scrollState),
        ) {
            Text(
                text = txText.ifEmpty() { "--- TX output ---" },
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = fg,
                ),
            )
        }

        var rxText by remember { mutableStateOf("") }

        BasicTextField(
            value = rxText,
            onValueChange = { newValue ->
                if (newValue.length > rxText.length) {
                    val added = newValue.lastOrNull()
                    if (added != null) onSendChar(added)
                }
                rxText = newValue
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .background(bg, MaterialTheme.shapes.small)
                .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = fg,
            ),
            cursorBrush = SolidColor(fg),
            singleLine = false,
            decorationBox = { innerTextField ->
                if (rxText.isEmpty()) {
                    Text(
                        "Type to send...",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = fg.copy(alpha = 0.4f),
                        ),
                    )
                }
                innerTextField()
            },
        )
    }
}
