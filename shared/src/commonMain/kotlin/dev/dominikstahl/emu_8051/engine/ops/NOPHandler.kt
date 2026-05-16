package dev.dominikstahl.emu_8051.engine.ops

import dev.dominikstahl.emu_8051.engine.CpuState
import dev.dominikstahl.emu_8051.engine.Instruction

class NOPHandler(state: CpuState) : InstructionHandler(state) {
    override val handledInstructions: Set<Instruction> = setOf(Instruction.NOP)

    override fun handle(instruction: Instruction, rawOpcode: Int): Int {
        state.pc++
        return 1
    }
}
