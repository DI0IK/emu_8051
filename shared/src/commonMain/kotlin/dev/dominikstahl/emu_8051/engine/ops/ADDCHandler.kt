package dev.dominikstahl.emu_8051.engine.ops

import dev.dominikstahl.emu_8051.engine.CpuState
import dev.dominikstahl.emu_8051.engine.Instruction

class ADDCHandler(state: CpuState) : InstructionHandler(state) {
    override val handledInstructions: Set<Instruction> = setOf(
        Instruction.ADD_A_IMM, Instruction.ADD_A_DIRECT,
        Instruction.ADD_A_RI, Instruction.ADD_A_RN,
        Instruction.ADDC_A_IMM, Instruction.ADDC_A_DIRECT,
        Instruction.ADDC_A_RI, Instruction.ADDC_A_RN,
    )

    override fun handle(instruction: Instruction, rawOpcode: Int): Int {
        val isWithCarry = instruction in setOf(
            Instruction.ADDC_A_IMM, Instruction.ADDC_A_DIRECT,
            Instruction.ADDC_A_RI, Instruction.ADDC_A_RN,
        )

        val srcValue: UByte = when (instruction) {
            Instruction.ADD_A_RN, Instruction.ADDC_A_RN -> {
                state.pc++
                getRegister(rawOpcode and 0x07)
            }

            Instruction.ADD_A_DIRECT, Instruction.ADDC_A_DIRECT -> {
                state.pc++
                state.readPin(state.rom[state.pc++])
            }

            Instruction.ADD_A_RI, Instruction.ADDC_A_RI -> {
                state.pc++
                val regIndex = rawOpcode and 0x01
                val pointer = getRegister(regIndex)
                state.readIndirect(pointer)
            }

            Instruction.ADD_A_IMM, Instruction.ADDC_A_IMM -> {
                state.pc++
                state.rom[state.pc++]
            }

            else -> return 1
        }

        performAddLogic(srcValue, isWithCarry)
        return 1
    }

    private fun performAddLogic(src: UByte, includeCarry: Boolean) {
        val a = state.ACC.toInt()
        val b = src.toInt()
        val cyIn = if (includeCarry && (state.PSW.toInt() and CY_BIT) != 0) 1 else 0

        val result = a + b + cyIn
        val acResult = (a and 0x0F) + (b and 0x0F) + cyIn
        val carry6 = ((a and 0x7F) + (b and 0x7F) + cyIn) shr 7
        val carry7 = (result shr 8) and 0x01

        setFlag(CY_BIT, carry7 != 0)
        setFlag(AC_BIT, (acResult shr 4) != 0)
        setFlag(OV_BIT, (carry6 xor carry7) != 0)

        state.ACC = (result and 0xFF).toUByte()
    }
}
