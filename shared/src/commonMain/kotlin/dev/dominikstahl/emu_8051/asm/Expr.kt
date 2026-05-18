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

fun parseExpr(tokens: List<Token>, start: Int): ParseResult<Expr> {
    return parseSum(tokens, start)
}

private fun parseSum(tokens: List<Token>, start: Int): ParseResult<Expr> {
    var (left, pos) = parseProduct(tokens, start)
    while (pos < tokens.size && tokens[pos].type in listOf(TokenType.PLUS, TokenType.MINUS)) {
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
    while (pos < tokens.size && tokens[pos].type in listOf(TokenType.STAR, TokenType.SLASH, TokenType.KW_MOD)) {
        val op = tokenToBinaryOp(tokens[pos].type)
        pos++
        val (right, newPos) = parseUnary(tokens, pos)
        left = Expr.Binary(left, op, right)
        pos = newPos
    }
    return ParseResult(left, pos)
}

private fun parseUnary(tokens: List<Token>, start: Int): ParseResult<Expr> {
    if (start >= tokens.size) return ParseResult(Expr.Number(0), start)

    return when (tokens[start].type) {
        TokenType.PLUS -> {
            val (e, p) = parseDotSelect(tokens, start + 1)
            ParseResult(Expr.Unary(UnaryOp.PLUS, e), p)
        }
        TokenType.MINUS -> {
            val (e, p) = parseDotSelect(tokens, start + 1)
            ParseResult(Expr.Unary(UnaryOp.MINUS, e), p)
        }
        TokenType.KW_NOT -> {
            val (e, p) = parseDotSelect(tokens, start + 1)
            ParseResult(Expr.Unary(UnaryOp.NOT, e), p)
        }
        TokenType.KW_HIGH -> {
            val (e, p) = parseDotSelect(tokens, start + 1)
            ParseResult(Expr.Unary(UnaryOp.HIGH, e), p)
        }
        TokenType.KW_LOW -> {
            val (e, p) = parseDotSelect(tokens, start + 1)
            ParseResult(Expr.Unary(UnaryOp.LOW, e), p)
        }
        else -> parseDotSelect(tokens, start)
    }
}

private fun parseDotSelect(tokens: List<Token>, start: Int): ParseResult<Expr> {
    var (left, pos) = parsePrimary(tokens, start)
    while (pos < tokens.size && tokens[pos].type == TokenType.DOT) {
        pos++
        if (pos < tokens.size && tokens[pos].type == TokenType.NUMBER) {
            left = Expr.Binary(left, BinaryOp.DOT, Expr.Number(tokens[pos].intValue))
            pos++
        } else {
            break
        }
    }
    return ParseResult(left, pos)
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
            val endPos = if (pos < tokens.size && tokens[pos].type == TokenType.RPAREN) pos + 1 else pos
            ParseResult(e, endPos)
        }
        TokenType.HASH -> {
            val (e, p) = parseExpr(tokens, start + 1)
            ParseResult(Expr.Immediate(e), p)
        }
        TokenType.AT -> {
            if (start + 1 >= tokens.size) return ParseResult(Expr.Number(0), start + 1)
            when (tokens[start + 1].text.uppercase()) {
                "R0" -> ParseResult(Expr.AtReg(0), start + 2)
                "R1" -> ParseResult(Expr.AtReg(1), start + 2)
                "DPTR" -> ParseResult(Expr.AtDPTR, start + 2)
                "A" -> {
                    if (start + 3 < tokens.size && tokens[start + 2].type == TokenType.PLUS) {
                        when (tokens[start + 3].text.uppercase()) {
                            "DPTR" -> ParseResult(Expr.AtAPlusDPTR, start + 4)
                            "PC" -> ParseResult(Expr.AtAPlusPC, start + 4)
                            else -> ParseResult(Expr.Number(0), start + 3)
                        }
                    } else ParseResult(Expr.Number(0), start + 2)
                }
                else -> ParseResult(Expr.Number(0), start + 2)
            }
        }
        TokenType.SLASH -> {
            val (e, p) = parseDotSelect(tokens, start + 1)
            ParseResult(Expr.NotBit(e), p)
        }
        else -> {
            // Treat unknown tokens as symbols for compatibility with syntax highlighting
            // Always advance to prevent infinite loops
            ParseResult(Expr.Symbol(tokens[start].text), start + 1)
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
                BinaryOp.DIV -> if (r != 0) l / r else 0
                BinaryOp.MOD -> if (r != 0) l % r else 0
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
    }
}
