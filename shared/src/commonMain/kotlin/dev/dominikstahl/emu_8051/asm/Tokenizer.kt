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
    val column: Int = 0
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

    fun emit(type: TokenType, text: String = "", intValue: Int = 0) {
        tokens.add(Token(type, text, intValue, line, col - text.length.coerceAtLeast(0)))
    }

    fun error(msg: String) {
        tokens.add(Token(TokenType.ERROR, msg, 0, line, col))
    }

    while (pos < s.length) {
        val c = s[pos]

        when {
            c == '\r' && pos + 1 < s.length && s[pos + 1] == '\n' -> {
                emit(TokenType.NEWLINE)
                pos += 2; line++; col = 1
            }
            c == '\n' -> {
                emit(TokenType.NEWLINE)
                pos++; line++; col = 1
            }
            c == ';' -> {
                while (pos < s.length && s[pos] != '\n') pos++
            }
            c.isWhitespace() -> { pos++; col++ }

            c == ',' -> { emit(TokenType.COMMA, ","); pos++; col++ }
            c == ':' -> { emit(TokenType.COLON, ":"); pos++; col++ }
            c == '@' -> { emit(TokenType.AT, "@"); pos++; col++ }
            c == '#' -> { emit(TokenType.HASH, "#"); pos++; col++ }
            c == '.' -> { emit(TokenType.DOT, "."); pos++; col++ }
            c == '/' -> { emit(TokenType.SLASH, "/"); pos++; col++ }
            c == '+' -> { emit(TokenType.PLUS, "+"); pos++; col++ }
            c == '-' -> { emit(TokenType.MINUS, "-"); pos++; col++ }
            c == '*' -> { emit(TokenType.STAR, "*"); pos++; col++ }
            c == '(' -> { emit(TokenType.LPAREN, "("); pos++; col++ }
            c == ')' -> { emit(TokenType.RPAREN, ")"); pos++; col++ }
            c == '$' -> { emit(TokenType.DOLLAR, "$"); pos++; col++ }

            c == '\'' || c == '"' -> {
                val quote = c
                val startCol = col
                pos++; col++
                val sb = StringBuilder()
                while (pos < s.length && s[pos] != quote) {
                    if (s[pos] == '\\' && pos + 1 < s.length) {
                        pos++; col++
                        when (s[pos]) {
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            '\'' -> sb.append('\'')
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            else -> { sb.append(s[pos]) }
                        }
                    } else {
                        sb.append(s[pos])
                    }
                    pos++; col++
                }
                if (pos < s.length) { pos++; col++ }
                tokens.add(Token(TokenType.STRING, sb.toString(), 0, line, startCol))
            }

            c == '0' && pos + 1 < s.length && (s[pos + 1] == 'x' || s[pos + 1] == 'X') -> {
                val start = pos
                pos += 2; col += 2
                while (pos < s.length && s[pos].digitToIntOrNull(16) != null) { pos++; col++ }
                val hexStr = s.substring(start + 2, pos)
                if (hexStr.isNotEmpty()) {
                    emit(TokenType.NUMBER, s.substring(start, pos), hexStr.toInt(16))
                } else {
                    error("Invalid hex number"); pos = start + 2; col = start + 3
                }
            }

            c.isDigit() -> {
                val start = pos
                while (pos < s.length && s[pos].digitToIntOrNull(16) != null) pos++
                col += (pos - start)
                if (pos < s.length && (s[pos] == 'h' || s[pos] == 'H')) {
                    val digits = s.substring(start, pos)
                    if (digits.all { it.digitToIntOrNull(16) != null }) {
                        emit(TokenType.NUMBER, s.substring(start, pos + 1), digits.toInt(16))
                        pos++; col++
                    } else {
                        error("Invalid hex digits")
                    }
                } else if (pos < s.length && (s[pos] == 'b' || s[pos] == 'B')) {
                    val digits = s.substring(start, pos)
                    if (digits.all { it == '0' || it == '1' }) {
                        emit(TokenType.NUMBER, s.substring(start, pos + 1), digits.toInt(2))
                        pos++; col++
                    } else {
                        error("Invalid binary digits")
                    }
                } else {
                    val numStr = s.substring(start, pos)
                    if (numStr.all { it.isDigit() }) {
                        emit(TokenType.NUMBER, numStr, numStr.toInt(10))
                    } else {
                        error("Invalid number: $numStr (missing 'h' suffix?)")
                    }
                }
            }

            c.isLetter() || c == '_' || c == '.' -> {
                val start = pos
                while (pos < s.length && (s[pos].isLetterOrDigit() || s[pos] == '_')) pos++
                col += (pos - start)
                val word = s.substring(start, pos)

                if (word.length > 1 && (word.last() == 'h' || word.last() == 'H')) {
                    val digits = word.dropLast(1)
                    if (digits.all { it.digitToIntOrNull(16) != null }) {
                        emit(TokenType.NUMBER, word, digits.toInt(16))
                        continue
                    }
                }
                if (word.length > 1 && (word.last() == 'b' || word.last() == 'B')) {
                    val digits = word.dropLast(1)
                    if (digits.all { it == '0' || it == '1' }) {
                        emit(TokenType.NUMBER, word, digits.toInt(2))
                        continue
                    }
                }

                val upper = word.uppercase()
                directiveKeywords[upper]?.let { emit(it, word); continue }
                operatorKeywords[upper]?.let { emit(it, word); continue }
                emit(TokenType.SYMBOL, word)
            }

            c == '\r' -> { pos++; line++; col = 1 }
            else -> { error("Unexpected character '$c'"); pos++; col++ }
        }
    }

    emit(TokenType.EOF)
    return tokens
}
