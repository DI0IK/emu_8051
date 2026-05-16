package dev.dominikstahl.emu_8051.engine.ops

import dev.dominikstahl.emu_8051.engine.CpuState
import dev.dominikstahl.emu_8051.engine.Instruction

class CJNEHandler(state: CpuState) : InstructionHandler(state) {
    override val handledInstructions: Set<Instruction> = setOf(
        Instruction.CJNE_A_IMM, Instruction.CJNE_A_DIRECT,
        Instruction.CJNE_RI_IMM, Instruction.CJNE_RN_IMM,
    )

    override fun handle(instruction: Instruction, rawOpcode: Int): Int {
        val destValue: Int
        val srcValue: Int

        state.pc++

        when (instruction) {
            Instruction.CJNE_A_DIRECT -> {
                destValue = state.ACC.toInt()
                val addr = state.rom[state.pc++]
                srcValue = state.readPin(addr).toInt()
            }

            Instruction.CJNE_A_IMM -> {
                destValue = state.ACC.toInt()
                srcValue = state.rom[state.pc++].toInt()
            }

            Instruction.CJNE_RN_IMM -> {
                destValue = getRegister(rawOpcode and 0x07).toInt()
                srcValue = state.rom[state.pc++].toInt()
            }

            Instruction.CJNE_RI_IMM -> {
                val pointer = getRegister(rawOpcode and 0x01)
                destValue = state.readIndirect(pointer).toInt()
                srcValue = state.rom[state.pc++].toInt()
            }

            else -> return 1
        }

        val relOffset = state.rom[state.pc++].toByte().toInt()
        val nextInstructionAddr = state.pc

        setFlag(CY_BIT, destValue < srcValue)

        if (destValue != srcValue) {
            state.pc = (nextInstructionAddr + relOffset) and 0xFFFF
        }

        return 2
    }
}
