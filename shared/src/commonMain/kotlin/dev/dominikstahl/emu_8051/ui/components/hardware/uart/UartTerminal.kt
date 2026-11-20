package dev.dominikstahl.emu_8051.ui.components.hardware.uart

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun UartTerminalView(
    txText: String,
    rxPendingCount: Int = 0,
    onSendText: (String) -> Unit,
    onClearTx: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val bg = MaterialTheme.colorScheme.surfaceVariant
    val fg = MaterialTheme.colorScheme.onSurface
    val scrollState = rememberScrollState()
    var rxInput by remember { mutableStateOf("") }

    LaunchedEffect(txText.length) {
        if (scrollState.maxValue > 0) {
            scrollState.scrollTo(scrollState.maxValue)
        }
    }

    fun submitInput() {
        if (rxInput.isNotEmpty()) {
            onSendText(rxInput)
            rxInput = ""
        }
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (rxPendingCount > 0) "UART (RX queued: $rxPendingCount)" else "UART Terminal (P3.0/P3.1)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (txText.isNotEmpty()) {
                Text(
                    "Clear",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onClearTx),
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(bg, MaterialTheme.shapes.small)
                .padding(6.dp)
                .verticalScroll(scrollState),
        ) {
            Text(
                text = txText.ifEmpty { "--- TX output ---" },
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = if (txText.isEmpty()) fg.copy(alpha = 0.4f) else fg,
                ),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = rxInput,
                onValueChange = { rxInput = it },
                modifier = Modifier
                    .weight(1f)
                    .background(bg, MaterialTheme.shapes.small)
                    .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small)
                    .padding(horizontal = 6.dp, vertical = 5.dp)
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                            submitInput()
                            true
                        } else {
                            false
                        }
                    },
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = fg,
                ),
                cursorBrush = SolidColor(fg),
                singleLine = true,
                decorationBox = { innerTextField ->
                    if (rxInput.isEmpty()) {
                        Text(
                            "Type RX text + Enter...",
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
            Spacer(Modifier.width(6.dp))
            Text(
                "Send",
                style = MaterialTheme.typography.labelSmall,
                color = if (rxInput.isNotEmpty()) MaterialTheme.colorScheme.primary else fg.copy(alpha = 0.35f),
                modifier = Modifier
                    .clickable(enabled = rxInput.isNotEmpty(), onClick = ::submitInput)
                    .padding(horizontal = 4.dp, vertical = 4.dp),
            )
        }
    }
}
