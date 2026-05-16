package dev.dominikstahl.emu_8051.engine.ops

import dev.dominikstahl.emu_8051.engine.CpuState
import dev.dominikstahl.emu_8051.engine.Instruction

class SWAPHandler(state: CpuState) : InstructionHandler(state) {
    override val handledInstructions: Set<Instruction> = setOf(Instruction.SWAP_A)

    override fun handle(instruction: Instruction, rawOpcode: Int): Int {
        state.pc++

        val acc = state.ACC.toInt()
        val lowNibble = acc and 0x0F
        val highNibble = acc and 0xF0
        state.ACC = ((lowNibble shl 4) or (highNibble shr 4)).toUByte()

        return 1
    }
}
