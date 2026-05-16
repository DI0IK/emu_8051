package dev.dominikstahl.emu_8051.engine.ops

import dev.dominikstahl.emu_8051.engine.CpuState
import dev.dominikstahl.emu_8051.engine.Instruction

abstract class InstructionHandler(val state: CpuState) {
    abstract val handledInstructions: Set<Instruction>
    abstract fun handle(instruction: Instruction, rawOpcode: Int): Int

    @OptIn(ExperimentalUnsignedTypes::class)
    protected fun pushStack(value: UByte) {
        state.SP = (state.SP + 1u).toUByte()
        state.ram[state.SP.toInt()] = value
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    protected fun popStack(): UByte {
        // Guard against stack underflow (SP should always be >= 0x08 when pop is valid)
        if (state.SP < 0x08u) return 0u
        val value = state.ram[state.SP.toInt()]
        state.SP = (state.SP - 1u).toUByte()
        return value
    }

    protected fun setFlag(bitMask: Int, value: Boolean) {
        if (value) {
            state.PSW = (state.PSW.toInt() or bitMask).toUByte()
        } else {
            state.PSW = (state.PSW.toInt() and bitMask.inv()).toUByte()
        }
    }

    /**
     * Accesses R0-R7 based on the currently selected register bank in PSW.
     */
    protected fun getRegister(index: Int): UByte {
        val bank = (state.PSW.toInt() shr 3) and 0x03
        val address = (bank * 8 + index).toUByte()
        return state.readDirect(address)
    }

    /**
     * Sets R0-R7 based on the currently selected register bank in PSW.
     */
    protected fun setRegister(index: Int, value: UByte) {
        val bank = (state.PSW.toInt() shr 3) and 0x03
        val address = (bank * 8 + index).toUByte()
        state.writeDirect(address, value)
    }

    /**
     * Reads a single bit from the bit-addressable area.
     * Addresses 0x00-0x7F map to Internal RAM (byte 0x20-0x2F).
     * Addresses 0x80-0xFF map to SFRs (only those ending in 0x00 or 0x08).
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    protected fun getBit(bitAddress: UByte): Boolean {
        val addr = bitAddress.toInt()
        return if (addr < 0x80) {
            val byteAddr = 0x20 + (addr / 8)
            val bitPos = addr % 8
            ((state.ram[byteAddr].toInt() shr bitPos) and 0x01) != 0
        } else {
            val byteAddr = (addr and 0xF8).toUByte()
            val bitPos = addr and 0x07
            ((state.readPin(byteAddr).toInt() shr bitPos) and 0x01) != 0
        }
    }

    /**
     * Sets or clears a single bit in the bit-addressable area.
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    protected fun setBit(bitAddress: UByte, value: Boolean) {
        val addr = bitAddress.toInt()
        if (addr < 0x80) {
            val byteAddr = 0x20 + (addr / 8)
            val bitPos = addr % 8
            val current = state.ram[byteAddr].toInt()
            state.ram[byteAddr] = if (value) (current or (1 shl bitPos)).toUByte()
            else (current and (1 shl bitPos).inv()).toUByte()
        } else {
            val byteAddr = (addr and 0xF8).toUByte()
            val bitPos = addr and 0x07
            val current = state.readDirect(byteAddr).toInt()
            val newVal = if (value) (current or (1 shl bitPos)).toUByte()
            else (current and (1 shl bitPos).inv()).toUByte()
            state.writeDirect(byteAddr, newVal)
        }
    }

    /**
     * Complements (toggles) a single bit in the bit-addressable area.
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    protected fun complementBit(bitAddress: UByte) {
        val addr = bitAddress.toInt()
        if (addr < 0x80) {
            val byteAddr = 0x20 + (addr / 8)
            val bitPos = addr % 8
            val current = state.ram[byteAddr].toInt()
            state.ram[byteAddr] = (current xor (1 shl bitPos)).toUByte()
        } else {
            val byteAddr = (addr and 0xF8).toUByte()
            val bitPos = addr and 0x07
            val current = state.readDirect(byteAddr).toInt()
            state.writeDirect(byteAddr, (current xor (1 shl bitPos)).toUByte())
        }
    }

    companion object {
        // PSW bits
        const val CY_BIT = 0x80
        const val AC_BIT = 0x40
        const val F0_BIT = 0x20
        const val RS1_BIT = 0x10
        const val RS0_BIT = 0x08
        const val OV_BIT = 0x04
        const val F1_BIT = 0x02
        const val P_BIT = 0x01

        // TCON bits (0x88)
        const val TF1_BIT = 0x80
        const val TR1_BIT = 0x40
        const val TF0_BIT = 0x20
        const val TR0_BIT = 0x10
        const val IE1_BIT = 0x08
        const val IT1_BIT = 0x04
        const val IE0_BIT = 0x02
        const val IT0_BIT = 0x01

        // SCON bits (0x98)
        const val SM0_BIT = 0x80
        const val SM1_BIT = 0x40
        const val SM2_BIT = 0x20
        const val REN_BIT = 0x10
        const val TB8_BIT = 0x08
        const val RB8_BIT = 0x04
        const val TI_BIT = 0x02
        const val RI_BIT = 0x01

        // IE bits (0xA8)
        const val EA_BIT = 0x80
        const val ET2_BIT = 0x20
        const val ES_BIT = 0x10
        const val ET1_BIT = 0x08
        const val EX1_BIT = 0x04
        const val ET0_BIT = 0x02
        const val EX0_BIT = 0x01

        // IP bits (0xB8)
        const val PT2_BIT = 0x20
        const val PS_BIT = 0x10
        const val PT1_BIT = 0x08
        const val PX1_BIT = 0x04
        const val PT0_BIT = 0x02
        const val PX0_BIT = 0x01

        // T2CON bits (0xC8)
        const val TF2_BIT = 0x80
        const val EXF2_BIT = 0x40
        const val RCLK_BIT = 0x20
        const val TCLK_BIT = 0x10
        const val EXEN2_BIT = 0x08
        const val TR2_BIT = 0x04
        const val CT2_BIT = 0x02
        const val CPRL2_BIT = 0x01

        // TMOD bits (0x89)
        const val GATE1_BIT = 0x80
        const val CT1_BIT = 0x40
        const val M11_BIT = 0x20
        const val M01_BIT = 0x10
        const val GATE0_BIT = 0x08
        const val CT0_BIT = 0x04
        const val M10_BIT = 0x02
        const val M00_BIT = 0x01

        // T2MOD bits (0xC9)
        const val T2OE_BIT = 0x02
        const val DCEN_BIT = 0x01

        // SFR addresses
        const val TCON_ADDR = 0x88
        const val TMOD_ADDR = 0x89
        const val IE_ADDR = 0xA8
        const val IP_ADDR = 0xB8
        const val SCON_ADDR = 0x98
        const val T2CON_ADDR = 0xC8
        const val T2MOD_ADDR = 0xC9
    }
}