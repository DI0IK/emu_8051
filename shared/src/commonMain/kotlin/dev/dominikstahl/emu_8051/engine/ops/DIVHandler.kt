package dev.dominikstahl.emu_8051.engine.ops

import dev.dominikstahl.emu_8051.engine.CpuState
import dev.dominikstahl.emu_8051.engine.Instruction

class DIVHandler(state: CpuState) : InstructionHandler(state) {
    override val handledInstructions: Set<Instruction> = setOf(Instruction.DIV_AB)

    override fun handle(instruction: Instruction, rawOpcode: Int): Int {
        state.pc++

        val a = state.ACC.toInt()
        val b = state.B.toInt()

        setFlag(CY_BIT, false)

        if (b == 0) {
            setFlag(OV_BIT, true)
        } else {
            val quotient = a / b
            val remainder = a % b

            state.ACC = quotient.toUByte()
            state.B = remainder.toUByte()

            setFlag(OV_BIT, false)
        }

        return 4
    }
}
