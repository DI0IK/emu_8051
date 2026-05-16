package dev.dominikstahl.emu_8051.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.graphics.Color
import dev.dominikstahl.emu_8051.asm.TokenType
import dev.dominikstahl.emu_8051.asm.tokenize

private val INSTRUCTION = setOf(
    "ADD", "ADDC", "SUBB", "INC", "DEC", "MUL", "DIV", "DA",
    "ANL", "ORL", "XRL",
    "MOV", "MOVC", "MOVX",
    "SJMP", "AJMP", "LJMP", "JMP",
    "JC", "JNC", "JZ", "JNZ", "JB", "JNB", "JBC",
    "CJNE", "DJNZ",
    "ACALL", "LCALL", "RET", "RETI",
    "PUSH", "POP",
    "CLR", "SETB", "CPL",
    "RR", "RRC", "RL", "RLC", "SWAP",
    "XCH", "XCHD",
    "NOP",
)

private val REGISTER = setOf("A", "B", "C", "DPTR", "AB", "PC") + (0..7).map { "R$it" }

private val colInstruction = Color(0xFF66D9EF)
private val colRegister   = Color(0xFFA6E22E)
private val colSfr        = Color(0xFFF92672)
private val colDirective  = Color(0xFFFD971F)
private val colLabel      = Color(0xFFE6DB74)
private val colNumber     = Color(0xFFAE81FF)
private val colString     = Color(0xFFE6DB74)
private val colComment    = Color(0xFF75715E)
private val colOperator   = Color(0xFF66D9EF)
private val colCurrentLine = Color(0x22FFFFFF)

class AssemblyHighlightTransformation(
    private val currentLine: Int? = null,
    private val breakpoints: Set<Int> = emptySet(),
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val source = text.text
        val tokens = tokenize(source)

        val lineStartOffsets = mutableListOf(0)
        for ((i, ch) in source.withIndex()) {
            if (ch == '\n') lineStartOffsets.add(i + 1)
        }
        lineStartOffsets.add(source.length)

        fun lineOffset(line1based: Int): Int =
            if (line1based in 1..lineStartOffsets.size) lineStartOffsets[line1based - 1] else -1

        val commentSpans = mutableListOf<Pair<Int, Int>>()
        var p = 0
        while (p < source.length) {
            if (source[p] == ';') {
                val start = p
                while (p < source.length && source[p] != '\n') {
                    p++
                }
                commentSpans.add(start to p)
            } else {
                p++
            }
        }

        data class StyleSpan(val start: Int, val end: Int, val style: SpanStyle)
        val styleSpans = mutableListOf<StyleSpan>()

        for (token in tokens) {
            val start = lineOffset(token.line)
            if (start < 0) continue
            val col0 = token.column - 1
            val tokenStart = start + col0
            val tokenEnd = tokenStart + token.text.length
            if (tokenEnd > source.length) continue

            when (token.type) {
                TokenType.SYMBOL -> {
                    val upper = token.text.uppercase()
                    val color = when {
                        upper in INSTRUCTION -> colInstruction
                        upper in REGISTER -> colRegister
                        upper in SFR_SYMBOLS -> colSfr
                        else -> colLabel
                    }
                    styleSpans.add(StyleSpan(tokenStart, tokenEnd, SpanStyle(color = color)))
                }
                TokenType.NUMBER ->
                    styleSpans.add(StyleSpan(tokenStart, tokenEnd, SpanStyle(color = colNumber)))
                TokenType.STRING -> {
                    val contentEnd = tokenStart + 1 + token.text.length
                    val end = if (contentEnd < source.length &&
                        (source[contentEnd] == '\'' || source[contentEnd] == '"'))
                        contentEnd + 1 else contentEnd
                    styleSpans.add(StyleSpan(tokenStart, end, SpanStyle(color = colString)))
                }
                TokenType.ORG, TokenType.END, TokenType.EQU,
                TokenType.DATA, TokenType.BIT, TokenType.CODE,
                TokenType.DB, TokenType.DW, TokenType.DS ->
                    styleSpans.add(StyleSpan(tokenStart, tokenEnd, SpanStyle(color = colDirective)))
                TokenType.DOT ->
                    styleSpans.add(StyleSpan(tokenStart, tokenEnd, SpanStyle(color = colDirective)))
                TokenType.PLUS, TokenType.MINUS, TokenType.STAR,
                TokenType.SLASH, TokenType.KW_MOD, TokenType.KW_NOT,
                TokenType.KW_AND, TokenType.KW_OR, TokenType.KW_XOR,
                TokenType.KW_SHR, TokenType.KW_SHL ->
                    styleSpans.add(StyleSpan(tokenStart, tokenEnd, SpanStyle(color = colOperator)))
                else -> {}
            }
        }

        for ((start, end) in commentSpans) {
            styleSpans.add(StyleSpan(start, end, SpanStyle(color = colComment, fontStyle = FontStyle.Italic)))
        }

        if (currentLine != null) {
            val off = lineOffset(currentLine)
            if (off >= 0) {
                var end = off
                while (end < source.length && source[end] != '\n') end++
                styleSpans.add(StyleSpan(off, end, SpanStyle(background = colCurrentLine)))
            }
        }

        val bpLineStarts = breakpoints.map { lineOffset(it) }.sorted()

        val transformBuilder = AnnotatedString.Builder()
        val origToTrans = mutableListOf<Int>()

        for ((origIdx, ch) in source.withIndex()) {
            if (origIdx in bpLineStarts && (origIdx == 0 || source[origIdx - 1] == '\n')) {
                // transformBuilder.append("\u25CF ")
            }
            origToTrans.add(transformBuilder.length)
            transformBuilder.append(ch)
        }
        origToTrans.add(transformBuilder.length)

        for (span in styleSpans) {
            val adjStart = origToTrans.getOrElse(span.start) { origToTrans.last() }
            val adjEnd = origToTrans.getOrElse(span.end) { origToTrans.last() }
            transformBuilder.addStyle(span.style, adjStart, adjEnd)
        }

        val annotated = transformBuilder.toAnnotatedString()

        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                origToTrans.getOrElse(offset) { origToTrans.last() }

            override fun transformedToOriginal(offset: Int): Int {
                var lo = 0
                var hi = origToTrans.lastIndex
                while (lo < hi) {
                    val mid = (lo + hi + 1) / 2
                    if (origToTrans[mid] <= offset) lo = mid else hi = mid - 1
                }
                return lo.coerceIn(0, source.length)
            }
        }

        return TransformedText(annotated, mapping)
    }
}

private val SFR_SYMBOLS = setOf(
    "P0", "P1", "P2", "P3",
    "SP", "DPL", "DPH", "DPTR",
    "PCON",
    "TCON", "TMOD", "TL0", "TL1", "TH0", "TH1",
    "AUXR", "CKCON",
    "SCON", "SBUF",
    "IE", "IP",
    "T2CON", "T2MOD", "RCAP2L", "RCAP2H", "TL2", "TH2",
    "PSW", "ACC", "B",
    "CY", "AC", "F0", "RS1", "RS0", "OV", "P",
    "EA", "ET2", "ES", "ET1", "EX1", "ET0", "EX0",
    "PT2", "PS", "PT1", "PX1", "PT0", "PX0",
    "TF1", "TR1", "TF0", "TR0", "IE1", "IT1", "IE0", "IT0",
    "SM0", "SM1", "SM2", "REN", "TB8", "RB8", "TI", "RI",
    "TF2", "EXF2", "RCLK", "TCLK", "EXEN2", "TR2", "C_T2", "CP_RL2",
)
