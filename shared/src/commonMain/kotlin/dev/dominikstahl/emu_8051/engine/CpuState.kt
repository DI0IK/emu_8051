package dev.dominikstahl.emu_8051.engine

@OptIn(ExperimentalUnsignedTypes::class)
class CpuState {
    /** Reference to the interrupt controller, set after construction. */
    var interruptController: InterruptController? = null

    /** Reference to the timer controller, set after construction. */
    var timerController: TimerController? = null

    /** Set to true when an instruction writes to IE (0xA8) or IP (0xB8). */
    var interruptControllerAccessFlag: Boolean = false

    /** Program memory (code space), 64KB. Read-only at runtime. */
    val rom = UByteArray(65536)

    /** Internal (on-chip) RAM, 256 bytes. Lower 128 bytes direct/indirect; upper 128 bytes indirect only. */
    val ram = UByteArray(256)

    /** External RAM (XRAM), 64KB. Accessed via MOVX instructions. */
    val xram = UByteArray(65536)

    /** Special Function Register space, 128 bytes (0x80-0xFF). Indexed as sfr[addr - 0x80]. */
    val sfr = UByteArray(128)

    /** Total cycles executed since last reset. */
    var totalCycles: Long = 0L

    /** External pin drive — bitmask of pins driven by virtual hardware per port index (0=P0..3=P3). */
    val externalDriven = IntArray(4)
    val externalValue = IntArray(4)

    /** Compute effective port value by merging SFR latch with external drive. */
    fun getEffectivePort(portIdx: Int): Int {
        val sfrIdx = portIdx * 16
        val sfrVal = sfr[sfrIdx].toInt()
        val driven = externalDriven[portIdx]
        return if (driven == 0) sfrVal
            else (sfrVal and driven.inv()) or (externalValue[portIdx] and driven)
    }

    // --- Port 0 (0x80) ---

    /** Port 0 (bit-addressable SFR at 0x80). Open-drain bidirectional I/O port. */
    var P0: UByte
        get() = sfr[0x80 - 0x80]
        set(value) { sfr[0x80 - 0x80] = value }

    // --- 0x81-0x87 ---

    /** Stack Pointer at 0x81. Holds the address of the last occupied stack byte. Default 0x07 on reset. */
    var SP: UByte
        get() = sfr[0x81 - 0x80]
        set(value) { sfr[0x81 - 0x80] = value }

    /** Data Pointer Low byte at 0x82. Low byte of the 16-bit DPTR. */
    var DPL: UByte
        get() = sfr[0x82 - 0x80]
        set(value) { sfr[0x82 - 0x80] = value }

    /** Data Pointer High byte at 0x83. High byte of the 16-bit DPTR. */
    var DPH: UByte
        get() = sfr[0x83 - 0x80]
        set(value) { sfr[0x83 - 0x80] = value }

    /** Full 16-bit Data Pointer (DPTR = DPH:DPL). Used for MOVC, MOVX, LJMP, LCALL, JMP @A+DPTR. */
    var DPTR: Int
        get() = (DPH.toInt() shl 8) or DPL.toInt()
        set(value) {
            DPH = (value shr 8).toUByte()
            DPL = (value and 0xFF).toUByte()
        }

    /** Power Control at 0x87. Bit 0 (IDL) = idle mode, bit 1 (PD) = power-down mode. */
    var PCON: UByte
        get() = sfr[0x87 - 0x80]
        set(value) { sfr[0x87 - 0x80] = value }

    // --- Timer/Counter Control (0x88-0x8F) ---

    /**
     * Timer/Counter Control at 0x88 (bit-addressable).
     * Bit 7 (TF1)  Timer 1 overflow flag
     * Bit 6 (TR1)  Timer 1 run control
     * Bit 5 (TF0)  Timer 0 overflow flag
     * Bit 4 (TR0)  Timer 0 run control
     * Bit 3 (IE1)  External interrupt 1 edge flag
     * Bit 2 (IT1)  External interrupt 1 type (edge/level)
     * Bit 1 (IE0)  External interrupt 0 edge flag
     * Bit 0 (IT0)  External interrupt 0 type (edge/level)
     */
    var TCON: UByte
        get() = sfr[0x88 - 0x80]
        set(value) { sfr[0x88 - 0x80] = value }

    /**
     * Timer/Counter Mode at 0x89.
     * Bit 7 (GATE1) Timer 1 gating (TR1 + INT1)
     * Bit 6 (CT1)   Timer 1 counter/timer select
     * Bit 5 (M11)   Timer 1 mode high bit
     * Bit 4 (M01)   Timer 1 mode low bit
     * Bit 3 (GATE0) Timer 0 gating
     * Bit 2 (CT0)   Timer 0 counter/timer select
     * Bit 1 (M10)   Timer 0 mode high bit
     * Bit 0 (M00)   Timer 0 mode low bit
     */
    var TMOD: UByte
        get() = sfr[0x89 - 0x80]
        set(value) { sfr[0x89 - 0x80] = value }

    /** Timer 0 low byte at 0x8A. */
    var TL0: UByte
        get() = sfr[0x8A - 0x80]
        set(value) { sfr[0x8A - 0x80] = value }

    /** Timer 1 low byte at 0x8B. */
    var TL1: UByte
        get() = sfr[0x8B - 0x80]
        set(value) { sfr[0x8B - 0x80] = value }

    /** Timer 0 high byte at 0x8C. */
    var TH0: UByte
        get() = sfr[0x8C - 0x80]
        set(value) { sfr[0x8C - 0x80] = value }

    /** Timer 1 high byte at 0x8D. */
    var TH1: UByte
        get() = sfr[0x8D - 0x80]
        set(value) { sfr[0x8D - 0x80] = value }

    /**
     * Auxiliary Register at 0x8E.
     * Bit 0 (RCLK)   Serial port receive clock source
     * Bit 1 (TCLK)   Serial port transmit clock source
     * Bit 2 (EXEN2)  Timer 2 external enable
     * Bit 3 (T2OE)   Timer 2 output enable (Atmel variant)
     * Bit 5 (M0)     External RAM timing (Atmel)
     * Bit 6 (A0)     XRAM addressing (Atmel, 256/64KB)
     * Bit 7 (INT2EN) Interrupt 2 enable (Atmel)
     */
    var AUXR: UByte
        get() = sfr[0x8E - 0x80]
        set(value) { sfr[0x8E - 0x80] = value }

    /** Clock Control at 0x8F. Selects divide-by ratio for MOVX and timer clocks (Atmel variant). */
    var CKCON: UByte
        get() = sfr[0x8F - 0x80]
        set(value) { sfr[0x8F - 0x80] = value }

    // --- Port 1 (0x90) ---

    /** Port 1 (bit-addressable SFR at 0x90). Bidirectional I/O port with internal pull-ups. */
    var P1: UByte
        get() = sfr[0x90 - 0x80]
        set(value) { sfr[0x90 - 0x80] = value }

    // --- Serial Interface (0x98-0x99) ---

    /**
     * Serial Control at 0x98 (bit-addressable).
     * Bit 7 (SM0)   Serial port mode bit 0
     * Bit 6 (SM1)   Serial port mode bit 1
     * Bit 5 (SM2)   Multiprocessor communication enable
     * Bit 4 (REN)   Receive enable
     * Bit 3 (TB8)   9th transmit bit
     * Bit 2 (RB8)   9th receive bit
     * Bit 1 (TI)    Transmit interrupt flag
     * Bit 0 (RI)    Receive interrupt flag
     */
    var SCON: UByte
        get() = sfr[0x98 - 0x80]
        set(value) { sfr[0x98 - 0x80] = value }

    /** Serial Data Buffer at 0x99. Two physically separate registers: transmit (write) and receive (read). */
    var SBUF: UByte
        get() = sfr[0x99 - 0x80]
        set(value) { sfr[0x99 - 0x80] = value }

    // --- Port 2 (0xA0) ---

    /** Port 2 (bit-addressable SFR at 0xA0). Bidirectional I/O port; emits high byte of address during external MOVX. */
    var P2: UByte
        get() = sfr[0xA0 - 0x80]
        set(value) { sfr[0xA0 - 0x80] = value }

    // --- Interrupt System (0xA8-0xB8) ---

    /**
     * Interrupt Enable at 0xA8 (bit-addressable).
     * Bit 7 (EA)    Global interrupt enable
     * Bit 6 (EC)    Timer 2 interrupt enable (Atmel)
     * Bit 5 (ET2)   Timer 2 interrupt enable (Intel)
     * Bit 4 (ES)    Serial port interrupt enable
     * Bit 3 (ET1)   Timer 1 interrupt enable
     * Bit 2 (EX1)   External interrupt 1 enable
     * Bit 1 (ET0)   Timer 0 interrupt enable
     * Bit 0 (EX0)   External interrupt 0 enable
     */
    var IE: UByte
        get() = sfr[0xA8 - 0x80]
        set(value) { sfr[0xA8 - 0x80] = value }

    // --- Port 3 (0xB0) ---

    /** Port 3 (bit-addressable SFR at 0xB0). Bidirectional I/O port with alternate functions on each pin. */
    var P3: UByte
        get() = sfr[0xB0 - 0x80]
        set(value) { sfr[0xB0 - 0x80] = value }

    /**
     * Interrupt Priority at 0xB8 (bit-addressable).
     * Bit 5 (PT2)  Timer 2 priority (Atmel)
     * Bit 4 (PS)   Serial port priority
     * Bit 3 (PT1)  Timer 1 priority
     * Bit 2 (PX1)  External interrupt 1 priority
     * Bit 1 (PT0)  Timer 0 priority
     * Bit 0 (PX0)  External interrupt 0 priority
     * 0 = low priority, 1 = high priority.
     */
    var IP: UByte
        get() = sfr[0xB8 - 0x80]
        set(value) { sfr[0xB8 - 0x80] = value }

    // --- Timer 2 (Atmel-specific, 0xC8-0xCD) ---

    /**
     * Timer 2 Control at 0xC8 (bit-addressable, Atmel variant).
     * Bit 7 (TF2)    Timer 2 overflow flag
     * Bit 6 (EXF2)   Timer 2 external flag
     * Bit 5 (RCLK)   Receive clock flag
     * Bit 4 (TCLK)   Transmit clock flag
     * Bit 3 (EXEN2)  Timer 2 external enable
     * Bit 2 (TR2)    Timer 2 run control
     * Bit 1 (CT2)    Timer 2 counter/timer select
     * Bit 0 (CPRL2)  Timer 2 capture/reload select
     */
    var T2CON: UByte
        get() = sfr[0xC8 - 0x80]
        set(value) { sfr[0xC8 - 0x80] = value }

    /** Timer 2 capture/reload low byte at 0xCA. */
    var RCAP2L: UByte
        get() = sfr[0xCA - 0x80]
        set(value) { sfr[0xCA - 0x80] = value }

    /** Timer 2 capture/reload high byte at 0xCB. */
    var RCAP2H: UByte
        get() = sfr[0xCB - 0x80]
        set(value) { sfr[0xCB - 0x80] = value }

    /** Timer 2 low byte at 0xCC. */
    var TL2: UByte
        get() = sfr[0xCC - 0x80]
        set(value) { sfr[0xCC - 0x80] = value }

    /** Timer 2 high byte at 0xCD. */
    var TH2: UByte
        get() = sfr[0xCD - 0x80]
        set(value) { sfr[0xCD - 0x80] = value }

    /** Timer 2 Mode Control at 0xC9. Bit 1 (T2OE) = clock-out enable, bit 0 (DCEN) = down-counter enable. */
    var T2MOD: UByte
        get() = sfr[0xC9 - 0x80]
        set(value) { sfr[0xC9 - 0x80] = value }

    // --- Program Status Word, ACC, B (0xD0-0xF0) ---

    /**
     * Program Status Word at 0xD0 (bit-addressable).
     *
     * Bit 7 (CY)  Carry flag. Set/cleared by arithmetic and bit operations.
     * Bit 6 (AC)  Auxiliary Carry flag (BCD operations).
     * Bit 5 (F0)  Flag 0. Available to the user for general purposes.
     * Bit 4 (RS1) Register bank select high bit.
     *             00 = Bank 0 (0x00-0x07), 01 = Bank 1 (0x08-0x0F),
     *             10 = Bank 2 (0x10-0x17), 11 = Bank 3 (0x18-0x1F).
     * Bit 3 (RS0) Register bank select low bit.
     * Bit 2 (OV)  Overflow flag. Set/cleared by arithmetic instructions.
     * Bit 1 (—)   Reserved (user-definable in some variants).
     * Bit 0 (P)   Parity flag. Set/cleared each instruction cycle to indicate
     *             odd/even number of 1-bits in the accumulator (even parity).
     */
    var PSW: UByte
        get() = sfr[0xD0 - 0x80]
        set(value) { sfr[0xD0 - 0x80] = value }

    /**
     * Accumulator at 0xE0 (bit-addressable). The primary working register for
     * arithmetic, logical, data movement, and shift operations.
     * Setting this value automatically updates the PSW parity bit.
     */
    var ACC: UByte
        get() = sfr[0xE0 - 0x80]
        set(value) { sfr[0xE0 - 0x80] = value; updateParity() }

    /**
     * B Register at 0xF0 (bit-addressable). Used as the second operand in
     * MUL AB and DIV AB instructions. General-purpose register otherwise.
     */
    var B: UByte
        get() = sfr[0xF0 - 0x80]
        set(value) { sfr[0xF0 - 0x80] = value }

    /** Program Counter. 16-bit address of the next instruction to execute. */
    var pc: Int = 0x0000

    /** Read a byte from internal RAM (0x00-0x7F) or SFR space (0x80-0xFF).
     *  Returns the raw SFR value (latch for port SFRs). Use readPin() to read the effective
     *  pin state (latch merged with external drive) for port SFRs (P0-P3). */
    fun readDirect(address: UByte): UByte {
        val addr = address.toInt()
        if (addr < 0x80) {
            return ram[addr]
        }
        return sfr[addr - 0x80]
    }

    /** Read a byte as a source operand. For port SFRs (P0-P3), returns the effective
     *  port value (latch merged with external drive). For all other addresses, delegates to readDirect(). */
    fun readPin(address: UByte): UByte {
        val addr = address.toInt()
        if (addr < 0x80) return readDirect(address)
        val portIdx = when (addr) {
            0x80 -> 0; 0x90 -> 1; 0xA0 -> 2; 0xB0 -> 3
            else -> -1
        }
        if (portIdx >= 0) return getEffectivePort(portIdx).toUByte()
        return sfr[addr - 0x80]
    }

    /** Write a byte to internal RAM (0x00-0x7F) or SFR space (0x80-0xFF).
     *  Writing to ACC (0xE0) automatically updates the parity flag.
     *  Writing to IE (0xA8) or IP (0xB8) sets the interrupt controller access flag. */
    fun writeDirect(address: UByte, value: UByte) {
        val addr = address.toInt()
        if (addr < 0x80) {
            ram[addr] = value
        } else {
            sfr[addr - 0x80] = value
            if (addr == 0xE0) updateParity()
            if (addr == 0xA8 || addr == 0xB8) interruptControllerAccessFlag = true
        }
    }

    /** Read a byte from internal RAM via register indirect addressing (@Ri).
     *  Always targets the 256-byte internal RAM regardless of address. */
    fun readIndirect(address: UByte): UByte {
        return ram[address.toInt()]
    }

    /** Write a byte to internal RAM via register indirect addressing (@Ri). */
    fun writeIndirect(address: UByte, value: UByte) {
        ram[address.toInt()] = value
    }

    /** Reset the CPU to its default state (same as hardware reset). */
    fun reset() {
        totalCycles = 0L
        interruptControllerAccessFlag = false
        interruptController?.reset()
        timerController?.reset()
        pc = 0x0000
        SP = 0x07u      // Stack starts at 0x07, grows to 0x08+

        P0 = 0xFFu
        P1 = 0xFFu
        P2 = 0xFFu
        P3 = 0xFFu

        ACC = 0x00u
        B = 0x00u
        PSW = 0x00u
        DPTR = 0x0000

        externalDriven.fill(0)
        externalValue.fill(0)

        updateParity()
    }

    /** Recalculate the PSW parity bit (P) based on the number of 1-bits in ACC.
     *  P is set (odd) when the count of 1-bits is odd; cleared (even) when even. */
    private fun updateParity() {
        val ones = ACC.toInt().countOneBits()
        PSW = if (ones % 2 != 0) {
            PSW or 0x01u
        } else {
            PSW and 0xFEu
        }
    }
}
