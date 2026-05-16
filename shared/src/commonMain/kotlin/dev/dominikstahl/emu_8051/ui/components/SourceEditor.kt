package dev.dominikstahl.emu_8051.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SourceEditor(
    text: String,
    onTextChange: (String) -> Unit,
    currentLine: Int? = null,
    breakpoints: Set<Int> = emptySet(),
    onToggleBreakpoint: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    val lines = text.split('\n')
    val lineCount = lines.size

    val fontSize = 12.sp
    val lineHeight = 18.sp

    // The shared style forces both the code canvas and the gutter lines to align identically
    val sharedTextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = fontSize,
        lineHeight = lineHeight
    )

    val density = LocalDensity.current
    val rowHeightPx = remember(density) { with(density) { lineHeight.toPx() } }

    // 1. Extract the theme colors out here, where the Composable context is fully intact
    val currentLineColor = MaterialTheme.colorScheme.primary
    val defaultLineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)

    // 2. Pass them cleanly into your string builder optimization block
    val gutterAnnotatedString = remember(lineCount, currentLine, breakpoints) {
        buildAnnotatedString {
            for (i in 0 until lineCount) {
                val lineNum = i + 1
                val isCurrent = lineNum == currentLine
                val isBp = lineNum in breakpoints

                if (isBp) {
                    withStyle(SpanStyle(color = androidx.compose.ui.graphics.Color(0xFFEF5350))) {
                        append("● ")
                    }
                } else {
                    append("  ")
                }

                // 3. Use the pre-extracted color variables here safely!
                val numColor = if (isCurrent) currentLineColor else defaultLineColor

                withStyle(SpanStyle(color = numColor)) {
                    append(lineNum.toString().padStart(3, ' '))
                }

                if (i < lineCount - 1) append("\n")
            }
        }
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Row(modifier = Modifier.fillMaxWidth().verticalScroll(verticalScrollState)) {

            // Left Column Gutter: Rendered as a single Text block to eliminate rounding drift
            Text(
                text = gutterAnnotatedString,
                style = sharedTextStyle,
                modifier = Modifier
                    .width(52.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(vertical = 0.dp)
                    // Seamless breakpoint click targeting via Y-offset pixel measurement
                    .pointerInput(lineCount) {
                        detectTapGestures { offset ->
                            val clickedLineIndex = (offset.y / rowHeightPx).toInt()
                            val targetLine = clickedLineIndex + 1
                            if (targetLine in 1..lineCount) {
                                onToggleBreakpoint(targetLine)
                            }
                        }
                    }
            )

            // Right Column Canvas: The Core Code Text Input Field
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .horizontalScroll(horizontalScrollState)
                    .padding(horizontal = 6.dp),
                textStyle = sharedTextStyle.copy(color = MaterialTheme.colorScheme.onSurface),
                visualTransformation = AssemblyHighlightTransformation(currentLine, breakpoints),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.TopStart) {
                        if (text.isEmpty()) {
                            Text(
                                text = "; Enter 8051 assembly source...",
                                style = sharedTextStyle.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            )
                        }
                        innerTextField()
                    }
                },
            )
        }
    }
}