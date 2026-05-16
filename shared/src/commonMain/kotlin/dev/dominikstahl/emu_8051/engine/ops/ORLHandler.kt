package dev.dominikstahl.emu_8051.engine.ops

import dev.dominikstahl.emu_8051.engine.CpuState
import dev.dominikstahl.emu_8051.engine.Instruction

class ORLHandler(state: CpuState) : InstructionHandler(state) {
    override val handledInstructions: Set<Instruction> = setOf(
        Instruction.ORL_A_IMM, Instruction.ORL_A_DIRECT,
        Instruction.ORL_A_RI, Instruction.ORL_A_RN,
        Instruction.ORL_DIRECT_A, Instruction.ORL_DIRECT_IMM,
        Instruction.ORL_C_BIT, Instruction.ORL_C_NOT_BIT,
    )

    override fun handle(instruction: Instruction, rawOpcode: Int): Int {
        return when (instruction) {
            Instruction.ORL_A_RN -> {
                state.pc++
                state.ACC = state.ACC or getRegister(rawOpcode and 0x07)
                1
            }

            Instruction.ORL_A_DIRECT -> {
                state.pc++
                val addr = state.rom[state.pc++]
                state.ACC = state.ACC or state.readPin(addr)
                1
            }

            Instruction.ORL_A_RI -> {
                state.pc++
                val pointer = getRegister(rawOpcode and 0x01)
                state.ACC = state.ACC or state.readIndirect(pointer)
                1
            }

            Instruction.ORL_A_IMM -> {
                state.pc++
                state.ACC = state.ACC or state.rom[state.pc++]
                1
            }

            Instruction.ORL_DIRECT_A -> {
                state.pc++
                val addr = state.rom[state.pc++]
                state.writeDirect(addr, state.readDirect(addr) or state.ACC)
                1
            }

            Instruction.ORL_DIRECT_IMM -> {
                state.pc++
                val addr = state.rom[state.pc++]
                val data = state.rom[state.pc++]
                state.writeDirect(addr, state.readDirect(addr) or data)
                2
            }

            Instruction.ORL_C_BIT -> {
                state.pc++
                val bitAddr = state.rom[state.pc++]
                val bitValue = getBit(bitAddr)
                val currentCarry = (state.PSW.toInt() and CY_BIT) != 0
                setFlag(CY_BIT, currentCarry || bitValue)
                2
            }

            Instruction.ORL_C_NOT_BIT -> {
                state.pc++
                val bitAddr = state.rom[state.pc++]
                val bitValue = getBit(bitAddr)
                val currentCarry = (state.PSW.toInt() and CY_BIT) != 0
                setFlag(CY_BIT, currentCarry || !bitValue)
                2
            }

            else -> 1
        }
    }
}
