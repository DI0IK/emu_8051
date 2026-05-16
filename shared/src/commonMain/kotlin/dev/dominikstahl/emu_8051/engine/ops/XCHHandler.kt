package dev.dominikstahl.emu_8051.engine.ops

import dev.dominikstahl.emu_8051.engine.CpuState
import dev.dominikstahl.emu_8051.engine.Instruction

class XCHHandler(state: CpuState) : InstructionHandler(state) {
    override val handledInstructions: Set<Instruction> = setOf(
        Instruction.XCH_A_DIRECT,
        Instruction.XCH_A_RI, Instruction.XCH_A_RN,
        Instruction.XCHD_A_RI,
    )

    override fun handle(instruction: Instruction, rawOpcode: Int): Int {
        val oldAcc = state.ACC

        when (instruction) {
            Instruction.XCH_A_DIRECT -> {
                state.pc++
                val addr = state.rom[state.pc++]
                val value = state.readPin(addr)
                state.ACC = value
                state.writeDirect(addr, oldAcc)
            }

            Instruction.XCH_A_RI -> {
                state.pc++
                val pointer = getRegister(rawOpcode and 0x01)
                val value = state.readIndirect(pointer)
                state.ACC = value
                state.writeIndirect(pointer, oldAcc)
            }

            Instruction.XCH_A_RN -> {
                state.pc++
                val regIndex = rawOpcode and 0x07
                val value = getRegister(regIndex)
                state.ACC = value
                setRegister(regIndex, oldAcc)
            }

            Instruction.XCHD_A_RI -> {
                state.pc++
                val pointer = getRegister(rawOpcode and 0x01)
                val memValue = state.readIndirect(pointer).toInt()
                val accValue = oldAcc.toInt()

                val newAcc = (accValue and 0xF0) or (memValue and 0x0F)
                val newMem = (memValue and 0xF0) or (accValue and 0x0F)

                state.ACC = newAcc.toUByte()
                state.writeIndirect(pointer, newMem.toUByte())
            }

            else -> {}
        }

        return 1
    }
}
