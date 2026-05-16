package dev.dominikstahl.emu_8051.engine.ops

import dev.dominikstahl.emu_8051.engine.CpuState
import dev.dominikstahl.emu_8051.engine.Instruction

class SJMPHandler(state: CpuState) : InstructionHandler(state) {
    override val handledInstructions: Set<Instruction> = setOf(Instruction.SJMP)

    override fun handle(instruction: Instruction, rawOpcode: Int): Int {
        state.pc++
        val relOffset = state.rom[state.pc].toByte().toInt()
        state.pc++
        state.pc = (state.pc + relOffset) and 0xFFFF

        return 2
    }
}
