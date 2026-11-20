package dev.dominikstahl.emu_8051.asm

sealed class Expr {
    data class Number(val value: Int) : Expr()
    data class Symbol(val name: String) : Expr()
    data object PCLocation : Expr()
    data class Unary(val op: UnaryOp, val expr: Expr) : Expr()
    data class Binary(val left: Expr, val op: BinaryOp, val right: Expr) : Expr()
    data class Immediate(val expr: Expr) : Expr()
    data class AtReg(val index: Int) : Expr()
    data object AtDPTR : Expr()
    data object AtAPlusDPTR : Expr()
    data object AtAPlusPC : Expr()
    data class NotBit(val expr: Expr) : Expr()
}

enum class UnaryOp { PLUS, MINUS, NOT, HIGH, LOW }
enum class BinaryOp { PLUS, MINUS, TIMES, DIV, MOD, AND, OR, XOR, SHR, SHL, DOT }

data class ParseResult<T>(val value: T, val nextIndex: Int)

fun tokenToBinaryOp(type: TokenType): BinaryOp = when (type) {
    TokenType.PLUS -> BinaryOp.PLUS
    TokenType.MINUS -> BinaryOp.MINUS
    TokenType.STAR -> BinaryOp.TIMES
    TokenType.SLASH -> BinaryOp.DIV
    TokenType.KW_MOD -> BinaryOp.MOD
    TokenType.KW_AND -> BinaryOp.AND
    TokenType.KW_OR -> BinaryOp.OR
    TokenType.KW_XOR -> BinaryOp.XOR
    TokenType.KW_SHR -> BinaryOp.SHR
    TokenType.KW_SHL -> BinaryOp.SHL
    TokenType.DOT -> BinaryOp.DOT
    else -> error("Not a binary op: $type")
}

fun parseExpr(tokens: List<Token>, start: Int): ParseResult<Expr> {
    return parseOr(tokens, start)
}

private fun parseOr(tokens: List<Token>, start: Int): ParseResult<Expr> {
    var (left, pos) = parseAnd(tokens, start)
    while (pos < tokens.size && (tokens[pos].type == TokenType.KW_OR || tokens[pos].type == TokenType.KW_XOR)) {
        val op = tokenToBinaryOp(tokens[pos].type)
        pos++
        val (right, newPos) = parseAnd(tokens, pos)
        left = Expr.Binary(left, op, right)
        pos = newPos
    }
    return ParseResult(left, pos)
}

private fun parseAnd(tokens: List<Token>, start: Int): ParseResult<Expr> {
    var (left, pos) = parseSum(tokens, start)
    while (pos < tokens.size && tokens[pos].type == TokenType.KW_AND) {
        pos++
        val (right, newPos) = parseSum(tokens, pos)
        left = Expr.Binary(left, BinaryOp.AND, right)
        pos = newPos
    }
    return ParseResult(left, pos)
}

private fun parseSum(tokens: List<Token>, start: Int): ParseResult<Expr> {
    var (left, pos) = parseProduct(tokens, start)
    while (pos < tokens.size && (tokens[pos].type == TokenType.PLUS || tokens[pos].type == TokenType.MINUS)) {
        val op = tokenToBinaryOp(tokens[pos].type)
        pos++
        val (right, newPos) = parseProduct(tokens, pos)
        left = Expr.Binary(left, op, right)
        pos = newPos
    }
    return ParseResult(left, pos)
}

private fun parseProduct(tokens: List<Token>, start: Int): ParseResult<Expr> {
    var (left, pos) = parseUnary(tokens, start)
    while (pos < tokens.size) {
        val type = tokens[pos].type
        val isMulOp = when (type) {
            TokenType.STAR, TokenType.SLASH, TokenType.KW_MOD, TokenType.KW_SHL, TokenType.KW_SHR -> true
            else -> false
        }
        if (!isMulOp) break
        val op = tokenToBinaryOp(type)
        pos++
        val (right, newPos) = parseUnary(tokens, pos)
        left = Expr.Binary(left, op, right)
        pos = newPos
    }
    return ParseResult(left, pos)
}

private fun parseUnary(tokens: List<Token>, start: Int): ParseResult<Expr> {
    if (start >= tokens.size) throw AssemblerException("Unexpected end of expression")

    return when (tokens[start].type) {
        TokenType.PLUS -> {
            val (e, p) = parseUnary(tokens, start + 1)
            ParseResult(Expr.Unary(UnaryOp.PLUS, e), p)
        }
        TokenType.MINUS -> {
            val (e, p) = parseUnary(tokens, start + 1)
            ParseResult(Expr.Unary(UnaryOp.MINUS, e), p)
        }
        TokenType.KW_NOT -> {
            val (e, p) = parseUnary(tokens, start + 1)
            ParseResult(Expr.Unary(UnaryOp.NOT, e), p)
        }
        TokenType.KW_HIGH -> {
            val (e, p) = parseUnary(tokens, start + 1)
            ParseResult(Expr.Unary(UnaryOp.HIGH, e), p)
        }
        TokenType.KW_LOW -> {
            val (e, p) = parseUnary(tokens, start + 1)
            ParseResult(Expr.Unary(UnaryOp.LOW, e), p)
        }
        else -> parseDotSelect(tokens, start)
    }
}

private fun parseDotSelect(tokens: List<Token>, start: Int): ParseResult<Expr> {
    var (left, pos) = parsePrimary(tokens, start)
    while (pos < tokens.size && tokens[pos].type == TokenType.DOT) {
        pos++
        if (pos >= tokens.size) throw AssemblerException("Expected bit position after '.'")
        val bitExpr = when (tokens[pos].type) {
            TokenType.NUMBER -> {
                val n = Expr.Number(tokens[pos].intValue)
                pos++
                n
            }
            TokenType.SYMBOL -> {
                val sym = Expr.Symbol(tokens[pos].text)
                pos++
                sym
            }
            else -> throw AssemblerException("Expected bit index after '.'")
        }
        left = Expr.Binary(left, BinaryOp.DOT, bitExpr)
    }
    return ParseResult(left, pos)
}

private fun isExprEnd(type: TokenType): Boolean = when (type) {
    TokenType.NEWLINE, TokenType.EOF, TokenType.COMMA, TokenType.RPAREN -> true
    else -> false
}

private fun parsePrimary(tokens: List<Token>, start: Int): ParseResult<Expr> {
    if (start >= tokens.size || isExprEnd(tokens[start].type)) {
        throw AssemblerException("Expected expression")
    }

    val tok = tokens[start]
    return when (tok.type) {
        TokenType.ERROR -> throw AssemblerException(tok.text)
        TokenType.NUMBER -> ParseResult(Expr.Number(tok.intValue), start + 1)
        TokenType.SYMBOL -> ParseResult(Expr.Symbol(tok.text), start + 1)
        TokenType.DOLLAR -> ParseResult(Expr.PCLocation, start + 1)
        TokenType.STRING -> {
            val s = tok.text
            if (s.length != 1) {
                throw AssemblerException("Character literal must be 1 character, got '$s'")
            }
            ParseResult(Expr.Number(s[0].code), start + 1)
        }
        TokenType.LPAREN -> {
            val (e, pos) = parseExpr(tokens, start + 1)
            if (pos >= tokens.size || tokens[pos].type != TokenType.RPAREN) {
                throw AssemblerException("Missing closing ')'")
            }
            ParseResult(e, pos + 1)
        }
        TokenType.HASH -> {
            val (e, p) = parseExpr(tokens, start + 1)
            ParseResult(Expr.Immediate(e), p)
        }
        TokenType.AT -> {
            if (start + 1 >= tokens.size) throw AssemblerException("Expected register after '@'")
            val target = tokens[start + 1]
            when (target.text.uppercase()) {
                "R0" -> ParseResult(Expr.AtReg(0), start + 2)
                "R1" -> ParseResult(Expr.AtReg(1), start + 2)
                "DPTR" -> ParseResult(Expr.AtDPTR, start + 2)
                "A" -> {
                    if (start + 3 < tokens.size && tokens[start + 2].type == TokenType.PLUS) {
                        when (tokens[start + 3].text.uppercase()) {
                            "DPTR" -> ParseResult(Expr.AtAPlusDPTR, start + 4)
                            "PC" -> ParseResult(Expr.AtAPlusPC, start + 4)
                            else -> throw AssemblerException("Invalid indirect operand '@A+${tokens[start + 3].text}'")
                        }
                    } else {
                        throw AssemblerException("Invalid indirect operand '@A'")
                    }
                }
                else -> throw AssemblerException("Invalid indirect operand '@${target.text}'")
            }
        }
        TokenType.SLASH -> {
            val (e, p) = parseDotSelect(tokens, start + 1)
            ParseResult(Expr.NotBit(e), p)
        }
        else -> throw AssemblerException("Unexpected token: '${tok.text.ifEmpty { tok.type.name }}'")
    }
}

fun evalExpr(expr: Expr, symbols: SymbolTable, pc: Int): Int {
    return when (expr) {
        is Expr.Number -> expr.value
        is Expr.Symbol -> symbols.get(expr.name)
            ?: throw AssemblerException("Undefined symbol: ${expr.name}")
        is Expr.PCLocation -> pc
        is Expr.Unary -> {
            val v = evalExpr(expr.expr, symbols, pc)
            when (expr.op) {
                UnaryOp.PLUS -> v
                UnaryOp.MINUS -> -v
                UnaryOp.NOT -> v.inv()
                UnaryOp.HIGH -> (v ushr 8) and 0xFF
                UnaryOp.LOW -> v and 0xFF
            }
        }
        is Expr.Binary -> {
            val l = evalExpr(expr.left, symbols, pc)
            val r = evalExpr(expr.right, symbols, pc)
            when (expr.op) {
                BinaryOp.PLUS -> l + r
                BinaryOp.MINUS -> l - r
                BinaryOp.TIMES -> l * r
                BinaryOp.DIV -> if (r != 0) l / r else throw AssemblerException("Division by zero")
                BinaryOp.MOD -> if (r != 0) l % r else throw AssemblerException("Modulo by zero")
                BinaryOp.AND -> l and r
                BinaryOp.OR -> l or r
                BinaryOp.XOR -> l xor r
                BinaryOp.SHR -> l ushr r
                BinaryOp.SHL -> l shl r
                BinaryOp.DOT -> {
                    if (r !in 0..7) throw AssemblerException("Bit index out of range (0..7): $r")
                    when {
                        l in 0x20..0x2F -> ((l - 0x20) shl 3) or r
                        l in 0x80..0xFF && (l and 0x07) == 0 -> l or r
                        else -> throw AssemblerException("Address 0x${l.toString(16).uppercase()} is not bit-addressable")
                    }
                }
            }
        }
        is Expr.Immediate -> evalExpr(expr.expr, symbols, pc)
        is Expr.AtReg -> throw AssemblerException("@R operand in expression context")
        is Expr.AtDPTR -> throw AssemblerException("@DPTR operand in expression context")
        is Expr.AtAPlusDPTR -> throw AssemblerException("@A+DPTR operand in expression context")
        is Expr.AtAPlusPC -> throw AssemblerException("@A+PC operand in expression context")
        is Expr.NotBit -> throw AssemblerException("'/' bit-complement operand in expression context")
    }
}
