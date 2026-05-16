package dev.dominikstahl.emu_8051.engine.ops

import dev.dominikstahl.emu_8051.engine.CpuState
import dev.dominikstahl.emu_8051.engine.Instruction

class MULHandler(state: CpuState) : InstructionHandler(state) {
    override val handledInstructions: Set<Instruction> = setOf(Instruction.MUL_AB)

    override fun handle(instruction: Instruction, rawOpcode: Int): Int {
        state.pc++

        val a = state.ACC.toInt()
        val b = state.B.toInt()

        val product = a * b

        setFlag(CY_BIT, false)
        setFlag(OV_BIT, product > 0xFF)

        state.ACC = (product and 0xFF).toUByte()
        state.B = ((product shr 8) and 0xFF).toUByte()

        return 4
    }
}
