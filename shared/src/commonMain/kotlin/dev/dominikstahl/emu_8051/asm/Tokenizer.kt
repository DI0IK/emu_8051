package dev.dominikstahl.emu_8051.asm

enum class TokenType {
    NUMBER, SYMBOL, STRING,
    COMMA, COLON, AT, HASH, DOT, SLASH,
    PLUS, MINUS, STAR,
    LPAREN, RPAREN,
    DOLLAR,
    ORG, END, EQU, DATA, BIT, CODE, DB, DW, DS,
    KW_MOD, KW_NOT, KW_AND, KW_OR, KW_XOR, KW_SHR, KW_SHL, KW_HIGH, KW_LOW,
    NEWLINE, EOF, ERROR
}

data class Token(
    val type: TokenType,
    val text: String = "",
    val intValue: Int = 0,
    val line: Int = 0,
    val column: Int = 0,
    val startOffset: Int = 0,
    val endOffset: Int = 0,
)

val directiveKeywords = mapOf(
    "ORG" to TokenType.ORG, "END" to TokenType.END,
    "EQU" to TokenType.EQU, "DATA" to TokenType.DATA,
    "BIT" to TokenType.BIT, "CODE" to TokenType.CODE,
    "DB" to TokenType.DB, "DW" to TokenType.DW, "DS" to TokenType.DS
)

val operatorKeywords = mapOf(
    "MOD" to TokenType.KW_MOD, "NOT" to TokenType.KW_NOT,
    "AND" to TokenType.KW_AND, "OR" to TokenType.KW_OR,
    "XOR" to TokenType.KW_XOR, "SHR" to TokenType.KW_SHR,
    "SHL" to TokenType.KW_SHL, "HIGH" to TokenType.KW_HIGH,
    "LOW" to TokenType.KW_LOW
)

fun tokenize(source: String): List<Token> {
    val tokens = mutableListOf<Token>()
    var pos = 0
    var line = 1
    var col = 1
    val s = source

    fun emit(
        type: TokenType,
        text: String = "",
        intValue: Int = 0,
        startPos: Int,
        endPos: Int,
        startCol: Int,
    ) {
        tokens.add(Token(type, text, intValue, line, startCol, startPos, endPos))
    }

    fun emitError(msg: String, startPos: Int, endPos: Int, startCol: Int) {
        tokens.add(Token(TokenType.ERROR, msg, 0, line, startCol, startPos, endPos))
    }

    while (pos < s.length) {
        val c = s[pos]

        when {
            c == '\r' && pos + 1 < s.length && s[pos + 1] == '\n' -> {
                emit(TokenType.NEWLINE, startPos = pos, endPos = pos + 2, startCol = col)
                pos += 2; line++; col = 1
            }
            c == '\n' || c == '\r' -> {
                emit(TokenType.NEWLINE, startPos = pos, endPos = pos + 1, startCol = col)
                pos++; line++; col = 1
            }
            c == ';' -> {
                while (pos < s.length && s[pos] != '\n' && s[pos] != '\r') pos++
            }
            c.isWhitespace() -> { pos++; col++ }

            c == ',' -> { emit(TokenType.COMMA, ",", startPos = pos, endPos = pos + 1, startCol = col); pos++; col++ }
            c == ':' -> { emit(TokenType.COLON, ":", startPos = pos, endPos = pos + 1, startCol = col); pos++; col++ }
            c == '@' -> { emit(TokenType.AT, "@", startPos = pos, endPos = pos + 1, startCol = col); pos++; col++ }
            c == '#' -> { emit(TokenType.HASH, "#", startPos = pos, endPos = pos + 1, startCol = col); pos++; col++ }
            c == '.' -> { emit(TokenType.DOT, ".", startPos = pos, endPos = pos + 1, startCol = col); pos++; col++ }
            c == '/' -> { emit(TokenType.SLASH, "/", startPos = pos, endPos = pos + 1, startCol = col); pos++; col++ }
            c == '+' -> { emit(TokenType.PLUS, "+", startPos = pos, endPos = pos + 1, startCol = col); pos++; col++ }
            c == '-' -> { emit(TokenType.MINUS, "-", startPos = pos, endPos = pos + 1, startCol = col); pos++; col++ }
            c == '*' -> { emit(TokenType.STAR, "*", startPos = pos, endPos = pos + 1, startCol = col); pos++; col++ }
            c == '(' -> { emit(TokenType.LPAREN, "(", startPos = pos, endPos = pos + 1, startCol = col); pos++; col++ }
            c == ')' -> { emit(TokenType.RPAREN, ")", startPos = pos, endPos = pos + 1, startCol = col); pos++; col++ }
            c == '$' -> { emit(TokenType.DOLLAR, "$", startPos = pos, endPos = pos + 1, startCol = col); pos++; col++ }

            c == '\'' || c == '"' -> {
                val quote = c
                val startPos = pos
                val startCol = col
                pos++; col++
                val sb = StringBuilder()
                while (pos < s.length && s[pos] != quote && s[pos] != '\n' && s[pos] != '\r') {
                    if (s[pos] == '\\' && pos + 1 < s.length && s[pos + 1] != '\n' && s[pos + 1] != '\r') {
                        pos++; col++
                        when (s[pos]) {
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            '0' -> sb.append('\u0000')
                            '\'' -> sb.append('\'')
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            else -> sb.append(s[pos])
                        }
                    } else {
                        sb.append(s[pos])
                    }
                    pos++; col++
                }
                if (pos < s.length && s[pos] == quote) {
                    pos++; col++
                    emit(TokenType.STRING, sb.toString(), startPos = startPos, endPos = pos, startCol = startCol)
                } else {
                    emitError("Unterminated string literal", startPos, pos, startCol)
                }
            }

            c == '0' && pos + 1 < s.length && (s[pos + 1] == 'x' || s[pos + 1] == 'X') -> {
                val startPos = pos
                val startCol = col
                pos += 2; col += 2
                while (pos < s.length && s[pos].isLetterOrDigit()) { pos++; col++ }
                val hexStr = s.substring(startPos + 2, pos)
                val parsed = if (hexStr.isNotEmpty() && hexStr.all { it.digitToIntOrNull(16) != null }) {
                    hexStr.toLongOrNull(16)
                } else null
                if (parsed != null && parsed <= 0xFFFFFFFFL) {
                    emit(TokenType.NUMBER, s.substring(startPos, pos), parsed.toInt(), startPos, pos, startCol)
                } else {
                    emitError("Invalid hex number: ${s.substring(startPos, pos)}", startPos, pos, startCol)
                }
            }

            c.isDigit() -> {
                val startPos = pos
                val startCol = col
                while (pos < s.length && s[pos].isLetterOrDigit()) { pos++; col++ }
                val raw = s.substring(startPos, pos)
                val last = raw.last()
                val (digits, radix) = when {
                    raw.length > 1 && (last == 'h' || last == 'H') -> raw.dropLast(1) to 16
                    raw.length > 2 && raw[0] == '0' && (raw[1] == 'b' || raw[1] == 'B') -> raw.drop(2) to 2
                    raw.length > 1 && (last == 'b' || last == 'B') -> raw.dropLast(1) to 2
                    raw.length > 1 && (last == 'o' || last == 'O' || last == 'q' || last == 'Q') -> raw.dropLast(1) to 8
                    raw.length > 1 && (last == 'd' || last == 'D') -> raw.dropLast(1) to 10
                    else -> raw to 10
                }
                val parsed = if (digits.isNotEmpty() && digits.all { it.digitToIntOrNull(radix) != null }) {
                    digits.toLongOrNull(radix)
                } else null
                if (parsed != null && parsed <= 0xFFFFFFFFL) {
                    emit(TokenType.NUMBER, raw, parsed.toInt(), startPos, pos, startCol)
                } else {
                    emitError("Invalid number: $raw", startPos, pos, startCol)
                }
            }

            c.isLetter() || c == '_' -> {
                val startPos = pos
                val startCol = col
                while (pos < s.length && (s[pos].isLetterOrDigit() || s[pos] == '_')) { pos++; col++ }
                val word = s.substring(startPos, pos)
                val upper = word.uppercase()
                val kwType = directiveKeywords[upper] ?: operatorKeywords[upper]
                if (kwType != null) {
                    emit(kwType, word, startPos = startPos, endPos = pos, startCol = startCol)
                } else {
                    emit(TokenType.SYMBOL, word, startPos = startPos, endPos = pos, startCol = startCol)
                }
            }

            else -> {
                val startPos = pos
                val startCol = col
                pos++; col++
                emitError("Unexpected character '$c'", startPos, pos, startCol)
            }
        }
    }

    emit(TokenType.EOF, startPos = s.length, endPos = s.length, startCol = col)
    return tokens
}
