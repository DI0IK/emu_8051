package dev.dominikstahl.emu_8051.engine.ops

import dev.dominikstahl.emu_8051.engine.CpuState
import dev.dominikstahl.emu_8051.engine.Instruction

class DECHandler(state: CpuState) : InstructionHandler(state) {
    override val handledInstructions: Set<Instruction> = setOf(
        Instruction.DEC_A, Instruction.DEC_DIRECT,
        Instruction.DEC_RI, Instruction.DEC_RN,
    )

    override fun handle(instruction: Instruction, rawOpcode: Int): Int {
        when (instruction) {
            Instruction.DEC_A -> {
                state.pc++
                state.ACC = (state.ACC - 1u).toUByte()
            }

            Instruction.DEC_DIRECT -> {
                state.pc++
                val addr = state.rom[state.pc++]
                state.writeDirect(addr, (state.readDirect(addr) - 1u).toUByte())
            }

            Instruction.DEC_RI -> {
                state.pc++
                val regIndex = rawOpcode and 0x01
                val pointer = getRegister(regIndex)
                state.writeIndirect(pointer, (state.readIndirect(pointer) - 1u).toUByte())
            }

            Instruction.DEC_RN -> {
                state.pc++
                val regIndex = rawOpcode and 0x07
                val value = getRegister(regIndex)
                setRegister(regIndex, (value - 1u).toUByte())
            }

            else -> {}
        }

        return 1
    }
}
