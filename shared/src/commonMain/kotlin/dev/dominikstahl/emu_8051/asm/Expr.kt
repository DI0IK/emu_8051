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
    data class Invalid(val message: String, val line: Int) : Expr()
}

enum class UnaryOp { PLUS, MINUS, NOT, HIGH, LOW }
enum class BinaryOp { PLUS, MINUS, TIMES, DIV, MOD, AND, OR, XOR, SHR, SHL, DOT }

data class ParseResult<T>(val value: T, val nextIndex: Int)

fun isBinaryOp(type: TokenType): Boolean = type in listOf(
    TokenType.PLUS, TokenType.MINUS, TokenType.STAR, TokenType.SLASH,
    TokenType.KW_MOD, TokenType.KW_AND, TokenType.KW_OR, TokenType.KW_XOR,
    TokenType.KW_SHR, TokenType.KW_SHL, TokenType.DOT
)

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

fun parseExpr(tokens: List<Token>, start: Int): ParseResult<Expr> = parseBinary(tokens, start, 0)

private val precedence = mapOf(
    TokenType.KW_OR to 1, TokenType.KW_XOR to 2, TokenType.KW_AND to 3,
    TokenType.KW_SHR to 4, TokenType.KW_SHL to 4,
    TokenType.PLUS to 5, TokenType.MINUS to 5,
    TokenType.STAR to 6, TokenType.SLASH to 6, TokenType.KW_MOD to 6,
    TokenType.DOT to 7
)

private fun parseBinary(tokens: List<Token>, start: Int, minPrecedence: Int): ParseResult<Expr> {
    var (left, pos) = parseUnary(tokens, start)
    while (pos < tokens.size) {
        val p = precedence[tokens[pos].type] ?: break
        if (p < minPrecedence) break
        val op = tokenToBinaryOp(tokens[pos].type)
        pos++
        val (right, newPos) = parseBinary(tokens, pos, p + 1)
        left = Expr.Binary(left, op, right)
        pos = newPos
    }
    return ParseResult(left, pos)
}

private fun parseUnary(tokens: List<Token>, start: Int): ParseResult<Expr> {
    if (start >= tokens.size) return ParseResult(Expr.Number(0), start)

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
        else -> parsePrimary(tokens, start)
    }
}

private fun parsePrimary(tokens: List<Token>, start: Int): ParseResult<Expr> {
    if (start >= tokens.size) return ParseResult(Expr.Number(0), start)

    return when (tokens[start].type) {
        TokenType.NUMBER -> ParseResult(Expr.Number(tokens[start].intValue), start + 1)
        TokenType.SYMBOL -> ParseResult(Expr.Symbol(tokens[start].text), start + 1)
        TokenType.DOLLAR -> ParseResult(Expr.PCLocation, start + 1)
        TokenType.STRING -> {
            val s = tokens[start].text
            val v = if (s.isNotEmpty()) s[0].code else 0
            ParseResult(Expr.Number(v), start + 1)
        }
        TokenType.LPAREN -> {
            val (e, pos) = parseExpr(tokens, start + 1)
            if (pos >= tokens.size || tokens[pos].type != TokenType.RPAREN) {
                return ParseResult(Expr.Invalid("Missing closing parenthesis", tokens[start].line), pos)
            }
            val endPos = pos + 1
            ParseResult(e, endPos)
        }
        TokenType.HASH -> {
            val (e, p) = parseExpr(tokens, start + 1)
            ParseResult(Expr.Immediate(e), p)
        }
        TokenType.AT -> {
            if (start + 1 >= tokens.size) return ParseResult(Expr.Invalid("Incomplete @ operand", tokens[start].line), start + 1)
            when (tokens[start + 1].text.uppercase()) {
                "R0" -> ParseResult(Expr.AtReg(0), start + 2)
                "R1" -> ParseResult(Expr.AtReg(1), start + 2)
                "DPTR" -> ParseResult(Expr.AtDPTR, start + 2)
                "A" -> {
                    if (start + 3 < tokens.size && tokens[start + 2].type == TokenType.PLUS) {
                        when (tokens[start + 3].text.uppercase()) {
                            "DPTR" -> ParseResult(Expr.AtAPlusDPTR, start + 4)
                            "PC" -> ParseResult(Expr.AtAPlusPC, start + 4)
                            else -> ParseResult(Expr.Invalid("Unknown @A+ operand", tokens[start + 3].line), start + 4)
                        }
                    } else ParseResult(Expr.Invalid("Expected @A+DPTR or @A+PC", tokens[start].line), start + 2)
                }
                else -> ParseResult(Expr.Invalid("Unknown @ operand", tokens[start + 1].line), start + 2)
            }
        }
        TokenType.SLASH -> {
            val (e, p) = parseBinary(tokens, start + 1, precedence.getValue(TokenType.DOT))
            ParseResult(Expr.NotBit(e), p)
        }
        else -> {
            ParseResult(Expr.Invalid("Expected expression, got ${tokens[start].type}", tokens[start].line), start + 1)
        }
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
                UnaryOp.HIGH -> (v shr 8) and 0xFF
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
                BinaryOp.SHR -> l shr r
                BinaryOp.SHL -> l shl r
                BinaryOp.DOT -> (l and 0xF8) or (r and 0x07)
            }
        }
        is Expr.Immediate -> evalExpr(expr.expr, symbols, pc)
        is Expr.AtReg -> throw AssemblerException("@R operand in expression context")
        is Expr.AtDPTR -> throw AssemblerException("@DPTR operand in expression context")
        is Expr.AtAPlusDPTR -> throw AssemblerException("@A+DPTR operand in expression context")
        is Expr.AtAPlusPC -> throw AssemblerException("@A+PC operand in expression context")
        is Expr.NotBit -> evalExpr(expr.expr, symbols, pc)
        is Expr.Invalid -> throw AssemblerException(expr.message)
    }
}
