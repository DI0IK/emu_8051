package dev.dominikstahl.emu_8051.engine.ops

import dev.dominikstahl.emu_8051.engine.CpuState
import dev.dominikstahl.emu_8051.engine.Instruction

class AJMPHandler(state: CpuState) : InstructionHandler(state) {
    override val handledInstructions: Set<Instruction> = setOf(Instruction.AJMP)

    override fun handle(instruction: Instruction, rawOpcode: Int): Int {
        state.pc++
        val addrLow = state.rom[state.pc].toInt()
        state.pc++

        val addrHigh3Bits = (rawOpcode shr 5) and 0x07
        val addr11 = (addrHigh3Bits shl 8) or addrLow

        val pcPage = state.pc and 0xF800
        state.pc = pcPage or addr11

        return 2
    }
}
