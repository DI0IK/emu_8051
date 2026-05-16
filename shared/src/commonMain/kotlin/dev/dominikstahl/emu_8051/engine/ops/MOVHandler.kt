package dev.dominikstahl.emu_8051.engine.ops

import dev.dominikstahl.emu_8051.engine.CpuState
import dev.dominikstahl.emu_8051.engine.Instruction

@OptIn(ExperimentalUnsignedTypes::class)
class MOVHandler(state: CpuState) : InstructionHandler(state) {
    override val handledInstructions: Set<Instruction> = setOf(
        Instruction.MOV_A_IMM, Instruction.MOV_A_DIRECT,
        Instruction.MOV_A_RI, Instruction.MOV_A_RN,
        Instruction.MOV_RN_A, Instruction.MOV_RN_DIRECT, Instruction.MOV_RN_IMM,
        Instruction.MOV_DIRECT_A, Instruction.MOV_DIRECT_RN,
        Instruction.MOV_DIRECT_DIRECT, Instruction.MOV_DIRECT_RI, Instruction.MOV_DIRECT_IMM,
        Instruction.MOV_RI_A, Instruction.MOV_RI_DIRECT, Instruction.MOV_RI_IMM,
        Instruction.MOV_C_BIT, Instruction.MOV_BIT_C,
        Instruction.MOV_DPTR_IMM16,
        Instruction.MOVC_A_AT_A_DPTR, Instruction.MOVC_A_AT_A_PC,
        Instruction.MOVX_A_AT_DPTR, Instruction.MOVX_A_RI,
        Instruction.MOVX_AT_DPTR_A, Instruction.MOVX_RI_A,
    )

    override fun handle(instruction: Instruction, rawOpcode: Int): Int {
        return when (instruction) {
            Instruction.MOVX_A_AT_DPTR -> {
                state.pc++
                state.ACC = state.xram[state.DPTR]
                2
            }

            Instruction.MOVX_A_RI -> {
                state.pc++
                val pointer = getRegister(rawOpcode and 0x01)
                state.ACC = state.xram[pointer.toInt()]
                2
            }

            Instruction.MOVX_AT_DPTR_A -> {
                state.pc++
                state.xram[state.DPTR] = state.ACC
                2
            }

            Instruction.MOVX_RI_A -> {
                state.pc++
                val pointer = getRegister(rawOpcode and 0x01)
                state.xram[pointer.toInt()] = state.ACC
                2
            }

            Instruction.MOVC_A_AT_A_DPTR -> {
                state.pc++
                val targetAddr = (state.ACC.toInt() + state.DPTR) and 0xFFFF
                state.ACC = state.rom[targetAddr]
                2
            }

            Instruction.MOVC_A_AT_A_PC -> {
                state.pc++
                val targetAddr = (state.ACC.toInt() + state.pc) and 0xFFFF
                state.ACC = state.rom[targetAddr]
                2
            }

            Instruction.MOV_A_IMM -> {
                state.pc++
                state.ACC = state.rom[state.pc++]
                1
            }

            Instruction.MOV_A_DIRECT -> {
                state.pc++
                val addr = state.rom[state.pc++]
                state.ACC = state.readPin(addr)
                1
            }

            Instruction.MOV_A_RI -> {
                state.pc++
                val pointer = getRegister(rawOpcode and 0x01)
                state.ACC = state.readIndirect(pointer)
                1
            }

            Instruction.MOV_A_RN -> {
                state.pc++
                state.ACC = getRegister(rawOpcode and 0x07)
                1
            }

            Instruction.MOV_RN_A -> {
                state.pc++
                setRegister(rawOpcode and 0x07, state.ACC)
                1
            }

            Instruction.MOV_RN_DIRECT -> {
                state.pc++
                val addr = state.rom[state.pc++]
                setRegister(rawOpcode and 0x07, state.readPin(addr))
                2
            }

            Instruction.MOV_RN_IMM -> {
                state.pc++
                val data = state.rom[state.pc++]
                setRegister(rawOpcode and 0x07, data)
                1
            }

            Instruction.MOV_DIRECT_A -> {
                state.pc++
                val addr = state.rom[state.pc++]
                state.writeDirect(addr, state.ACC)
                1
            }

            Instruction.MOV_DIRECT_RN -> {
                state.pc++
                val addr = state.rom[state.pc++]
                state.writeDirect(addr, getRegister(rawOpcode and 0x07))
                2
            }

            Instruction.MOV_DIRECT_DIRECT -> {
                state.pc++
                val srcAddr = state.rom[state.pc++]
                val destAddr = state.rom[state.pc++]
                state.writeDirect(destAddr, state.readPin(srcAddr))
                2
            }

            Instruction.MOV_DIRECT_RI -> {
                state.pc++
                val pointer = getRegister(rawOpcode and 0x01)
                val destAddr = state.rom[state.pc++]
                state.writeDirect(destAddr, state.readIndirect(pointer))
                2
            }

            Instruction.MOV_DIRECT_IMM -> {
                state.pc++
                val addr = state.rom[state.pc++]
                val data = state.rom[state.pc++]
                state.writeDirect(addr, data)
                2
            }

            Instruction.MOV_RI_A -> {
                state.pc++
                val pointer = getRegister(rawOpcode and 0x01)
                state.writeIndirect(pointer, state.ACC)
                1
            }

            Instruction.MOV_RI_DIRECT -> {
                state.pc++
                val pointer = getRegister(rawOpcode and 0x01)
                val addr = state.rom[state.pc++]
                state.writeIndirect(pointer, state.readPin(addr))
                2
            }

            Instruction.MOV_RI_IMM -> {
                state.pc++
                val pointer = getRegister(rawOpcode and 0x01)
                val data = state.rom[state.pc++]
                state.writeIndirect(pointer, data)
                1
            }

            Instruction.MOV_C_BIT -> {
                state.pc++
                val bitAddr = state.rom[state.pc++]
                setFlag(CY_BIT, getBit(bitAddr))
                1
            }

            Instruction.MOV_BIT_C -> {
                state.pc++
                val bitAddr = state.rom[state.pc++]
                val carry = (state.PSW.toInt() and CY_BIT) != 0
                setBit(bitAddr, carry)
                2
            }

            Instruction.MOV_DPTR_IMM16 -> {
                state.pc++
                val high = state.rom[state.pc++].toInt()
                val low = state.rom[state.pc++].toInt()
                state.DPTR = (high shl 8) or low
                2
            }

            else -> 1
        }
    }
}
