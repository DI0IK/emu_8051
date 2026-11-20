package dev.dominikstahl.emu_8051.engine

private data class InterruptSource(
    val index: Int,
    val vector: Int,
    val flagMask: Int,
    val flagAddr: Int,
    val enableMask: Int,
    val enableAddr: Int,
    val priorityMask: Int,
    val priorityAddr: Int,
    val autoClearEdge: Boolean,
)

@OptIn(ExperimentalUnsignedTypes::class)
class InterruptController(private val state: CpuState) {

    private var lowPriorityInService: Boolean = false
    private var highPriorityInService: Boolean = false

    // P3 resets to 0xFF (high), so active-low external interrupts start inactive (false)
    private var prevInt0Low: Boolean = false
    private var prevInt1Low: Boolean = false

    var lastInterruptCycles: Int = 0

    private val sources = listOf(
        InterruptSource(0, 0x0003, IE0_BIT, TCON_ADDR,
            EX0_BIT, IE_ADDR,
            PX0_BIT, IP_ADDR, true),
        InterruptSource(1, 0x000B, TF0_BIT, TCON_ADDR,
            ET0_BIT, IE_ADDR,
            PT0_BIT, IP_ADDR, true),
        InterruptSource(2, 0x0013, IE1_BIT, TCON_ADDR,
            EX1_BIT, IE_ADDR,
            PX1_BIT, IP_ADDR, true),
        InterruptSource(3, 0x001B, TF1_BIT, TCON_ADDR,
            ET1_BIT, IE_ADDR,
            PT1_BIT, IP_ADDR, true),
        InterruptSource(4, 0x0023, TI_BIT or RI_BIT,
            SCON_ADDR,
            ES_BIT, IE_ADDR,
            PS_BIT, IP_ADDR, false),
        InterruptSource(5, 0x002B, TF2_BIT or EXF2_BIT,
            T2CON_ADDR,
            ET2_BIT, IE_ADDR,
            PT2_BIT, IP_ADDR, false),
    )

    /** Sample external interrupt pins (active low) and latch edge-triggered flags. Call once per machine cycle. */
    fun sample() {
        var tcon = state.TCON.toInt()
        val p3 = state.getEffectivePort(3)
        val int0Low = (p3 and 0x04) == 0
        val int1Low = (p3 and 0x08) == 0

        if ((tcon and IT0_BIT) != 0) {
            if (!prevInt0Low && int0Low) {
                tcon = tcon or IE0_BIT
            }
        } else {
            tcon = if (int0Low) tcon or IE0_BIT else tcon and IE0_BIT.inv()
        }

        if ((tcon and IT1_BIT) != 0) {
            if (!prevInt1Low && int1Low) {
                tcon = tcon or IE1_BIT
            }
        } else {
            tcon = if (int1Low) tcon or IE1_BIT else tcon and IE1_BIT.inv()
        }

        state.TCON = (tcon and 0xFF).toUByte()
        prevInt0Low = int0Low
        prevInt1Low = int1Low
    }

    /** Poll for pending interrupts respecting two-level priority and natural polling order. */
    fun poll(): Int {
        lastInterruptCycles = 0

        if ((state.IE.toInt() and EA_BIT) == 0) return 0
        if (highPriorityInService) return 0

        // Pass 1: high-priority interrupts (can preempt low-priority ISRs)
        val highCandidate = findPendingSource(wantHighPriority = true)
        if (highCandidate != null) {
            acknowledge(highCandidate, isHighPriority = true)
            lastInterruptCycles = 2
            return 2
        }

        // Pass 2: low-priority interrupts (blocked if either high or low ISR is active)
        if (!lowPriorityInService) {
            val lowCandidate = findPendingSource(wantHighPriority = false)
            if (lowCandidate != null) {
                acknowledge(lowCandidate, isHighPriority = false)
                lastInterruptCycles = 2
                return 2
            }
        }

        return 0
    }

    private fun findPendingSource(wantHighPriority: Boolean): InterruptSource? {
        for (source in sources) {
            val flagReg = readSFR(source.flagAddr).toInt()
            if ((flagReg and source.flagMask) == 0) continue

            val enableReg = readSFR(source.enableAddr).toInt()
            if ((enableReg and source.enableMask) == 0) continue

            val ipReg = readSFR(source.priorityAddr).toInt()
            val isHigh = (ipReg and source.priorityMask) != 0
            if (isHigh == wantHighPriority) return source
        }
        return null
    }

    /** Clear in-service flag for the highest priority level (called by RETI). */
    fun clearInService() {
        when {
            highPriorityInService -> highPriorityInService = false
            lowPriorityInService -> lowPriorityInService = false
        }
    }

    fun reset() {
        lowPriorityInService = false
        highPriorityInService = false
        prevInt0Low = false
        prevInt1Low = false
        lastInterruptCycles = 0
    }

    private fun acknowledge(source: InterruptSource, isHighPriority: Boolean) {
        if (isHighPriority) highPriorityInService = true
        else lowPriorityInService = true

        val pc = state.pc

        state.SP = (state.SP + 1u).toUByte()
        state.ram[state.SP.toInt()] = (pc and 0xFF).toUByte()

        state.SP = (state.SP + 1u).toUByte()
        state.ram[state.SP.toInt()] = ((pc shr 8) and 0xFF).toUByte()

        state.pc = source.vector

        if (source.autoClearEdge) {
            when (source.index) {
                1, 3 -> {
                    state.TCON = ((state.TCON.toInt() and source.flagMask.inv()) and 0xFF).toUByte()
                }
                0, 2 -> {
                    val isEdge = if (source.index == 0) (state.TCON.toInt() and IT0_BIT) != 0
                    else (state.TCON.toInt() and IT1_BIT) != 0
                    if (isEdge) {
                        state.TCON = ((state.TCON.toInt() and source.flagMask.inv()) and 0xFF).toUByte()
                    }
                }
            }
        }
    }

    private fun readSFR(addr: Int): UByte = state.sfr[addr - 0x80]
}
