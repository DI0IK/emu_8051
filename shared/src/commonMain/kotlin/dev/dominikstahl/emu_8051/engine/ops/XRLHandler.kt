package dev.dominikstahl.emu_8051.engine.ops

import dev.dominikstahl.emu_8051.engine.CpuState
import dev.dominikstahl.emu_8051.engine.Instruction

class XRLHandler(state: CpuState) : InstructionHandler(state) {
    override val handledInstructions: Set<Instruction> = setOf(
        Instruction.XRL_A_IMM, Instruction.XRL_A_DIRECT,
        Instruction.XRL_A_RI, Instruction.XRL_A_RN,
        Instruction.XRL_DIRECT_A, Instruction.XRL_DIRECT_IMM,
    )

    override fun handle(instruction: Instruction, rawOpcode: Int): Int {
        return when (instruction) {
            Instruction.XRL_A_RN -> {
                state.pc++
                state.ACC = state.ACC xor getRegister(rawOpcode and 0x07)
                1
            }

            Instruction.XRL_A_DIRECT -> {
                state.pc++
                val addr = state.rom[state.pc++]
                state.ACC = state.ACC xor state.readPin(addr)
                1
            }

            Instruction.XRL_A_RI -> {
                state.pc++
                val pointer = getRegister(rawOpcode and 0x01)
                state.ACC = state.ACC xor state.readIndirect(pointer)
                1
            }

            Instruction.XRL_A_IMM -> {
                state.pc++
                state.ACC = state.ACC xor state.rom[state.pc++]
                1
            }

            Instruction.XRL_DIRECT_A -> {
                state.pc++
                val addr = state.rom[state.pc++]
                state.writeDirect(addr, state.readDirect(addr) xor state.ACC)
                1
            }

            Instruction.XRL_DIRECT_IMM -> {
                state.pc++
                val addr = state.rom[state.pc++]
                val data = state.rom[state.pc++]
                state.writeDirect(addr, state.readDirect(addr) xor data)
                2
            }

            else -> 1
        }
    }
}
