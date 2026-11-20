@file:OptIn(ExperimentalUnsignedTypes::class)

package dev.dominikstahl.emu_8051.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val HEX_DIGITS = "0123456789ABCDEF".toCharArray()

private fun Int.toHexChar(): Char = HEX_DIGITS[this and 0x0F]

private fun formatHexLine(
    data: UByteArray,
    rowIndex: Int,
    bytesPerLine: Int,
    baseAddress: Int,
    highlightAddress: Int?,
    addressColor: Color,
    hexColor: Color,
    asciiColor: Color,
    highlightColor: Color,
): AnnotatedString {
    val dataOffset = rowIndex * bytesPerLine
    val displayOffset = baseAddress + dataOffset
    val total = data.size

    return buildAnnotatedString {
        withStyle(SpanStyle(color = addressColor)) {
            append(((displayOffset ushr 12) and 0xF).toHexChar())
            append(((displayOffset ushr 8) and 0xF).toHexChar())
            append(((displayOffset ushr 4) and 0xF).toHexChar())
            append((displayOffset and 0xF).toHexChar())
            append(":  ")
        }

        for (i in 0 until bytesPerLine) {
            val targetIndex = dataOffset + i
            if (targetIndex < total) {
                val absAddr = baseAddress + targetIndex
                val isHighlighted = highlightAddress != null && absAddr == highlightAddress
                val byteVal = data[targetIndex].toInt()
                val style = if (isHighlighted) {
                    SpanStyle(
                        color = highlightColor,
                        background = highlightColor.copy(alpha = 0.2f),
                        fontWeight = FontWeight.Bold,
                    )
                } else {
                    SpanStyle(color = hexColor)
                }
                withStyle(style) {
                    append(HEX_DIGITS[(byteVal ushr 4) and 0x0F])
                    append(HEX_DIGITS[byteVal and 0x0F])
                }
                append(' ')
            } else {
                append("   ")
            }
        }
        append(" ")

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

@Composable
fun MemoryHexView(
    data: UByteArray?,
    stateKey: Any,
    baseAddress: Int = 0,
    highlightAddress: Int? = null,
    scrollToAddressRequest: Pair<Int, Int>? = null,
    modifier: Modifier = Modifier,
    overrideBytesPerLine: Int? = null,
    onWidthCalculated: (Dp) -> Unit = {},
) {
    if (data == null || data.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)

    @Composable
    fun RenderHexList(bytesPerLine: Int) {
        val totalChars = 4 + 3 + (bytesPerLine * 3) + 1 + bytesPerLine
        val widthInDp = remember(bytesPerLine, textStyle, density) {
            val templateString = "X".repeat(totalChars)
            val widthInPixels = textMeasurer.measure(templateString, style = textStyle).size.width
            with(density) { widthInPixels.toDp() } + 32.dp
        }
        LaunchedEffect(widthInDp) {
            onWidthCalculated(widthInDp)
        }

        val rowCount = remember(data.size, bytesPerLine) {
            (data.size + bytesPerLine - 1) / bytesPerLine
        }
        val listState = rememberLazyListState()
        val hScroll = rememberScrollState()
        val addressColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        val hexColor = MaterialTheme.colorScheme.onSurface
        val asciiColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        val highlightColor = MaterialTheme.colorScheme.primary

        LaunchedEffect(scrollToAddressRequest, bytesPerLine, baseAddress, rowCount) {
            val targetAddr = scrollToAddressRequest?.first ?: return@LaunchedEffect
            val relAddr = (targetAddr - baseAddress).coerceIn(0, (data.size - 1).coerceAtLeast(0))
            val targetRow = (relAddr / bytesPerLine).coerceIn(0, (rowCount - 1).coerceAtLeast(0))
            listState.scrollToItem(targetRow)
        }

        LazyColumn(
            state = listState,
            modifier = modifier.horizontalScroll(hScroll).padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            items(count = rowCount, key = { index -> index }) { rowIndex ->
                val rowStart = baseAddress + rowIndex * bytesPerLine
                val rowEnd = rowStart + bytesPerLine - 1
                val rowHighlight = if (highlightAddress != null && highlightAddress in rowStart..rowEnd) {
                    highlightAddress
                } else null

                val annotatedLine = remember(stateKey, rowIndex, bytesPerLine, baseAddress, rowHighlight) {
                    formatHexLine(
                        data = data,
                        rowIndex = rowIndex,
                        bytesPerLine = bytesPerLine,
                        baseAddress = baseAddress,
                        highlightAddress = rowHighlight,
                        addressColor = addressColor,
                        hexColor = hexColor,
                        asciiColor = asciiColor,
                        highlightColor = highlightColor,
                    )
                }
                Text(text = annotatedLine, style = textStyle, maxLines = 1)
            }
        }
    }

    if (overrideBytesPerLine != null) {
        RenderHexList(bytesPerLine = overrideBytesPerLine)
    } else {
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
