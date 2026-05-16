package dev.dominikstahl.emu_8051.engine.ops

import dev.dominikstahl.emu_8051.engine.CpuState
import dev.dominikstahl.emu_8051.engine.Instruction

class INCHandler(state: CpuState) : InstructionHandler(state) {
    override val handledInstructions: Set<Instruction> = setOf(
        Instruction.INC_A, Instruction.INC_DIRECT,
        Instruction.INC_RI, Instruction.INC_RN,
        Instruction.INC_DPTR,
    )

    override fun handle(instruction: Instruction, rawOpcode: Int): Int {
        return when (instruction) {
            Instruction.INC_A -> {
                state.pc++
                state.ACC = (state.ACC + 1u).toUByte()
                1
            }

            Instruction.INC_DIRECT -> {
                state.pc++
                val addr = state.rom[state.pc++]
                state.writeDirect(addr, (state.readDirect(addr) + 1u).toUByte())
                1
            }

            Instruction.INC_RI -> {
                state.pc++
                val regIndex = rawOpcode and 0x01
                val pointer = getRegister(regIndex)
                state.writeIndirect(pointer, (state.readIndirect(pointer) + 1u).toUByte())
                1
            }

            Instruction.INC_RN -> {
                state.pc++
                val regIndex = rawOpcode and 0x07
                val value = getRegister(regIndex)
                setRegister(regIndex, (value + 1u).toUByte())
                1
            }

            Instruction.INC_DPTR -> {
                state.pc++
                state.DPTR = (state.DPTR + 1) and 0xFFFF
                2
            }

            else -> 1
        }
    }
}
