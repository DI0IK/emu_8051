package dev.dominikstahl.emu_8051.engine.ops

import dev.dominikstahl.emu_8051.engine.CpuState
import dev.dominikstahl.emu_8051.engine.Instruction

class RotateHandler(state: CpuState) : InstructionHandler(state) {
    override val handledInstructions: Set<Instruction> = setOf(
        Instruction.RR_A, Instruction.RRC_A,
        Instruction.RL_A, Instruction.RLC_A,
    )

    override fun handle(instruction: Instruction, rawOpcode: Int): Int {
        state.pc++

        when (instruction) {
            Instruction.RR_A -> {
                val acc = state.ACC.toInt()
                val bit0 = acc and 0x01
                state.ACC = (((acc shr 1) or (bit0 shl 7)) and 0xFF).toUByte()
            }

            Instruction.RRC_A -> {
                val acc = state.ACC.toInt()
                val cy = if ((state.PSW.toInt() and CY_BIT) != 0) 1 else 0
                val bit0 = acc and 0x01
                state.ACC = (((acc shr 1) or (cy shl 7)) and 0xFF).toUByte()
                setFlag(CY_BIT, bit0 == 1)
            }

            Instruction.RL_A -> {
                val acc = state.ACC.toInt()
                val bit7 = (acc shr 7) and 0x01
                state.ACC = (((acc shl 1) or bit7) and 0xFF).toUByte()
            }

            Instruction.RLC_A -> {
                val acc = state.ACC.toInt()
                val cy = if ((state.PSW.toInt() and CY_BIT) != 0) 1 else 0
                val bit7 = (acc shr 7) and 0x01
                state.ACC = (((acc shl 1) or cy) and 0xFF).toUByte()
                setFlag(CY_BIT, bit7 == 1)
            }

            else -> {}
        }

        return 1
    }
}
