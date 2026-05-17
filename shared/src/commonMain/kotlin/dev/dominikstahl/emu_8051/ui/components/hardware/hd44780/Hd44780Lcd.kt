package dev.dominikstahl.emu_8051.ui.components.hardware.hd44780

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Hd44780Lcd(
    lines: List<String>,
    cols: Int,
    rawBytes: List<List<Int>> = emptyList(),
    cgramBytes: List<Int> = emptyList(),
    modifier: Modifier = Modifier,
) {
    val cgramArray = remember(cgramBytes) {
        ByteArray(64) { i -> if (i < cgramBytes.size) cgramBytes[i].toByte() else 0 }
    }
    val displayLines = if (lines.isEmpty()) listOf("") else lines
    val rawData = if (rawBytes.isNotEmpty()) rawBytes
        else displayLines.map { line -> line.map { it.code } }

    val bezel = 3.dp
    val pad = 5.dp
    val cellW = 10.dp
    val cellH = 14.dp
    val charGap = 3.dp
    val lineGap = 3.dp

    val lineCount = displayLines.size
    val gridW = cellW * cols + charGap * (cols - 1)
    val gridH = cellH * lineCount + lineGap * (lineCount - 1)
    val innerW = gridW + pad * 2
    val innerH = gridH + pad * 2

    Canvas(
        modifier = modifier
            .width(innerW + bezel * 2)
            .height(innerH + bezel * 2),
    ) {
        val totalW = size.width
        val totalH = size.height
        val inset = bezel.toPx()

        drawBezel(totalW, totalH, inset)
        drawInnerBg(inset, totalW, totalH)
        drawCharGrid(
            rawData, cols, cgramArray, inset, pad, cellW, cellH, charGap, lineGap
        )
    }
}

private fun DrawScope.drawBezel(totalW: Float, totalH: Float, inset: Float) {
    drawRoundRect(
        color = Color(0xFF444444),
        topLeft = Offset.Zero,
        size = Size(totalW, totalH),
        cornerRadius = CornerRadius(4.dp.toPx()),
    )
    drawRoundRect(
        color = Color(0xFF333333),
        topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
        size = Size(totalW - 2.dp.toPx(), totalH - 2.dp.toPx()),
        cornerRadius = CornerRadius(3.dp.toPx()),
    )
}

private fun DrawScope.drawInnerBg(inset: Float, totalW: Float, totalH: Float) {
    drawRoundRect(
        color = Color(0xFF1A3A1A),
        topLeft = Offset(inset, inset),
        size = Size(totalW - inset * 2, totalH - inset * 2),
        cornerRadius = CornerRadius(2.dp.toPx()),
    )
}

private fun DrawScope.drawCharGrid(
    rawData: List<List<Int>>,
    cols: Int,
    cgram: ByteArray,
    inset: Float,
    pad: Dp,
    cellW: Dp,
    cellH: Dp,
    charGap: Dp,
    lineGap: Dp,
) {
    val cellWPx = cellW.toPx()
    val cellHPx = cellH.toPx()
    val charGapPx = charGap.toPx()
    val lineGapPx = lineGap.toPx()
    val padPx = pad.toPx()
    val dotR = 1.05.dp.toPx()

    val dotOn = Color(0xFFD4F4D4)

    for ((lineIdx, lineData) in rawData.withIndex()) {
        for (colIdx in 0 until cols) {
            val charCode = lineData.getOrElse(colIdx) { 0x20 }
            val pattern = Hd44780Font.getCharPattern(charCode, cgram)

            val baseX = padPx + inset + colIdx * (cellWPx + charGapPx)
            val baseY = padPx + inset + lineIdx * (cellHPx + lineGapPx)

            for (row in 0..7) {
                val rowBits = pattern.getOrElse(row) { 0 }.toInt() and 0x1F
                for (col in 0..4) {
                    if ((rowBits shr (4 - col)) and 1 != 0) {
                        drawCircle(
                            color = dotOn,
                            radius = dotR,
                            center = Offset(
                                baseX + col * cellWPx / 5 + cellWPx / 10,
                                baseY + row * cellHPx / 8 + cellHPx / 16,
                            ),
                        )
                    }
                }
            }
        }
    }
}
