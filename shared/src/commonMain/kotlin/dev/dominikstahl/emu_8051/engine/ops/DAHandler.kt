package dev.dominikstahl.emu_8051.engine.ops

import dev.dominikstahl.emu_8051.engine.CpuState
import dev.dominikstahl.emu_8051.engine.Instruction

class DAHandler(state: CpuState) : InstructionHandler(state) {
    override val handledInstructions: Set<Instruction> = setOf(Instruction.DA_A)

    override fun handle(instruction: Instruction, rawOpcode: Int): Int {
        state.pc++

        var accValue = state.ACC.toInt()
        val psw = state.PSW.toInt()

        val initialCy = (psw and CY_BIT) != 0
        val initialAc = (psw and AC_BIT) != 0

        var carryOut = initialCy

        if ((accValue and 0x0F) > 0x09 || initialAc) {
            accValue += 0x06
        }

        if ((accValue shr 4) > 0x09 || initialCy) {
            accValue += 0x60
            carryOut = true
        }

        setFlag(CY_BIT, carryOut)
        state.ACC = (accValue and 0xFF).toUByte()

        return 1
    }
}
