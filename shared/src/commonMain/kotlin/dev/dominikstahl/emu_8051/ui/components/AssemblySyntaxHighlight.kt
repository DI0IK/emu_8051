package dev.dominikstahl.emu_8051.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
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
private val colRegister    = Color(0xFFA6E22E)
private val colSfr         = Color(0xFFF92672)
private val colDirective   = Color(0xFFFD971F)
private val colLabel       = Color(0xFFE6DB74)
private val colNumber      = Color(0xFFAE81FF)
private val colString      = Color(0xFFE6DB74)
private val colComment     = Color(0xFF75715E)
private val colOperator    = Color(0xFF66D9EF)
private val colError       = Color(0xFFEF5350)
private val colCurrentLine = Color(0x22FFFFFF)

data class AssemblyHighlightTransformation(
    private val currentLine: Int? = null,
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val source = text.text
        if (source.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val tokens = tokenize(source)
        val builder = AnnotatedString.Builder(source)

        for (token in tokens) {
            val start = token.startOffset.coerceIn(0, source.length)
            val end = token.endOffset.coerceIn(0, source.length)
            if (start >= end) continue

            val style = when (token.type) {
                TokenType.SYMBOL -> {
                    val upper = token.text.uppercase()
                    val color = when {
                        upper in INSTRUCTION -> colInstruction
                        upper in REGISTER -> colRegister
                        upper in SFR_SYMBOLS -> colSfr
                        else -> colLabel
                    }
                    SpanStyle(color = color)
                }
                TokenType.NUMBER -> SpanStyle(color = colNumber)
                TokenType.STRING -> SpanStyle(color = colString)
                TokenType.ORG, TokenType.END, TokenType.EQU,
                TokenType.DATA, TokenType.BIT, TokenType.CODE,
                TokenType.DB, TokenType.DW, TokenType.DS,
                TokenType.DOT -> SpanStyle(color = colDirective)
                TokenType.PLUS, TokenType.MINUS, TokenType.STAR,
                TokenType.SLASH, TokenType.KW_MOD, TokenType.KW_NOT,
                TokenType.KW_AND, TokenType.KW_OR, TokenType.KW_XOR,
                TokenType.KW_SHR, TokenType.KW_SHL,
                TokenType.KW_HIGH, TokenType.KW_LOW -> SpanStyle(color = colOperator)
                TokenType.ERROR -> SpanStyle(color = colError, background = colError.copy(alpha = 0.18f))
                else -> null
            }
            if (style != null) {
                builder.addStyle(style, start, end)
            }
        }

        // Scan comments while respecting single and double quoted string literals
        var p = 0
        var inQuote: Char? = null
        while (p < source.length) {
            val ch = source[p]
            if (inQuote != null) {
                if (ch == '\\' && p + 1 < source.length && source[p + 1] != '\n') {
                    p += 2
                    continue
                }
                if (ch == inQuote || ch == '\n') {
                    inQuote = null
                }
                p++
            } else {
                when (ch) {
                    '\'', '"' -> { inQuote = ch; p++ }
                    ';' -> {
                        val cStart = p
                        while (p < source.length && source[p] != '\n' && source[p] != '\r') p++
                        if (cStart < p) {
                            builder.addStyle(
                                SpanStyle(color = colComment, fontStyle = FontStyle.Italic),
                                cStart,
                                p,
                            )
                        }
                    }
                    else -> p++
                }
            }
        }

        if (currentLine != null && currentLine >= 1) {
            var line = 1
            var lineStart = 0
            var idx = 0
            while (idx < source.length && line < currentLine) {
                if (source[idx] == '\n') {
                    line++
                    lineStart = idx + 1
                }
                idx++
            }
            if (line == currentLine) {
                var lineEnd = lineStart
                while (lineEnd < source.length && source[lineEnd] != '\n' && source[lineEnd] != '\r') {
                    lineEnd++
                }
                if (lineStart < lineEnd) {
                    builder.addStyle(SpanStyle(background = colCurrentLine), lineStart, lineEnd)
                }
            }
        }

        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
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
    "CY", "AC", "F0", "F1", "RS1", "RS0", "OV", "P",
    "EA", "ET2", "ES", "ET1", "EX1", "ET0", "EX0",
    "PT2", "PS", "PT1", "PX1", "PT0", "PX0",
    "TF1", "TR1", "TF0", "TR0", "IE1", "IT1", "IE0", "IT0",
    "SM0", "SM1", "SM2", "REN", "TB8", "RB8", "TI", "RI",
    "TF2", "EXF2", "RCLK", "TCLK", "EXEN2", "TR2", "CT2", "CPRL2",
    "RXD", "TXD", "INT0", "INT1", "T0", "T1", "WR", "RD", "T2", "T2EX",
)
