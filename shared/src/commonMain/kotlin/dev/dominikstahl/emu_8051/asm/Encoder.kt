@file:OptIn(ExperimentalUnsignedTypes::class)

package dev.dominikstahl.emu_8051.asm

import dev.dominikstahl.emu_8051.engine.Instruction
import dev.dominikstahl.emu_8051.engine.OpcodePattern
import dev.dominikstahl.emu_8051.engine.Operand

class AssemblerException(message: String) : Exception(message)

fun safeEval(expr: Expr, symbols: SymbolTable, pc: Int, default: Int = 0): Int {
    return try { evalExpr(expr, symbols, pc) } catch (_: Exception) { default }
}

private fun evalOperand(expr: Expr, symbols: SymbolTable, pc: Int, strict: Boolean): Int {
    return if (strict) evalExpr(expr, symbols, pc) else safeEval(expr, symbols, pc)
}

fun getOperandTypes(
    expr: Expr,
    symbols: SymbolTable,
    pc: Int,
    strict: Boolean = false,
): List<Pair<Operand, Int>> {
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
                    val v = evalOperand(expr, symbols, pc, strict)
                    listOf(
                        Operand.DIRECT to v,
                        Operand.BIT to v,
                        Operand.REL to v,
                        Operand.ADDR11 to v,
                        Operand.ADDR16 to v
                    )
                }
            }
        }
        is Expr.Number -> {
            val v = expr.value
            listOf(
                Operand.DIRECT to v,
                Operand.BIT to v,
                Operand.REL to v,
                Operand.ADDR11 to v,
                Operand.ADDR16 to v
            )
        }
        is Expr.PCLocation -> {
            listOf(
                Operand.DIRECT to pc,
                Operand.BIT to pc,
                Operand.REL to pc,
                Operand.ADDR11 to pc,
                Operand.ADDR16 to pc
            )
        }
        is Expr.Binary -> {
            if (expr.op == BinaryOp.DOT) {
                val bitAddr = evalOperand(expr, symbols, pc, strict)
                listOf(Operand.BIT to bitAddr)
            } else {
                val v = evalOperand(expr, symbols, pc, strict)
                listOf(
                    Operand.DIRECT to v,
                    Operand.BIT to v,
                    Operand.REL to v,
                    Operand.ADDR11 to v,
                    Operand.ADDR16 to v
                )
            }
        }
        is Expr.Unary -> {
            val v = evalOperand(expr, symbols, pc, strict)
            listOf(
                Operand.DIRECT to v,
                Operand.BIT to v,
                Operand.REL to v,
                Operand.ADDR11 to v,
                Operand.ADDR16 to v
            )
        }
        is Expr.Immediate -> {
            val v = evalOperand(expr.expr, symbols, pc, strict)
            listOf(Operand.IMM8 to v, Operand.IMM16 to v)
        }
        is Expr.AtReg -> listOf(Operand.RI to expr.index)
        is Expr.AtDPTR -> listOf(Operand.AT_DPTR to 0)
        is Expr.AtAPlusDPTR -> listOf(Operand.AT_A_DPTR to 0)
        is Expr.AtAPlusPC -> listOf(Operand.AT_A_PC to 0)
        is Expr.NotBit -> {
            val v = evalOperand(expr.expr, symbols, pc, strict)
            listOf(Operand.NOT_BIT to v)
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
    pc: Int,
    strict: Boolean = false,
): EncodedInstruction? {
    val upper = mnemonic.uppercase()
    val candidates = Instruction.byMnemonic[upper] ?: return null

    if (exprs.isEmpty()) {
        val inst = candidates.find { it.operands.isEmpty() } ?: return null
        return EncodedInstruction(inst, emptyList())
    }

    val allOptions = exprs.map { getOperandTypes(it, symbols, pc, strict) }

    for (inst in candidates) {
        if (inst.operands.size != allOptions.size) continue
        var matched = true
        val classified = ArrayList<Pair<Operand, Int>>(allOptions.size)
        for (i in inst.operands.indices) {
            val expected = inst.operands[i]
            val match = allOptions[i].firstOrNull { it.first == expected }
            if (match == null) {
                matched = false
                break
            }
            classified.add(match)
        }
        if (matched) return EncodedInstruction(inst, classified)
    }
    return null
}

fun encodeOperandBytes(
    instruction: Instruction,
    classified: List<Pair<Operand, Int>>,
    pc: Int,
    line: Int = 0,
    warnings: MutableList<AssemblyWarning>? = null,
): List<Int> {
    val bytes = mutableListOf<Int>()

    // 8051 MOV direct, direct (0x85) encodes [0x85, src, dest] while ASM syntax is MOV dest, src
    val ordered = if (instruction == Instruction.MOV_DIRECT_DIRECT && classified.size == 2) {
        listOf(classified[1], classified[0])
    } else {
        classified
    }

    for ((type, value) in ordered) {
        when (type) {
            Operand.IMM8 -> {
                if (value !in -128..255) {
                    warnings?.add(AssemblyWarning(line, "Immediate value $value truncated to 8 bits (0x${(value and 0xFF).toString(16).uppercase()})"))
                }
                bytes.add(value and 0xFF)
            }
            Operand.IMM16 -> {
                if (value !in -32768..65535) {
                    warnings?.add(AssemblyWarning(line, "Immediate value $value truncated to 16 bits"))
                }
                bytes.add((value shr 8) and 0xFF)
                bytes.add(value and 0xFF)
            }
            Operand.DIRECT -> {
                if (value !in 0..255) throw AssemblerException("Direct address out of range (0..255): $value")
                bytes.add(value and 0xFF)
            }
            Operand.BIT, Operand.NOT_BIT -> {
                if (value !in 0..255) throw AssemblerException("Bit address out of range (0..255): $value")
                bytes.add(value and 0xFF)
            }
            Operand.REL -> {
                val offset = value - (pc + instruction.bytes)
                if (offset !in -128..127) {
                    throw AssemblerException("Relative jump offset $offset out of range (-128..127)")
                }
                bytes.add(offset and 0xFF)
            }
            Operand.ADDR11 -> {
                val nextPc = (pc + 2) and 0xFFFF
                if (value !in 0..0xFFFF || (value and 0xF800) != (nextPc and 0xF800)) {
                    throw AssemblerException(
                        "11-bit address 0x${value.toString(16).uppercase()} outside 2KB page 0x${(nextPc and 0xF800).toString(16).uppercase()}"
                    )
                }
                bytes.add(value and 0xFF)
            }
            Operand.ADDR16 -> {
                if (value !in 0..0xFFFF) {
                    throw AssemblerException("16-bit target address out of range (0..65535): $value")
                }
                bytes.add((value shr 8) and 0xFF)
                bytes.add(value and 0xFF)
            }
            Operand.A, Operand.C, Operand.AB, Operand.DPTR,
            Operand.RN, Operand.RI, Operand.AT_DPTR, Operand.AT_A_DPTR, Operand.AT_A_PC -> {}
        }
    }
    return bytes
}

private data class Record(
    val stmt: Stmt,
    val address: Int,
    val line: Int,
)

private fun StatementLine(stmt: Stmt): Int = when (stmt) {
    is Stmt.Org -> stmt.line
    is Stmt.End -> stmt.line
    is Stmt.Equ -> stmt.line
    is Stmt.Data -> stmt.line
    is Stmt.Bit -> stmt.line
    is Stmt.Code -> stmt.line
    is Stmt.Db -> stmt.line
    is Stmt.Dw -> stmt.line
    is Stmt.Ds -> stmt.line
    is Stmt.Instruction -> stmt.line
    is Stmt.Label -> stmt.line
    is Stmt.ErrorMsg -> stmt.line
    Stmt.Blank -> 0
}

fun assembleInternal(statements: List<Stmt>): AssemblyResult {
    val symbols = SymbolTable()
    initPredefinedSymbols(symbols)
    val errors = mutableListOf<AssemblyError>()
    val warnings = mutableListOf<AssemblyWarning>()

    // ---- Pass 1: Build symbol table, compute PC ----
    var pc = 0
    val records = mutableListOf<Record>()
    var endEncountered = false

    fun defineConstant(symbol: String, expr: Expr, type: SymType, line: Int) {
        try {
            val v = evalExpr(expr, symbols, pc)
            symbols.set(symbol, v, type, line)
        } catch (e: AssemblerException) {
            if (e.message?.startsWith("Undefined symbol:") == true) {
                symbols.defer(symbol, expr, type, pc, line)
            } else {
                errors.add(AssemblyError(line, e.message ?: "Symbol definition error"))
            }
        }
    }

    for (stmt in statements) {
        if (endEncountered) break
        val line = StatementLine(stmt)

        try {
            when (stmt) {
                is Stmt.Org -> {
                    val newPc = evalExpr(stmt.expr, symbols, pc)
                    if (newPc !in 0..0xFFFF) {
                        errors.add(AssemblyError(stmt.line, "ORG address out of range (0..65535): $newPc"))
                        break
                    }
                    pc = newPc
                }
                is Stmt.End -> { endEncountered = true }
                is Stmt.Equ -> defineConstant(stmt.symbol, stmt.expr, SymType.EQU, stmt.line)
                is Stmt.Data -> defineConstant(stmt.symbol, stmt.expr, SymType.DATA, stmt.line)
                is Stmt.Bit -> defineConstant(stmt.symbol, stmt.expr, SymType.BIT, stmt.line)
                is Stmt.Code -> defineConstant(stmt.symbol, stmt.expr, SymType.CODE, stmt.line)
                is Stmt.Label -> {
                    symbols.set(stmt.name, pc, SymType.LABEL, stmt.line)
                }
                is Stmt.Instruction -> {
                    if (stmt.label != null) {
                        symbols.set(stmt.label, pc, SymType.LABEL, stmt.line)
                    }
                    val result = findInstruction(stmt.mnemonic, stmt.operands, symbols, pc, strict = false)
                    if (result != null) {
                        if (pc + result.instruction.bytes > 0x10000) {
                            errors.add(AssemblyError(stmt.line, "Program exceeds 64KB ROM limit at 0x${pc.toString(16).uppercase()}"))
                            break
                        }
                        records.add(Record(stmt, pc, stmt.line))
                        pc += result.instruction.bytes
                    } else if (stmt.mnemonic.uppercase() !in Instruction.byMnemonic) {
                        errors.add(AssemblyError(stmt.line, "Unknown mnemonic: ${stmt.mnemonic}"))
                    } else {
                        errors.add(AssemblyError(stmt.line, "No matching instruction for ${stmt.mnemonic} with these operands"))
                    }
                }
                is Stmt.Db -> {
                    if (stmt.label != null) {
                        symbols.set(stmt.label, pc, SymType.LABEL, stmt.line)
                    }
                    var size = 0
                    for (item in stmt.values) {
                        when (item) {
                            is DbItem.ExprValue -> size++
                            is DbItem.StringValue -> size += item.string.length
                        }
                    }
                    if (pc + size > 0x10000) {
                        errors.add(AssemblyError(stmt.line, "DB exceeds 64KB ROM limit"))
                        break
                    }
                    records.add(Record(stmt, pc, stmt.line))
                    pc += size
                }
                is Stmt.Dw -> {
                    if (stmt.label != null) {
                        symbols.set(stmt.label, pc, SymType.LABEL, stmt.line)
                    }
                    val size = stmt.values.size * 2
                    if (pc + size > 0x10000) {
                        errors.add(AssemblyError(stmt.line, "DW exceeds 64KB ROM limit"))
                        break
                    }
                    records.add(Record(stmt, pc, stmt.line))
                    pc += size
                }
                is Stmt.Ds -> {
                    if (stmt.label != null) {
                        symbols.set(stmt.label, pc, SymType.LABEL, stmt.line)
                    }
                    val count = evalExpr(stmt.count, symbols, pc)
                    if (count < 0) {
                        errors.add(AssemblyError(stmt.line, "DS count cannot be negative: $count"))
                    } else if (pc + count > 0x10000) {
                        errors.add(AssemblyError(stmt.line, "DS exceeds 64KB ROM limit"))
                        break
                    } else {
                        pc += count
                    }
                }
                is Stmt.Blank -> {}
                is Stmt.ErrorMsg -> errors.add(AssemblyError(stmt.line, stmt.message))
            }
        } catch (e: AssemblerException) {
            errors.add(AssemblyError(line, e.message ?: "Assembly error"))
        }
    }

    // Resolve deferred symbols (EQU, DATA, BIT, CODE)
    errors.addAll(symbols.resolveDeferred())

    if (errors.isNotEmpty()) return AssemblyResult.failure(errors, warnings)

    val sourceMap = mutableMapOf<Int, Int>()
    for (rec in records) {
        if (rec.stmt is Stmt.Instruction) {
            sourceMap[rec.stmt.line] = rec.address
        }
    }

    // ---- Pass 2: Encode ----
    val rom = UByteArray(65536)
    val written = BooleanArray(65536)

    fun writeByte(addr: Int, byteVal: Int, lineNum: Int, warnedOverlap: Boolean): Boolean {
        if (addr !in 0..65535) throw AssemblerException("ROM address out of range: $addr")
        var didWarn = warnedOverlap
        if (written[addr] && !didWarn) {
            warnings.add(AssemblyWarning(lineNum, "Overwriting previously emitted ROM address 0x${addr.toString(16).uppercase()}"))
            didWarn = true
        }
        written[addr] = true
        rom[addr] = (byteVal and 0xFF).toUByte()
        return didWarn
    }

    for (rec in records) {
        try {
            var warnedOverlap = false
            when (val stmt = rec.stmt) {
                is Stmt.Instruction -> {
                    val result = findInstruction(stmt.mnemonic, stmt.operands, symbols, rec.address, strict = true)
                    if (result == null) {
                        errors.add(AssemblyError(stmt.line, "Cannot encode ${stmt.mnemonic}"))
                        continue
                    }

                    val inst = result.instruction
                    val opcode = computeOpcode(inst, result.classified)
                    val operandBytes = encodeOperandBytes(inst, result.classified, rec.address, stmt.line, warnings)

                    warnedOverlap = writeByte(rec.address, opcode, stmt.line, warnedOverlap)
                    for ((i, b) in operandBytes.withIndex()) {
                        warnedOverlap = writeByte(rec.address + 1 + i, b, stmt.line, warnedOverlap)
                    }
                }
                is Stmt.Db -> {
                    var addr = rec.address
                    for (item in stmt.values) {
                        when (item) {
                            is DbItem.ExprValue -> {
                                val v = evalExpr(item.expr, symbols, addr)
                                if (v !in -128..255) {
                                    warnings.add(AssemblyWarning(stmt.line, "DB value $v truncated to 8 bits"))
                                }
                                warnedOverlap = writeByte(addr++, v, stmt.line, warnedOverlap)
                            }
                            is DbItem.StringValue -> {
                                for (ch in item.string) {
                                    warnedOverlap = writeByte(addr++, ch.code, stmt.line, warnedOverlap)
                                }
                            }
                        }
                    }
                }
                is Stmt.Dw -> {
                    var addr = rec.address
                    for (expr in stmt.values) {
                        val v = evalExpr(expr, symbols, addr)
                        if (v !in -32768..65535) {
                            warnings.add(AssemblyWarning(stmt.line, "DW value $v truncated to 16 bits"))
                        }
                        warnedOverlap = writeByte(addr++, v shr 8, stmt.line, warnedOverlap)
                        warnedOverlap = writeByte(addr++, v, stmt.line, warnedOverlap)
                    }
                }
                else -> {}
            }
        } catch (e: AssemblerException) {
            errors.add(AssemblyError(rec.line, e.message ?: "Encoding error"))
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
