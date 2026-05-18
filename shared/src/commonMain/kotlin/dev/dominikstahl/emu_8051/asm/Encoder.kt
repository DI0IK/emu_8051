package dev.dominikstahl.emu_8051.asm

import dev.dominikstahl.emu_8051.engine.Instruction
import dev.dominikstahl.emu_8051.engine.OpcodePattern
import dev.dominikstahl.emu_8051.engine.Operand

class AssemblerException(message: String) : Exception(message)

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
                        errors.add(AssemblyError(0, "ORG expression error: ${e.message}"))
                        break
                    }
                }
                is Stmt.End -> { endEncountered = true }
                is Stmt.Equ -> symbols.defer(stmt.symbol, stmt.expr)
                is Stmt.Data -> symbols.defer(stmt.symbol, stmt.expr)
                is Stmt.Bit -> symbols.defer(stmt.symbol, stmt.expr)
                is Stmt.Code -> symbols.defer(stmt.symbol, stmt.expr)
                is Stmt.Label -> {
                    symbols.set(stmt.name, pc, SymType.LABEL)
                }
                is Stmt.Instruction -> {
                    if (stmt.label != null) {
                        symbols.set(stmt.label, pc, SymType.LABEL)
                    }
                    val result = findInstruction(stmt.mnemonic, stmt.operands, symbols, pc)
                    if (result != null) {
                        records.add(Record(stmt, pc))
                        pc += result.instruction.bytes
                    } else if (stmt.mnemonic.uppercase() !in Instruction.byMnemonic) {
                        errors.add(AssemblyError(stmt.line, "Unknown mnemonic: ${stmt.mnemonic}"))
                    } else {
                        errors.add(AssemblyError(stmt.line, "No matching instruction for ${stmt.mnemonic} with these operands"))
                    }
                }
                is Stmt.Db -> {
                    if (stmt.label != null) {
                        symbols.set(stmt.label, pc, SymType.LABEL)
                    }
                    var size = 0
                    for (item in stmt.values) {
                        when (item) {
                            is DbItem.ExprValue -> size++
                            is DbItem.StringValue -> size += item.string.length
                        }
                    }
                    records.add(Record(stmt, pc))
                    pc += size
                }
                is Stmt.Dw -> {
                    if (stmt.label != null) {
                        symbols.set(stmt.label, pc, SymType.LABEL)
                    }
                    records.add(Record(stmt, pc))
                    pc += stmt.values.size * 2
                }
                is Stmt.Ds -> {
                    if (stmt.label != null) {
                        symbols.set(stmt.label, pc, SymType.LABEL)
                    }
                    val count = try {
                        evalExpr(stmt.count, symbols, pc)
                    } catch (e: Exception) {
                        errors.add(AssemblyError(0, "DS count error: ${e.message}"))
                        0
                    }
                    if (count > 0) pc += count
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
                                rom[addr++] = v.toUByte()
                            }
                            is DbItem.StringValue -> {
                                for (ch in item.string) {
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
                        rom[addr++] = ((v shr 8) and 0xFF).toUByte()
                        rom[addr++] = (v and 0xFF).toUByte()
                    }
                }
                else -> {}
            }
        } catch (e: AssemblerException) {
            errors.add(AssemblyError(0, e.message ?: "Encoding error"))
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
