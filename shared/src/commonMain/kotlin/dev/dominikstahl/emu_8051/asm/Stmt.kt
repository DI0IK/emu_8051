package dev.dominikstahl.emu_8051.asm

sealed class Stmt {
    data class Org(val expr: Expr) : Stmt()
    data object End : Stmt()
    data class Equ(val symbol: String, val expr: Expr) : Stmt()
    data class Data(val symbol: String, val expr: Expr) : Stmt()
    data class Bit(val symbol: String, val expr: Expr) : Stmt()
    data class Code(val symbol: String, val expr: Expr) : Stmt()
    data class Db(val values: List<DbItem>, val label: String? = null) : Stmt()
    data class Dw(val values: List<Expr>, val label: String? = null) : Stmt()
    data class Ds(val count: Expr, val label: String? = null) : Stmt()
    data class Instruction(val label: String?, val mnemonic: String, val operands: List<Expr>, val line: Int) : Stmt()
    data class Label(val name: String) : Stmt()
    data object Blank : Stmt()
    data class ErrorMsg(val message: String, val line: Int) : Stmt()
}

sealed class DbItem {
    data class ExprValue(val expr: Expr) : DbItem()
    data class StringValue(val string: String) : DbItem()
}

fun parseStatements(tokens: List<Token>): List<Stmt> {
    val stmts = mutableListOf<Stmt>()
    var pos = 0

    while (pos < tokens.size) {
        while (pos < tokens.size && tokens[pos].type == TokenType.NEWLINE) pos++
        if (pos >= tokens.size || tokens[pos].type == TokenType.EOF) break

        val stmt: Stmt
        val result = parseLine(tokens, pos)
        stmt = result.value
        pos = result.nextIndex

        stmts.add(stmt)
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
    if (pos + 1 < tokens.size &&
        tokens[pos].type == TokenType.SYMBOL &&
        tokens[pos + 1].type == TokenType.COLON
    ) {
        label = tokens[pos].text
        pos += 2
    }

    while (pos < tokens.size && tokens[pos].type == TokenType.NEWLINE) {
        if (label != null) return ParseResult(Stmt.Label(label), pos)
        pos++
    }
    if (pos >= tokens.size || tokens[pos].type == TokenType.EOF) {
        return ParseResult(if (label != null) Stmt.Label(label) else Stmt.Blank, pos)
    }

    val stmt: Stmt

    if (tokens[pos].type == TokenType.SYMBOL && pos + 1 < tokens.size &&
        tokens[pos + 1].type in listOf(TokenType.EQU, TokenType.DATA, TokenType.BIT, TokenType.CODE)
    ) {
        val symbolName = tokens[pos].text
        val directiveType = tokens[pos + 1].type
        val lineNum = tokens[pos].line
        pos += 2
        val (expr, newPos) = parseExpr(tokens, pos)
        stmt = when (directiveType) {
            TokenType.EQU -> Stmt.Equ(symbolName, expr)
            TokenType.DATA -> Stmt.Data(symbolName, expr)
            TokenType.BIT -> Stmt.Bit(symbolName, expr)
            TokenType.CODE -> Stmt.Code(symbolName, expr)
            else -> Stmt.ErrorMsg("Unknown directive", lineNum)
        }
        pos = newPos
    } else when (tokens[pos].type) {
        TokenType.END -> { stmt = Stmt.End; pos++ }
        TokenType.ORG -> {
            pos++
            val (expr, newPos) = parseExpr(tokens, pos)
            stmt = Stmt.Org(expr); pos = newPos
        }
        TokenType.EQU -> {
            val symbol = label ?: return ParseResult(
                Stmt.ErrorMsg("EQU without symbol", tokens[pos].line), pos + 1
            )
            label = null
            pos++
            val (expr, newPos) = parseExpr(tokens, pos)
            stmt = Stmt.Equ(symbol, expr); pos = newPos
        }
        TokenType.DATA -> {
            val symbol = label ?: return ParseResult(
                Stmt.ErrorMsg("DATA without symbol", tokens[pos].line), pos + 1
            )
            label = null
            pos++
            val (expr, newPos) = parseExpr(tokens, pos)
            stmt = Stmt.Data(symbol, expr); pos = newPos
        }
        TokenType.BIT -> {
            val symbol = label ?: return ParseResult(
                Stmt.ErrorMsg("BIT without symbol", tokens[pos].line), pos + 1
            )
            label = null
            pos++
            val (expr, newPos) = parseExpr(tokens, pos)
            stmt = Stmt.Bit(symbol, expr); pos = newPos
        }
        TokenType.CODE -> {
            val symbol = label ?: return ParseResult(
                Stmt.ErrorMsg("CODE without symbol", tokens[pos].line), pos + 1
            )
            label = null
            pos++
            val (expr, newPos) = parseExpr(tokens, pos)
            stmt = Stmt.Code(symbol, expr); pos = newPos
        }
        TokenType.DB -> {
            pos++
            val items = mutableListOf<DbItem>()
            while (pos < tokens.size && tokens[pos].type !in listOf(TokenType.NEWLINE, TokenType.EOF)) {
                when (tokens[pos].type) {
                    TokenType.STRING -> {
                        items.add(DbItem.StringValue(tokens[pos].text))
                        pos++
                    }
                    TokenType.COMMA -> pos++
                    else -> {
                        val (expr, newPos) = parseExpr(tokens, pos)
                        items.add(DbItem.ExprValue(expr)); pos = newPos
                    }
                }
            }
            stmt = Stmt.Db(items, label)
        }
        TokenType.DW -> {
            pos++
            val values = mutableListOf<Expr>()
            while (pos < tokens.size && tokens[pos].type !in listOf(TokenType.NEWLINE, TokenType.EOF)) {
                when (tokens[pos].type) {
                    TokenType.COMMA -> pos++
                    TokenType.STRING -> {
                        val s = tokens[pos].text
                        for (ch in s) {
                            values.add(Expr.Number(ch.code))
                        }
                        pos++
                    }
                    else -> {
                        val (expr, newPos) = parseExpr(tokens, pos)
                        values.add(expr); pos = newPos
                    }
                }
            }
            stmt = Stmt.Dw(values, label)
        }
        TokenType.DS -> {
            pos++
            val (expr, newPos) = parseExpr(tokens, pos)
            stmt = Stmt.Ds(expr, label); pos = newPos
        }
        TokenType.SYMBOL -> {
            val mnemonic = tokens[pos].text
            val lineNum = tokens[pos].line
            pos++
            val operands = mutableListOf<Expr>()
            while (pos < tokens.size && tokens[pos].type !in listOf(TokenType.NEWLINE, TokenType.EOF)) {
                when (tokens[pos].type) {
                    TokenType.COMMA -> pos++
                    else -> {
                        val (expr, newPos) = parseExpr(tokens, pos)
                        operands.add(expr); pos = newPos
                    }
                }
            }
            stmt = Stmt.Instruction(label, mnemonic, operands, lineNum)
        }
        else -> {
            stmt = Stmt.ErrorMsg("Unexpected token: ${tokens[pos].type}", tokens[pos].line)
            pos++
        }
    }

    return ParseResult(stmt, pos)
}
