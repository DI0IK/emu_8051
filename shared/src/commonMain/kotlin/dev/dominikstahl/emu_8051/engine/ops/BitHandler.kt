package dev.dominikstahl.emu_8051.engine.ops

import dev.dominikstahl.emu_8051.engine.CpuState
import dev.dominikstahl.emu_8051.engine.Instruction

class BitHandler(state: CpuState) : InstructionHandler(state) {
    override val handledInstructions: Set<Instruction> = setOf(
        Instruction.CLR_A, Instruction.CLR_C, Instruction.CLR_BIT,
        Instruction.SETB_C, Instruction.SETB_BIT,
        Instruction.CPL_A, Instruction.CPL_C, Instruction.CPL_BIT,
    )

    override fun handle(instruction: Instruction, rawOpcode: Int): Int {
        return when (instruction) {
            Instruction.CLR_A -> {
                state.pc++
                state.ACC = 0x00u
                1
            }

            Instruction.CLR_C -> {
                state.pc++
                setFlag(CY_BIT, false)
                1
            }

            Instruction.CLR_BIT -> {
                state.pc++
                val bitAddr = state.rom[state.pc++]
                setBit(bitAddr, false)
                1
            }

            Instruction.SETB_C -> {
                state.pc++
                setFlag(CY_BIT, true)
                1
            }

            Instruction.SETB_BIT -> {
                state.pc++
                val bitAddr = state.rom[state.pc++]
                setBit(bitAddr, true)
                1
            }

            Instruction.CPL_A -> {
                state.pc++
                state.ACC = state.ACC.inv()
                1
            }

            Instruction.CPL_C -> {
                state.pc++
                val currentCarry = (state.PSW.toInt() and CY_BIT) != 0
                setFlag(CY_BIT, !currentCarry)
                1
            }

            Instruction.CPL_BIT -> {
                state.pc++
                val bitAddr = state.rom[state.pc++]
                complementBit(bitAddr)
                1
            }

            else -> 1
        }
    }
}
