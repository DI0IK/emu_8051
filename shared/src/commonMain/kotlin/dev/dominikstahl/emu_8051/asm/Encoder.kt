package dev.dominikstahl.emu_8051.asm

import dev.dominikstahl.emu_8051.engine.Instruction
import dev.dominikstahl.emu_8051.engine.OpcodePattern
import dev.dominikstahl.emu_8051.engine.Operand

class AssemblerException(message: String) : Exception(message)

private fun containsSymbol(expr: Expr): Boolean = when (expr) {
    is Expr.Symbol -> true
    is Expr.Unary -> containsSymbol(expr.expr)
    is Expr.Binary -> containsSymbol(expr.left) || containsSymbol(expr.right)
    is Expr.Immediate -> containsSymbol(expr.expr)
    is Expr.NotBit -> containsSymbol(expr.expr)
    else -> false
}

fun safeEval(expr: Expr, symbols: SymbolTable, pc: Int, default: Int = 0): Int {
    return try { evalExpr(expr, symbols, pc) } catch (_: Exception) { default }
}

fun getOperandTypes(expr: Expr, symbols: SymbolTable, pc: Int): List<Pair<Operand, Int>> {
    return when (expr) {
        is Expr.Symbol -> {
            val name = expr.name.uppercase()
            when (name) {
                "A" -> listOf(Operand.A to 0)
                "C" -> listOf(Operand.C to 0)
                "AB" -> listOf(Operand.AB to 0)
                "DPTR" -> listOf(Operand.DPTR to 0)
                "R0" -> listOf(Operand.RN to 0)
                "R1" -> listOf(Operand.RN to 1)
                "R2" -> listOf(Operand.RN to 2)
                "R3" -> listOf(Operand.RN to 3)
                "R4" -> listOf(Operand.RN to 4)
                "R5" -> listOf(Operand.RN to 5)
                "R6" -> listOf(Operand.RN to 6)
                "R7" -> listOf(Operand.RN to 7)
                else -> {
                    val v = safeEval(expr, symbols, pc)
                    listOf(
                        Operand.DIRECT to (v and 0xFF),
                        Operand.BIT to (v and 0xFF),
                        Operand.REL to (v and 0xFF),
                        Operand.ADDR11 to (v and 0x7FF),
                        Operand.ADDR16 to (v and 0xFFFF)
                    )
                }
            }
        }
        is Expr.Number -> {
            val v = expr.value
            listOf(
                Operand.DIRECT to (v and 0xFF),
                Operand.BIT to (v and 0xFF),
                Operand.REL to (v and 0xFF),
                Operand.ADDR11 to (v and 0x7FF),
                Operand.ADDR16 to (v and 0xFFFF)
            )
        }
        is Expr.PCLocation -> {
            listOf(
                Operand.DIRECT to (pc and 0xFF),
                Operand.BIT to (pc and 0xFF),
                Operand.REL to (pc and 0xFF),
                Operand.ADDR11 to (pc and 0x7FF),
                Operand.ADDR16 to (pc and 0xFFFF)
            )
        }
        is Expr.Binary -> {
            if (expr.op == BinaryOp.DOT) {
                val byteVal = safeEval(expr.left, symbols, pc)
                val bitVal = safeEval(expr.right, symbols, pc)
                val bitAddr = (byteVal and 0xF8) or (bitVal and 0x07)
                listOf(Operand.BIT to bitAddr)
            } else {
                val v = safeEval(expr, symbols, pc)
                listOf(
                    Operand.DIRECT to (v and 0xFF),
                    Operand.BIT to (v and 0xFF),
                    Operand.REL to (v and 0xFF),
                    Operand.ADDR11 to (v and 0x7FF),
                    Operand.ADDR16 to (v and 0xFFFF)
                )
            }
        }
        is Expr.Unary -> {
            val v = safeEval(expr, symbols, pc)
            listOf(
                Operand.DIRECT to (v and 0xFF),
                Operand.BIT to (v and 0xFF),
                Operand.REL to (v and 0xFF),
                Operand.ADDR11 to (v and 0x7FF),
                Operand.ADDR16 to (v and 0xFFFF)
            )
        }
        is Expr.Immediate -> {
            val v = safeEval(expr.expr, symbols, pc)
            listOf(Operand.IMM8 to (v and 0xFF), Operand.IMM16 to (v and 0xFFFF))
        }
        is Expr.AtReg -> listOf(Operand.RI to expr.index)
        is Expr.AtDPTR -> listOf(Operand.AT_DPTR to 0)
        is Expr.AtAPlusDPTR -> listOf(Operand.AT_A_DPTR to 0)
        is Expr.AtAPlusPC -> listOf(Operand.AT_A_PC to 0)
        is Expr.NotBit -> {
            val v = safeEval(expr.expr, symbols, pc)
            listOf(Operand.NOT_BIT to (v and 0xFF))
        }
        is Expr.Invalid -> emptyList()
    }
}

data class EncodedInstruction(
    val instruction: Instruction,
    val classified: List<Pair<Operand, Int>>
)

fun findInstruction(
    mnemonic: String,
    exprs: List<Expr>,
    symbols: SymbolTable,
    pc: Int
): EncodedInstruction? {
    val upper = mnemonic.uppercase()
    val candidates = Instruction.byMnemonic[upper] ?: return null

    if (exprs.isEmpty()) {
        val inst = Instruction.byMnemonic[upper]?.find { it.operands.isEmpty() } ?: return null
        return EncodedInstruction(inst, emptyList())
    }

    val allOptions = exprs.map { getOperandTypes(it, symbols, pc) }

    fun <T> cartesianProduct(lists: List<List<T>>): Sequence<List<T>> = when {
        lists.isEmpty() -> sequenceOf(emptyList())
        else -> lists.first().asSequence().flatMap { e ->
            cartesianProduct(lists.drop(1)).map { listOf(e) + it }
        }
    }

    for (combo in cartesianProduct(allOptions)) {
        val types = combo.map { it.first }
        val inst = Instruction.lookup(upper, *types.toTypedArray())
        if (inst != null) {
            return EncodedInstruction(inst, combo)
        }
    }
    return null
}

fun encodeOperandBytes(
    instruction: Instruction,
    classified: List<Pair<Operand, Int>>,
    pc: Int
): List<Int> {
    val bytes = mutableListOf<Int>()
    for ((type, value) in classified) {
        when (type) {
            Operand.IMM8 -> bytes.add(value and 0xFF)
            Operand.IMM16 -> {
                bytes.add((value shr 8) and 0xFF)
                bytes.add(value and 0xFF)
            }
            Operand.DIRECT -> bytes.add(value and 0xFF)
            Operand.BIT -> bytes.add(value and 0xFF)
            Operand.NOT_BIT -> bytes.add(value and 0xFF)
            Operand.REL -> {
                val offset = value - (pc + instruction.bytes)
                bytes.add(offset and 0xFF)
            }
            Operand.ADDR11 -> {
                bytes.add(value and 0xFF)
            }
            Operand.ADDR16 -> {
                bytes.add((value shr 8) and 0xFF)
                bytes.add(value and 0xFF)
            }
            Operand.A, Operand.C, Operand.AB, Operand.DPTR -> {}
            Operand.RN, Operand.RI, Operand.AT_DPTR, Operand.AT_A_DPTR, Operand.AT_A_PC -> {}
        }
    }
    return bytes
}

private fun validateInstruction(stmt: Stmt.Instruction, result: EncodedInstruction, pc: Int, symbols: SymbolTable) {
    val inst = result.instruction
    if (stmt.operands.size != inst.operands.size && !(inst.mnemonic == "JMP" && stmt.operands.size == 1))
        throw AssemblerException("Wrong operand count for ${stmt.mnemonic}")
    for (i in stmt.operands.indices) {
        val expr = stmt.operands[i]
        if (inst.mnemonic == "JMP") continue
        val type = inst.operands.getOrNull(i) ?: continue
        if (type !in setOf(
                Operand.IMM8, Operand.IMM16, Operand.DIRECT, Operand.BIT,
                Operand.NOT_BIT, Operand.REL, Operand.ADDR11, Operand.ADDR16
            )
        ) continue
        val value = evalExpr(expr, symbols, pc)
        when (type) {
            Operand.IMM8 ->
                if (value !in 0..0xFF && !(containsSymbol(expr) && value in 0..0xFFFF)) {
                    throw AssemblerException("Value $value does not fit in 8 bits")
                }
            Operand.DIRECT, Operand.BIT, Operand.NOT_BIT ->
                if (value !in 0..0xFF) throw AssemblerException("Value $value does not fit in 8 bits")
            Operand.IMM16, Operand.ADDR16 ->
                if (value !in 0..0xFFFF) throw AssemblerException("Address/value $value does not fit in 16 bits")
            Operand.REL -> {
                val offset = value - (pc + inst.bytes)
                if (offset !in -128..127) throw AssemblerException("Relative branch out of range: $offset")
            }
            Operand.ADDR11 -> {
                if (value !in 0..0xFFFF) throw AssemblerException("Address $value does not fit in 16 bits")
                if ((value and 0xF800) != ((pc + 2) and 0xF800))
                    throw AssemblerException("AJMP/ACALL target is outside the current 2KiB page")
            }
            else -> {}
        }
        if (type == Operand.BIT || type == Operand.NOT_BIT) {
            val addressable = (value in 0x20..0x2F) || value in 0x80..0xFF ||
                (expr is Expr.Symbol && symbols.getType(expr.name) == SymType.BIT)
            if (!addressable) throw AssemblerException("Invalid bit address: $value")
        }
        if (expr is Expr.Binary && expr.op == BinaryOp.DOT) {
            val base = evalExpr(expr.left, symbols, pc)
            val bit = evalExpr(expr.right, symbols, pc)
            if (bit !in 0..7) throw AssemblerException("Bit index out of range: $bit")
            if (base !in 0x20..0x2F && !(base in 0x80..0xFF && (base and 7) == 0))
                throw AssemblerException("Address is not bit-addressable: $base")
        }
        if ((type == Operand.BIT || type == Operand.NOT_BIT) &&
            expr is Expr.Symbol && symbols.getType(expr.name) != SymType.BIT
        ) {
            throw AssemblerException("Symbol ${expr.name} is not a bit symbol")
        }
    }
}

private data class Record(
    val stmt: Stmt,
    val address: Int
)

fun assembleInternal(statements: List<Stmt>): AssemblyResult {
    val symbols = SymbolTable()
    initPredefinedSymbols(symbols)
    val errors = mutableListOf<AssemblyError>()
    val warnings = mutableListOf<AssemblyWarning>()

    // ---- Pass 1: Build symbol table, compute PC ----
    var pc = 0
    val records = mutableListOf<Record>()
    var endEncountered = false

    for (stmt in statements) {
        if (endEncountered) break

        try {
            when (stmt) {
                is Stmt.Org -> {
                    pc = try {
                        evalExpr(stmt.expr, symbols, pc)
                    } catch (e: Exception) {
                        errors.add(AssemblyError(stmt.line, "ORG expression error: ${e.message}"))
                        break
                    }
                    if (pc !in 0..0xFFFF) {
                        errors.add(AssemblyError(stmt.line, "ORG address out of range: $pc"))
                        break
                    }
                }
                is Stmt.End -> { endEncountered = true }
                is Stmt.Equ -> if (!symbols.defer(stmt.symbol, stmt.expr, SymType.EQU, pc))
                    errors.add(AssemblyError(stmt.line, "Duplicate symbol: ${stmt.symbol}"))
                is Stmt.Data -> if (!symbols.defer(stmt.symbol, stmt.expr, SymType.DATA, pc))
                    errors.add(AssemblyError(stmt.line, "Duplicate symbol: ${stmt.symbol}"))
                is Stmt.Bit -> if (!symbols.defer(stmt.symbol, stmt.expr, SymType.BIT, pc))
                    errors.add(AssemblyError(stmt.line, "Duplicate symbol: ${stmt.symbol}"))
                is Stmt.Code -> if (!symbols.defer(stmt.symbol, stmt.expr, SymType.CODE, pc))
                    errors.add(AssemblyError(stmt.line, "Duplicate symbol: ${stmt.symbol}"))
                is Stmt.Label -> {
                    if (!symbols.set(stmt.name, pc, SymType.LABEL))
                        errors.add(AssemblyError(0, "Duplicate symbol: ${stmt.name}"))
                }
                is Stmt.Instruction -> {
                    if (stmt.label != null) {
                        if (!symbols.set(stmt.label, pc, SymType.LABEL))
                            errors.add(AssemblyError(stmt.line, "Duplicate symbol: ${stmt.label}"))
                    }
                    val result = findInstruction(stmt.mnemonic, stmt.operands, symbols, pc)
                    if (result != null) {
                        if (result.instruction.bytes > 0x10000 - pc) {
                            errors.add(AssemblyError(stmt.line, "Instruction exceeds code address space"))
                        } else {
                            records.add(Record(stmt, pc))
                            pc += result.instruction.bytes
                        }
                    } else if (stmt.mnemonic.uppercase() !in Instruction.byMnemonic) {
                        errors.add(AssemblyError(stmt.line, "Unknown mnemonic: ${stmt.mnemonic}"))
                    } else {
                        errors.add(AssemblyError(stmt.line, "No matching instruction for ${stmt.mnemonic} with these operands"))
                    }
                }
                is Stmt.Db -> {
                    if (stmt.label != null) {
                        if (!symbols.set(stmt.label, pc, SymType.LABEL))
                            errors.add(AssemblyError(0, "Duplicate symbol: ${stmt.label}"))
                    }
                    var size = 0
                    for (item in stmt.values) {
                        when (item) {
                            is DbItem.ExprValue -> size++
                            is DbItem.StringValue -> size += item.string.length
                        }
                    }
                    records.add(Record(stmt, pc))
                    if (size > 0x10000 - pc) errors.add(AssemblyError(0, "DB data exceeds address space"))
                    else pc += size
                }
                is Stmt.Dw -> {
                    if (stmt.label != null) {
                        if (!symbols.set(stmt.label, pc, SymType.LABEL))
                            errors.add(AssemblyError(0, "Duplicate symbol: ${stmt.label}"))
                    }
                    records.add(Record(stmt, pc))
                    val size = stmt.values.size * 2
                    if (size > 0x10000 - pc) errors.add(AssemblyError(0, "DW data exceeds address space"))
                    else pc += size
                }
                is Stmt.Ds -> {
                    if (stmt.label != null) {
                        if (!symbols.set(stmt.label, pc, SymType.LABEL))
                            errors.add(AssemblyError(stmt.line, "Duplicate symbol: ${stmt.label}"))
                    }
                    val count = try {
                        evalExpr(stmt.count, symbols, pc)
                    } catch (e: Exception) {
                        errors.add(AssemblyError(stmt.line, "DS count error: ${e.message}"))
                        0
                    }
                    if (count < 0 || count > 0x10000 - pc) {
                        errors.add(AssemblyError(stmt.line, "DS count out of range: $count"))
                    } else pc += count
                }
                is Stmt.Blank -> {}
                is Stmt.ErrorMsg -> errors.add(AssemblyError(stmt.line, stmt.message))
            }
        } catch (e: AssemblerException) {
            errors.add(AssemblyError(0, e.message ?: "Assembly error"))
        }
    }

    // Resolve deferred symbols (EQU, DATA, BIT, CODE)
    symbols.resolveDeferred(0)
    for ((name, expr) in symbols.unresolvedExpressions()) {
        try {
            evalExpr(expr, symbols, 0)
            errors.add(AssemblyError(0, "Undefined symbol: $name"))
        } catch (e: AssemblerException) {
            errors.add(AssemblyError(0, "$name: ${e.message}"))
        }

    }

    for (stmt in statements) {
        val declaration = when (stmt) {
            is Stmt.Data -> Triple(stmt.symbol, stmt.expr, 8)
            is Stmt.Bit -> Triple(stmt.symbol, stmt.expr, 8)
            is Stmt.Code -> Triple(stmt.symbol, stmt.expr, 16)
            else -> null
        } ?: continue
        try {
                val value = symbols.get(declaration.first)
                    ?: throw AssemblerException("Undefined symbol: ${declaration.first}")
                val valid = when (stmt) {
                    is Stmt.Bit -> value in 0x00..0xFF &&
                        ((value in 0x20..0x2F) || (value in 0x80..0xFF))
                    else -> value in 0..((1 shl declaration.third) - 1)
                }
                if (!valid) {
                    errors.add(AssemblyError(
                        when (stmt) {
                            is Stmt.Data -> stmt.line
                            is Stmt.Bit -> stmt.line
                            is Stmt.Code -> stmt.line
                            else -> 0
                        },
                        "Value $value is out of range for ${when (stmt) {
                            is Stmt.Data -> "DATA"
                            is Stmt.Bit -> "BIT"
                            is Stmt.Code -> "CODE"
                            else -> "symbol"
                        }}"
                    ))
                }
        } catch (_: AssemblerException) {
            // The unresolved-symbol pass above reports the underlying error.
        }
    }

    if (errors.isNotEmpty()) return AssemblyResult.failure(errors, warnings)

    val sourceMap = mutableMapOf<Int, Int>()
    for (rec in records) {
        when (val stmt = rec.stmt) {
            is Stmt.Instruction -> sourceMap[stmt.line] = rec.address
            else -> {}
        }
    }

    // ---- Pass 2: Encode ----
    val rom = UByteArray(65536)

    for (rec in records) {
        try {
            when (val stmt = rec.stmt) {
                is Stmt.Instruction -> {
                    val result = findInstruction(stmt.mnemonic, stmt.operands, symbols, rec.address)
                    if (result == null) {
                        errors.add(AssemblyError(stmt.line, "Cannot encode ${stmt.mnemonic}"))
                        continue
                    }

                    val inst = result.instruction
                    validateInstruction(stmt, result, rec.address, symbols)
                    val opcode = computeOpcode(inst, result.classified)
                    val operandBytes = encodeOperandBytes(inst, result.classified, rec.address)

                    rom[rec.address] = opcode.toUByte()
                    for ((i, b) in operandBytes.withIndex()) {
                        rom[rec.address + 1 + i] = b.toUByte()
                    }
                }
                is Stmt.Db -> {
                    var addr = rec.address
                    for (item in stmt.values) {
                        when (item) {
                            is DbItem.ExprValue -> {
                                val v = evalExpr(item.expr, symbols, addr)
                                if (v !in 0..0xFF) throw AssemblerException("DB value $v does not fit in 8 bits")
                                rom[addr++] = v.toUByte()
                            }
                            is DbItem.StringValue -> {
                                for (ch in item.string) {
                                    if (ch.code > 0xFF) {
                                        throw AssemblerException("DB string character is not an 8-bit value")
                                    }
                                    rom[addr++] = ch.code.toUByte()
                                }
                            }
                        }
                    }
                }
                is Stmt.Dw -> {
                    var addr = rec.address
                    for (expr in stmt.values) {
                        val v = evalExpr(expr, symbols, addr)
                        if (v !in 0..0xFFFF) throw AssemblerException("DW value $v does not fit in 16 bits")
                        rom[addr++] = ((v shr 8) and 0xFF).toUByte()
                        rom[addr++] = (v and 0xFF).toUByte()
                    }
                }
                else -> {}
            }
        } catch (e: AssemblerException) {
            val line = when (val s = rec.stmt) {
                is Stmt.Instruction -> s.line
                is Stmt.Org -> s.line
                is Stmt.Equ -> s.line
                is Stmt.Data -> s.line
                is Stmt.Bit -> s.line
                is Stmt.Code -> s.line
                is Stmt.Ds -> s.line
                else -> 0
            }
            errors.add(AssemblyError(line, e.message ?: "Encoding error"))
        }
    }

    if (errors.isNotEmpty()) return AssemblyResult.failure(errors, warnings)
    return AssemblyResult.success(rom, sourceMap, warnings)
}

fun computeOpcode(instruction: Instruction, classified: List<Pair<Operand, Int>>): Int {
    return when (val pattern = instruction.pattern) {
        is OpcodePattern.Fixed -> pattern.opcode
        is OpcodePattern.RegisterRange -> {
            val rnValue = classified.firstOrNull { it.first == Operand.RN }?.second ?: 0
            pattern.base + (rnValue and 0x07)
        }
        is OpcodePattern.IndirectRange -> {
            val riValue = classified.firstOrNull { it.first == Operand.RI }?.second ?: 0
            pattern.base + (riValue and 0x01)
        }
        is OpcodePattern.PageRange -> {
            val addr11 = classified.firstOrNull { it.first == Operand.ADDR11 }?.second ?: 0
            val page = (addr11 shr 8) and 0x07
            pattern.base + page * pattern.step
        }
    }
}
