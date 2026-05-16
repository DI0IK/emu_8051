package dev.dominikstahl.emu_8051.engine.ops

import dev.dominikstahl.emu_8051.engine.CpuState
import dev.dominikstahl.emu_8051.engine.Instruction

class SUBBHandler(state: CpuState) : InstructionHandler(state) {
    override val handledInstructions: Set<Instruction> = setOf(
        Instruction.SUBB_A_IMM, Instruction.SUBB_A_DIRECT,
        Instruction.SUBB_A_RI, Instruction.SUBB_A_RN,
    )

    override fun handle(instruction: Instruction, rawOpcode: Int): Int {
        val srcValue: UByte = when (instruction) {
            Instruction.SUBB_A_RN -> {
                state.pc++
                getRegister(rawOpcode and 0x07)
            }

            Instruction.SUBB_A_DIRECT -> {
                state.pc++
                state.readPin(state.rom[state.pc++])
            }

            Instruction.SUBB_A_RI -> {
                state.pc++
                val pointer = getRegister(rawOpcode and 0x01)
                state.readIndirect(pointer)
            }

            Instruction.SUBB_A_IMM -> {
                state.pc++
                state.rom[state.pc++]
            }

            else -> 0u
        }

        performSubbLogic(srcValue)
        return 1
    }

    private fun performSubbLogic(src: UByte) {
        val a = state.ACC.toInt()
        val b = src.toInt()
        val cyIn = if ((state.PSW.toInt() and CY_BIT) != 0) 1 else 0

        val result = a - b - cyIn
        val acResult = (a and 0x0F) - (b and 0x0F) - cyIn
        val borrow6 = if (((a and 0x7F) - (b and 0x7F) - cyIn) < 0) 1 else 0
        val borrow7 = if (result < 0) 1 else 0

        setFlag(CY_BIT, result < 0)
        setFlag(AC_BIT, acResult < 0)
        setFlag(OV_BIT, (borrow6 xor borrow7) != 0)

        state.ACC = (result and 0xFF).toUByte()
    }
}
