package dev.dominikstahl.emu_8051.asm

sealed class Stmt {
    data class Org(val expr: Expr, val line: Int) : Stmt()
    data class End(val line: Int) : Stmt()
    data class Equ(val symbol: String, val expr: Expr, val line: Int) : Stmt()
    data class Data(val symbol: String, val expr: Expr, val line: Int) : Stmt()
    data class Bit(val symbol: String, val expr: Expr, val line: Int) : Stmt()
    data class Code(val symbol: String, val expr: Expr, val line: Int) : Stmt()
    data class Db(val values: List<DbItem>, val label: String? = null, val line: Int) : Stmt()
    data class Dw(val values: List<Expr>, val label: String? = null, val line: Int) : Stmt()
    data class Ds(val count: Expr, val label: String? = null, val line: Int) : Stmt()
    data class Instruction(val label: String?, val mnemonic: String, val operands: List<Expr>, val line: Int) : Stmt()
    data class Label(val name: String, val line: Int) : Stmt()
    data object Blank : Stmt()
    data class ErrorMsg(val message: String, val line: Int) : Stmt()
}

sealed class DbItem {
    data class ExprValue(val expr: Expr) : DbItem()
    data class StringValue(val string: String) : DbItem()
}

private fun isEndOfLine(type: TokenType): Boolean =
    type == TokenType.NEWLINE || type == TokenType.EOF

private fun skipToEndOfLine(tokens: List<Token>, start: Int): Int {
    var pos = start
    while (pos < tokens.size && !isEndOfLine(tokens[pos].type)) pos++
    return pos
}

private fun isSymbolDefineDirective(type: TokenType): Boolean = when (type) {
    TokenType.EQU, TokenType.DATA, TokenType.BIT, TokenType.CODE -> true
    else -> false
}

private fun isDataDirective(type: TokenType): Boolean = when (type) {
    TokenType.DB, TokenType.DW, TokenType.DS -> true
    else -> false
}

fun parseStatements(tokens: List<Token>): List<Stmt> {
    val stmts = mutableListOf<Stmt>()
    var pos = 0

    while (pos < tokens.size) {
        while (pos < tokens.size && tokens[pos].type == TokenType.NEWLINE) pos++
        if (pos >= tokens.size || tokens[pos].type == TokenType.EOF) break

        val result = parseLine(tokens, pos)
        stmts.add(result.value)
        pos = result.nextIndex
    }
    return stmts
}

fun parseLine(tokens: List<Token>, start: Int): ParseResult<Stmt> {
    var pos = start

    while (pos < tokens.size && tokens[pos].type == TokenType.NEWLINE) pos++
    if (pos >= tokens.size || tokens[pos].type == TokenType.EOF) {
        return ParseResult(Stmt.Blank, pos)
    }

    var label: String? = null
    var labelLine = tokens[pos].line
    if (pos + 1 < tokens.size &&
        tokens[pos].type == TokenType.SYMBOL &&
        tokens[pos + 1].type == TokenType.COLON
    ) {
        label = tokens[pos].text
        labelLine = tokens[pos].line
        pos += 2
    }

    if (pos >= tokens.size || isEndOfLine(tokens[pos].type)) {
        return ParseResult(if (label != null) Stmt.Label(label, labelLine) else Stmt.Blank, pos)
    }

    if (tokens[pos].type == TokenType.ERROR) {
        val tok = tokens[pos]
        return ParseResult(Stmt.ErrorMsg(tok.text, tok.line), skipToEndOfLine(tokens, pos))
    }

    // Label without colon before EQU/DATA/BIT/CODE or DB/DW/DS
    if (label == null && tokens[pos].type == TokenType.SYMBOL && pos + 1 < tokens.size) {
        val nextType = tokens[pos + 1].type
        if (isSymbolDefineDirective(nextType)) {
            val symbolName = tokens[pos].text
            val lineNum = tokens[pos].line
            pos += 2
            return try {
                val (expr, newPos) = parseExpr(tokens, pos)
                if (newPos < tokens.size && !isEndOfLine(tokens[newPos].type)) {
                    ParseResult(
                        Stmt.ErrorMsg("Unexpected token '${tokens[newPos].text}'", tokens[newPos].line),
                        skipToEndOfLine(tokens, newPos),
                    )
                } else {
                    val stmt = when (nextType) {
                        TokenType.EQU -> Stmt.Equ(symbolName, expr, lineNum)
                        TokenType.DATA -> Stmt.Data(symbolName, expr, lineNum)
                        TokenType.BIT -> Stmt.Bit(symbolName, expr, lineNum)
                        TokenType.CODE -> Stmt.Code(symbolName, expr, lineNum)
                        else -> Stmt.ErrorMsg("Unknown directive", lineNum)
                    }
                    ParseResult(stmt, newPos)
                }
            } catch (e: AssemblerException) {
                ParseResult(Stmt.ErrorMsg(e.message ?: "Syntax error", lineNum), skipToEndOfLine(tokens, pos))
            }
        } else if (isDataDirective(nextType)) {
            label = tokens[pos].text
            labelLine = tokens[pos].line
            pos++
        }
    }

    val curTok = tokens[pos]
    val lineNum = curTok.line

    return try {
        when (curTok.type) {
            TokenType.END -> {
                val nextPos = pos + 1
                if (nextPos < tokens.size && !isEndOfLine(tokens[nextPos].type)) {
                    ParseResult(
                        Stmt.ErrorMsg("Unexpected token '${tokens[nextPos].text}' after END", tokens[nextPos].line),
                        skipToEndOfLine(tokens, nextPos),
                    )
                } else {
                    ParseResult(Stmt.End(lineNum), nextPos)
                }
            }
            TokenType.ORG -> {
                pos++
                val (expr, newPos) = parseExpr(tokens, pos)
                if (newPos < tokens.size && !isEndOfLine(tokens[newPos].type)) {
                    ParseResult(
                        Stmt.ErrorMsg("Unexpected token '${tokens[newPos].text}'", tokens[newPos].line),
                        skipToEndOfLine(tokens, newPos),
                    )
                } else {
                    ParseResult(Stmt.Org(expr, lineNum), newPos)
                }
            }
            TokenType.EQU, TokenType.DATA, TokenType.BIT, TokenType.CODE -> {
                val symbol = label ?: return ParseResult(
                    Stmt.ErrorMsg("${curTok.type.name} without symbol", lineNum),
                    skipToEndOfLine(tokens, pos + 1),
                )
                pos++
                val (expr, newPos) = parseExpr(tokens, pos)
                if (newPos < tokens.size && !isEndOfLine(tokens[newPos].type)) {
                    ParseResult(
                        Stmt.ErrorMsg("Unexpected token '${tokens[newPos].text}'", tokens[newPos].line),
                        skipToEndOfLine(tokens, newPos),
                    )
                } else {
                    val stmt = when (curTok.type) {
                        TokenType.EQU -> Stmt.Equ(symbol, expr, lineNum)
                        TokenType.DATA -> Stmt.Data(symbol, expr, lineNum)
                        TokenType.BIT -> Stmt.Bit(symbol, expr, lineNum)
                        else -> Stmt.Code(symbol, expr, lineNum)
                    }
                    ParseResult(stmt, newPos)
                }
            }
            TokenType.DB -> {
                pos++
                val items = mutableListOf<DbItem>()
                if (pos >= tokens.size || isEndOfLine(tokens[pos].type)) {
                    return ParseResult(Stmt.ErrorMsg("DB expects at least one value", lineNum), pos)
                }
                while (true) {
                    val tok = tokens.getOrNull(pos) ?: throw AssemblerException("Expected value after ','")
                    if (isEndOfLine(tok.type)) throw AssemblerException("Expected value after ','")
                    if (tok.type == TokenType.STRING) {
                        val nextType = tokens.getOrNull(pos + 1)?.type
                        if (tok.text.length != 1 || nextType == null || nextType == TokenType.COMMA || isEndOfLine(nextType)) {
                            items.add(DbItem.StringValue(tok.text))
                            pos++
                        } else {
                            val (expr, newPos) = parseExpr(tokens, pos)
                            items.add(DbItem.ExprValue(expr))
                            pos = newPos
                        }
                    } else {
                        val (expr, newPos) = parseExpr(tokens, pos)
                        items.add(DbItem.ExprValue(expr))
                        pos = newPos
                    }
                    if (pos >= tokens.size || isEndOfLine(tokens[pos].type)) break
                    if (tokens[pos].type != TokenType.COMMA) {
                        throw AssemblerException("Expected ',' between DB values")
                    }
                    pos++
                }
                ParseResult(Stmt.Db(items, label, lineNum), pos)
            }
            TokenType.DW -> {
                pos++
                val values = mutableListOf<Expr>()
                if (pos >= tokens.size || isEndOfLine(tokens[pos].type)) {
                    return ParseResult(Stmt.ErrorMsg("DW expects at least one value", lineNum), pos)
                }
                while (true) {
                    val tok = tokens.getOrNull(pos) ?: throw AssemblerException("Expected value after ','")
                    if (isEndOfLine(tok.type)) throw AssemblerException("Expected value after ','")
                    if (tok.type == TokenType.STRING) {
                        val nextType = tokens.getOrNull(pos + 1)?.type
                        if (tok.text.length != 1 || nextType == null || nextType == TokenType.COMMA || isEndOfLine(nextType)) {
                            for (ch in tok.text) {
                                values.add(Expr.Number(ch.code))
                            }
                            pos++
                        } else {
                            val (expr, newPos) = parseExpr(tokens, pos)
                            values.add(expr)
                            pos = newPos
                        }
                    } else {
                        val (expr, newPos) = parseExpr(tokens, pos)
                        values.add(expr)
                        pos = newPos
                    }
                    if (pos >= tokens.size || isEndOfLine(tokens[pos].type)) break
                    if (tokens[pos].type != TokenType.COMMA) {
                        throw AssemblerException("Expected ',' between DW values")
                    }
                    pos++
                }
                ParseResult(Stmt.Dw(values, label, lineNum), pos)
            }
            TokenType.DS -> {
                pos++
                val (expr, newPos) = parseExpr(tokens, pos)
                if (newPos < tokens.size && !isEndOfLine(tokens[newPos].type)) {
                    ParseResult(
                        Stmt.ErrorMsg("Unexpected token '${tokens[newPos].text}'", tokens[newPos].line),
                        skipToEndOfLine(tokens, newPos),
                    )
                } else {
                    ParseResult(Stmt.Ds(expr, label, lineNum), newPos)
                }
            }
            TokenType.SYMBOL -> {
                val mnemonic = curTok.text
                pos++
                val operands = mutableListOf<Expr>()
                if (pos < tokens.size && !isEndOfLine(tokens[pos].type)) {
                    while (true) {
                        if (pos >= tokens.size || isEndOfLine(tokens[pos].type)) {
                            throw AssemblerException("Expected operand after ','")
                        }
                        val (expr, newPos) = parseExpr(tokens, pos)
                        operands.add(expr)
                        pos = newPos
                        if (pos >= tokens.size || isEndOfLine(tokens[pos].type)) break
                        if (tokens[pos].type != TokenType.COMMA) {
                            throw AssemblerException("Expected ',' between operands")
                        }
                        pos++
                    }
                }
                ParseResult(Stmt.Instruction(label, mnemonic, operands, lineNum), pos)
            }
            else -> {
                ParseResult(
                    Stmt.ErrorMsg("Unexpected token: '${curTok.text.ifEmpty { curTok.type.name }}'", lineNum),
                    skipToEndOfLine(tokens, pos + 1),
                )
            }
        }
    } catch (e: AssemblerException) {
        ParseResult(Stmt.ErrorMsg(e.message ?: "Syntax error", lineNum), skipToEndOfLine(tokens, pos))
    }
}
