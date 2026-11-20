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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun SourceEditor(
    text: String,
    onTextChange: (String) -> Unit,
    currentLine: Int? = null,
    errorLines: Set<Int> = emptySet(),
    breakpoints: Set<Int> = emptySet(),
    jumpToLineRequest: Pair<Int, Int>? = null,
    onToggleBreakpoint: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()
    var viewportHeightPx by remember { mutableStateOf(0) }

    var textFieldValue by remember { mutableStateOf(TextFieldValue(text)) }
    if (textFieldValue.text != text) {
        val clampedCursor = textFieldValue.selection.start.coerceIn(0, text.length)
        textFieldValue = TextFieldValue(text = text, selection = TextRange(clampedCursor))
    }

    val lineCount = remember(text) {
        var count = 1
        for (ch in text) {
            if (ch == '\n') count++
        }
        count
    }

    val fontSize = 12.sp
    val lineHeight = 18.sp

    val sharedTextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = fontSize,
        lineHeight = lineHeight,
    )

    val density = LocalDensity.current
    val rowHeightPx = remember(density) { with(density) { lineHeight.toPx() } }
    val digits = lineCount.toString().length.coerceAtLeast(3)
    val gutterWidth = (24 + digits * 8).dp

    val currentLineColor = MaterialTheme.colorScheme.primary
    val defaultLineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    val errorColor = MaterialTheme.colorScheme.error
    val currentOnToggleBreakpoint by rememberUpdatedState(onToggleBreakpoint)

    // Scroll to active line when stepping or hitting a breakpoint
    LaunchedEffect(currentLine) {
        if (currentLine != null && rowHeightPx > 0f && viewportHeightPx > 0) {
            val targetTop = ((currentLine - 1) * rowHeightPx).roundToInt()
            val targetBottom = targetTop + rowHeightPx.roundToInt()
            val curScroll = verticalScrollState.value
            if (targetTop < curScroll) {
                verticalScrollState.animateScrollTo(targetTop.coerceAtLeast(0))
            } else if (targetBottom > curScroll + viewportHeightPx) {
                verticalScrollState.animateScrollTo((targetBottom - viewportHeightPx).coerceAtLeast(0))
            }
        }
    }

    // Jump to requested line (e.g., when clicking an assembly error)
    LaunchedEffect(jumpToLineRequest) {
        val reqLine = jumpToLineRequest?.first ?: return@LaunchedEffect
        val targetOffset = lineStartOffset(text, reqLine)
        textFieldValue = textFieldValue.copy(selection = TextRange(targetOffset))
        val targetPx = ((reqLine - 1) * rowHeightPx).roundToInt().coerceAtLeast(0)
        verticalScrollState.animateScrollTo(targetPx)
    }

    val gutterAnnotatedString = remember(lineCount, currentLine, breakpoints, errorLines, digits) {
        buildAnnotatedString {
            for (i in 0 until lineCount) {
                val lineNum = i + 1
                val isCurrent = lineNum == currentLine
                val isBp = lineNum in breakpoints
                val isErr = lineNum in errorLines

                if (isBp) {
                    withStyle(SpanStyle(color = Color(0xFFEF5350))) {
                        append("● ")
                    }
                } else {
                    append("  ")
                }

                val numColor = when {
                    isErr -> errorColor
                    isCurrent -> currentLineColor
                    else -> defaultLineColor
                }

                withStyle(SpanStyle(color = numColor)) {
                    append(lineNum.toString().padStart(digits, ' '))
                }

                if (i < lineCount - 1) append("\n")
            }
        }
    }

    val highlightTransformation = remember(currentLine) {
        AssemblyHighlightTransformation(currentLine)
    }

    Surface(
        modifier = modifier.onSizeChanged { viewportHeightPx = it.height },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Row(modifier = Modifier.fillMaxWidth().verticalScroll(verticalScrollState)) {
            Text(
                text = gutterAnnotatedString,
                style = sharedTextStyle,
                modifier = Modifier
                    .width(gutterWidth)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .pointerInput(lineCount, rowHeightPx) {
                        detectTapGestures { offset ->
                            val clickedLineIndex = (offset.y / rowHeightPx).toInt()
                            val targetLine = clickedLineIndex + 1
                            if (targetLine in 1..lineCount) {
                                currentOnToggleBreakpoint(targetLine)
                            }
                        }
                    },
            )

            BasicTextField(
                value = textFieldValue,
                onValueChange = { updated ->
                    val textChanged = updated.text != textFieldValue.text
                    textFieldValue = updated
                    if (textChanged) {
                        onTextChange(updated.text)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .horizontalScroll(horizontalScrollState)
                    .padding(horizontal = 6.dp)
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && event.key == Key.Tab) {
                            val cur = textFieldValue
                            val start = minOf(cur.selection.start, cur.selection.end).coerceIn(0, cur.text.length)
                            val end = maxOf(cur.selection.start, cur.selection.end).coerceIn(0, cur.text.length)
                            val inserted = cur.text.substring(0, start) + "    " + cur.text.substring(end)
                            val newCursor = start + 4
                            textFieldValue = TextFieldValue(inserted, TextRange(newCursor))
                            onTextChange(inserted)
                            true
                        } else {
                            false
                        }
                    },
                textStyle = sharedTextStyle.copy(color = MaterialTheme.colorScheme.onSurface),
                visualTransformation = highlightTransformation,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.TopStart) {
                        if (text.isEmpty()) {
                            Text(
                                text = "; Enter 8051 assembly source...",
                                style = sharedTextStyle.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                ),
                            )
                        }
                        innerTextField()
                    }
                },
            )
        }
    }
}

private fun lineStartOffset(text: String, line1Based: Int): Int {
    if (line1Based <= 1) return 0
    var line = 1
    for (i in text.indices) {
        if (text[i] == '\n') {
            line++
            if (line == line1Based) return (i + 1).coerceAtMost(text.length)
        }
    }
    return text.length
}
