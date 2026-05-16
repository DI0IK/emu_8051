package dev.dominikstahl.emu_8051.engine.ops

import dev.dominikstahl.emu_8051.engine.CpuState
import dev.dominikstahl.emu_8051.engine.Instruction

class DJNZHandler(state: CpuState) : InstructionHandler(state) {
    override val handledInstructions: Set<Instruction> = setOf(
        Instruction.DJNZ_DIRECT, Instruction.DJNZ_RN,
    )

    override fun handle(instruction: Instruction, rawOpcode: Int): Int {
        val newValue: Int
        val relOffset: Int

        when (instruction) {
            Instruction.DJNZ_RN -> {
                val regIndex = rawOpcode and 0x07
                state.pc++
                relOffset = state.rom[state.pc++].toByte().toInt()

                val value = getRegister(regIndex)
                val decValue = (value - 1u).toUByte()
                setRegister(regIndex, decValue)
                newValue = decValue.toInt()
            }

            Instruction.DJNZ_DIRECT -> {
                state.pc++
                val directAddr = state.rom[state.pc++]
                relOffset = state.rom[state.pc++].toByte().toInt()

                val value = state.readDirect(directAddr)
                val decValue = (value - 1u).toUByte()
                state.writeDirect(directAddr, decValue)
                newValue = decValue.toInt()
            }

            else -> return 1
        }

        if (newValue != 0) {
            state.pc = (state.pc + relOffset) and 0xFFFF
        }

        return 2
    }
}
