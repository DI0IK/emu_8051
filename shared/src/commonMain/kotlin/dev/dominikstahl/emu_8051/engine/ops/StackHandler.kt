package dev.dominikstahl.emu_8051.engine.ops

import dev.dominikstahl.emu_8051.engine.CpuState
import dev.dominikstahl.emu_8051.engine.Instruction

@OptIn(ExperimentalUnsignedTypes::class)
class StackHandler(state: CpuState) : InstructionHandler(state) {
    override val handledInstructions: Set<Instruction> = setOf(
        Instruction.ACALL, Instruction.LCALL,
        Instruction.RET, Instruction.RETI,
        Instruction.PUSH_DIRECT, Instruction.POP_DIRECT,
    )

    override fun handle(instruction: Instruction, rawOpcode: Int): Int {
        return when (instruction) {
            Instruction.ACALL -> {
                state.pc++
                val addrLow = state.rom[state.pc].toInt()
                state.pc++

                val returnAddr = state.pc
                pushStack((returnAddr and 0xFF).toUByte())
                pushStack(((returnAddr shr 8) and 0xFF).toUByte())

                val addrHigh3Bits = (rawOpcode shr 5) and 0x07
                val addr11 = (addrHigh3Bits shl 8) or addrLow
                val pcPage = state.pc and 0xF800
                state.pc = pcPage or addr11
                2
            }

            Instruction.LCALL -> {
                val returnAddr = state.pc + 3
                state.pc++
                val addrHigh = state.rom[state.pc++].toInt()
                val addrLow = state.rom[state.pc].toInt()

                pushStack((returnAddr and 0xFF).toUByte())
                pushStack(((returnAddr shr 8) and 0xFF).toUByte())

                state.pc = (addrHigh shl 8) or addrLow
                2
            }

            Instruction.RET -> {
                val high = popStack().toInt()
                val low = popStack().toInt()

                state.pc = (high shl 8) or low
                2
            }

            Instruction.RETI -> {
                val high = popStack().toInt()
                val low = popStack().toInt()

                state.pc = (high shl 8) or low

                state.interruptController?.clearInService()
                2
            }

            Instruction.PUSH_DIRECT -> {
                state.pc++
                val addr = state.rom[state.pc++]
                val value = state.readPin(addr)
                pushStack(value)
                2
            }

            Instruction.POP_DIRECT -> {
                state.pc++
                val addr = state.rom[state.pc++]
                val value = popStack()
                state.writeDirect(addr, value)
                2
            }

            else -> 1
        }
    }
}
