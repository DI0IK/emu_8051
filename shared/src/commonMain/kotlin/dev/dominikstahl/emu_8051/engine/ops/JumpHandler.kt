package dev.dominikstahl.emu_8051.engine.ops

import dev.dominikstahl.emu_8051.engine.CpuState
import dev.dominikstahl.emu_8051.engine.Instruction

@OptIn(ExperimentalUnsignedTypes::class)
class JumpHandler(state: CpuState) : InstructionHandler(state) {
    override val handledInstructions: Set<Instruction> = setOf(
        Instruction.LJMP,
        Instruction.JBC, Instruction.JB, Instruction.JNB,
        Instruction.JC, Instruction.JNC,
        Instruction.JZ, Instruction.JNZ,
        Instruction.JMP_AT_A_DPTR,
    )

    override fun handle(instruction: Instruction, rawOpcode: Int): Int {
        return when (instruction) {
            Instruction.LJMP -> {
                state.pc++
                val addrHigh = state.rom[state.pc++].toInt()
                val addrLow = state.rom[state.pc].toInt()
                state.pc = (addrHigh shl 8) or addrLow
                2
            }

            Instruction.JBC -> {
                state.pc++
                val bitAddr = state.rom[state.pc++]
                val relOffset = state.rom[state.pc++].toByte().toInt()
                if (getBit(bitAddr)) {
                    setBit(bitAddr, false)
                    state.pc = (state.pc + relOffset) and 0xFFFF
                }
                2
            }

            Instruction.JB -> {
                state.pc++
                val bitAddr = state.rom[state.pc++]
                val relOffset = state.rom[state.pc++].toByte().toInt()
                if (getBit(bitAddr)) state.pc = (state.pc + relOffset) and 0xFFFF
                2
            }

            Instruction.JNB -> {
                state.pc++
                val bitAddr = state.rom[state.pc++]
                val relOffset = state.rom[state.pc++].toByte().toInt()
                if (!getBit(bitAddr)) state.pc = (state.pc + relOffset) and 0xFFFF
                2
            }

            Instruction.JC -> {
                state.pc++
                val relOffset = state.rom[state.pc++].toByte().toInt()
                if ((state.PSW.toInt() and CY_BIT) != 0) state.pc = (state.pc + relOffset) and 0xFFFF
                2
            }

            Instruction.JNC -> {
                state.pc++
                val relOffset = state.rom[state.pc++].toByte().toInt()
                if ((state.PSW.toInt() and CY_BIT) == 0) state.pc = (state.pc + relOffset) and 0xFFFF
                2
            }

            Instruction.JZ -> {
                state.pc++
                val relOffset = state.rom[state.pc++].toByte().toInt()
                if (state.ACC.toInt() == 0) state.pc = (state.pc + relOffset) and 0xFFFF
                2
            }

            Instruction.JNZ -> {
                state.pc++
                val relOffset = state.rom[state.pc++].toByte().toInt()
                if (state.ACC.toInt() != 0) state.pc = (state.pc + relOffset) and 0xFFFF
                2
            }

            Instruction.JMP_AT_A_DPTR -> {
                state.pc++
                state.pc = (state.ACC.toInt() + state.DPTR) and 0xFFFF
                2
            }

            else -> 1
        }
    }
}
