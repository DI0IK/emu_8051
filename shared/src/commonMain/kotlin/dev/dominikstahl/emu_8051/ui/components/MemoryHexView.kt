package dev.dominikstahl.emu_8051.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

private val HEX_DIGITS = "0123456789ABCDEF".toCharArray()

private fun Int.toHexChar(): Char = HEX_DIGITS[this and 0x0F]

/**
 * Enhanced line-renderer accepting a dynamic number of bytes per line.
 */
private fun formatHexLine(
    data: UByteArray,
    rowIndex: Int,
    bytesPerLine: Int,
    baseAddress: Int,
    addressColor: androidx.compose.ui.graphics.Color,
    hexColor: androidx.compose.ui.graphics.Color,
    asciiColor: androidx.compose.ui.graphics.Color
): AnnotatedString {
    val dataOffset = rowIndex * bytesPerLine
    val displayOffset = baseAddress + dataOffset
    val total = data.size

    return buildAnnotatedString {
        // 1. Dynamic Address formatting based on 16-bit space
        withStyle(SpanStyle(color = addressColor)) {
            append(((displayOffset ushr 12) and 0xF).toHexChar())
            append(((displayOffset ushr 8) and 0xF).toHexChar())
            append(((displayOffset ushr 4) and 0xF).toHexChar())
            append((displayOffset and 0xF).toHexChar())
            append(":  ")
        }

        // 2. Hex Values
        withStyle(SpanStyle(color = hexColor)) {
            for (i in 0 until bytesPerLine) {
                val targetIndex = dataOffset + i
                if (targetIndex < total) {
                    val byteVal = data[targetIndex].toInt()
                    append(HEX_DIGITS[(byteVal ushr 4) and 0x0F])
                    append(HEX_DIGITS[byteVal and 0x0F])
                    append(' ')
                } else {
                    append("   ")
                }
            }
            append(" ")
        }

        // 3. ASCII Values
        withStyle(SpanStyle(color = asciiColor)) {
            for (i in 0 until bytesPerLine) {
                val targetIndex = dataOffset + i
                if (targetIndex < total) {
                    val byteVal = data[targetIndex].toInt()
                    if (byteVal in 0x20..0x7E) append(byteVal.toChar()) else append('.')
                }
            }
        }
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
@Composable
fun MemoryHexView(
    data: UByteArray?,
    stateKey: Any,
    baseAddress: Int = 0,
    modifier: Modifier = Modifier,
    overrideBytesPerLine: Int? = null,
    onWidthCalculated: (androidx.compose.ui.unit.Dp) -> Unit = {}
) {
    if (data == null || data.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)

    // Helper function to handle the actual rendering logic
    @Composable
    fun RenderHexList(bytesPerLine: Int) {
        val totalChars = 4 + 3 + (bytesPerLine * 3) + 1 + bytesPerLine

        remember(bytesPerLine, textStyle) {
            val templateString = "X".repeat(totalChars)
            val widthInPixels = textMeasurer.measure(templateString, style = textStyle).size.width
            val widthInDp = with(density) { widthInPixels.toDp() } + 32.dp
            onWidthCalculated(widthInDp)
            true
        }

        val rowCount = remember(data.size, bytesPerLine) {
            (data.size + bytesPerLine - 1) / bytesPerLine
        }
        val hScroll = rememberScrollState()
        val addressColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        val hexColor = MaterialTheme.colorScheme.onSurface
        val asciiColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

        LazyColumn(
            modifier = modifier.horizontalScroll(hScroll).padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            items(count = rowCount, key = { index -> "$index-$bytesPerLine-$stateKey" }) { rowIndex ->
                val annotatedLine = remember(stateKey, rowIndex, bytesPerLine, baseAddress) {
                    formatHexLine(data, rowIndex, bytesPerLine, baseAddress, addressColor, hexColor, asciiColor)
                }
                Text(text = annotatedLine, style = textStyle, maxLines = 1)
            }
        }
    }

    // If explicit bytes are provided (Desktop), use them directly without reading local constraints
    if (overrideBytesPerLine != null) {
        RenderHexList(bytesPerLine = overrideBytesPerLine)
    } else {
        // Otherwise (Mobile), dynamically adapt to available screen width
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val bytesPerLine = remember(maxWidth) {
                when {
                    maxWidth < 380.dp -> 4
                    maxWidth < 650.dp -> 8
                    else -> 16
                }
            }
            RenderHexList(bytesPerLine = bytesPerLine)
        }
    }
}