package dev.dominikstahl.emu_8051.engine

import dev.dominikstahl.emu_8051.engine.ops.InstructionHandler

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

    private var prevInt0: Boolean = false
    private var prevInt1: Boolean = false

    var lastInterruptCycles: Int = 0

    private val sources = listOf(
        InterruptSource(0, 0x0003, InstructionHandler.IE0_BIT, InstructionHandler.TCON_ADDR,
            InstructionHandler.EX0_BIT, InstructionHandler.IE_ADDR,
            InstructionHandler.PX0_BIT, InstructionHandler.IP_ADDR, true),
        InterruptSource(1, 0x000B, InstructionHandler.TF0_BIT, InstructionHandler.TCON_ADDR,
            InstructionHandler.ET0_BIT, InstructionHandler.IE_ADDR,
            InstructionHandler.PT0_BIT, InstructionHandler.IP_ADDR, false),
        InterruptSource(2, 0x0013, InstructionHandler.IE1_BIT, InstructionHandler.TCON_ADDR,
            InstructionHandler.EX1_BIT, InstructionHandler.IE_ADDR,
            InstructionHandler.PX1_BIT, InstructionHandler.IP_ADDR, true),
        InterruptSource(3, 0x001B, InstructionHandler.TF1_BIT, InstructionHandler.TCON_ADDR,
            InstructionHandler.ET1_BIT, InstructionHandler.IE_ADDR,
            InstructionHandler.PT1_BIT, InstructionHandler.IP_ADDR, false),
        InterruptSource(4, 0x0023, InstructionHandler.TI_BIT or InstructionHandler.RI_BIT,
            InstructionHandler.SCON_ADDR,
            InstructionHandler.ES_BIT, InstructionHandler.IE_ADDR,
            InstructionHandler.PS_BIT, InstructionHandler.IP_ADDR, false),
        InterruptSource(5, 0x002B, InstructionHandler.TF2_BIT or InstructionHandler.EXF2_BIT,
            InstructionHandler.T2CON_ADDR,
            InstructionHandler.ET2_BIT, InstructionHandler.IE_ADDR,
            InstructionHandler.PT2_BIT, InstructionHandler.IP_ADDR, false),
    )

    /** Sample external interrupt pins and latch edge-triggered flags. Call once per machine cycle. */
    fun sample() {
        val tcon = state.TCON.toInt()
        val p3 = state.getEffectivePort(3)
        val int0 = (p3 and 0x04) == 0
        val int1 = (p3 and 0x08) == 0

        if ((tcon and InstructionHandler.IT0_BIT) != 0) {
            if (prevInt0 && !int0) {
                state.TCON = ((tcon or InstructionHandler.IE0_BIT) and 0xFF).toUByte()
            }
        } else {
            if (int0) {
                state.TCON = ((tcon or InstructionHandler.IE0_BIT) and 0xFF).toUByte()
            } else {
                state.TCON = ((tcon and InstructionHandler.IE0_BIT.inv()) and 0xFF).toUByte()
            }
        }

        if ((tcon and InstructionHandler.IT1_BIT) != 0) {
            if (prevInt1 && !int1) {
                state.TCON = ((tcon or InstructionHandler.IE1_BIT) and 0xFF).toUByte()
            }
        } else {
            if (int1) {
                state.TCON = ((tcon or InstructionHandler.IE1_BIT) and 0xFF).toUByte()
            } else {
                state.TCON = ((tcon and InstructionHandler.IE1_BIT.inv()) and 0xFF).toUByte()
            }
        }

        prevInt0 = int0
        prevInt1 = int1
    }

    /** Poll for pending interrupts. Returns the number of cycles consumed (0 or 2). */
    fun poll(): Int {
        lastInterruptCycles = 0

        if ((state.IE.toInt() and InstructionHandler.EA_BIT) == 0) return 0

        for (source in sources) {
            val flagReg = readSFR(source.flagAddr)
            if ((flagReg.toInt() and source.flagMask) == 0) continue

            val enableReg = readSFR(source.enableAddr)
            if ((enableReg.toInt() and source.enableMask) == 0) continue

            val ipReg = readSFR(source.priorityAddr)
            val isHighPriority = (ipReg.toInt() and source.priorityMask) != 0

            if (isHighPriority && highPriorityInService) continue
            if (!isHighPriority && lowPriorityInService) continue

            acknowledge(source, isHighPriority)
            lastInterruptCycles = 2
            return 2
        }

        return 0
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
        prevInt0 = false
        prevInt1 = false
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
            val isEdge = when (source.index) {
                0 -> (state.TCON.toInt() and InstructionHandler.IT0_BIT) != 0
                2 -> (state.TCON.toInt() and InstructionHandler.IT1_BIT) != 0
                else -> false
            }
            if (isEdge) {
                state.TCON = ((state.TCON.toInt() and source.flagMask.inv()) and 0xFF).toUByte()
            }
        }
    }

    private fun readSFR(addr: Int): UByte = state.sfr[addr - 0x80]
}
