package dev.dominikstahl.emu_8051.ui.components.hardware.hd44780

class Hd44780Controller(
    val cols: Int = 16,
    val lines: Int = 2,
) {
    companion object {
        private const val PIN_RS = 0
        private const val PIN_RW = 1
        private const val PIN_E = 2

        private const val CMD_CLEAR = 0x01
        private const val CMD_HOME = 0x02
        private const val CMD_ENTRY_MODE = 0x04
        private const val CMD_DISPLAY = 0x08
        private const val CMD_SHIFT = 0x10
        private const val CMD_FUNCTION = 0x20
        private const val CMD_CGRAM = 0x40
        private const val CMD_DDRAM = 0x80

        private const val DDRAM_SIZE = 128
        private val ROW_OFFSETS = intArrayOf(0x00, 0x40, 0x14, 0x54)
    }

    private val ddram = ByteArray(DDRAM_SIZE) { ' '.code.toByte() }
    val cgram = ByteArray(64) { 0 }

    var displayOn = true
    var cursorOn = false
    var blinkOn = false
    var entryInc = true
    var entryShift = false
    var dataLen8Bit = false
    var displayLines = 1
    var font5x10 = false
    var ddramAddr = 0
    var cgramAddr = 0
    var isCgramMode = false
    var displayShiftOffset = 0

    private var prevE = false
    private var nibblePhase = 0
    private var pendingNibble = 0
    private var initialized4Bit = false

    fun tick(portVal: Int) {
        val rs = ((portVal shr PIN_RS) and 1) != 0
        val rw = ((portVal shr PIN_RW) and 1) != 0
        val e = ((portVal shr PIN_E) and 1) != 0
        val data = (portVal shr 4) and 0x0F

        val fallingEdge = prevE && !e
        prevE = e

        if (!fallingEdge) return
        if (rw) return

        if (!initialized4Bit) {
            val byte = data shl 4
            if (!rs) {
                execCmd(byte)
                if ((byte and CMD_FUNCTION) != 0 && (byte and 0x10) == 0) {
                    initialized4Bit = true
                }
            } else {
                writeData(byte)
            }
        } else {
            if (nibblePhase == 0) {
                pendingNibble = data shl 4
                nibblePhase = 1
            } else {
                val byte = pendingNibble or data
                nibblePhase = 0
                if (!rs) execCmd(byte)
                else writeData(byte)
            }
        }
    }

    private fun execCmd(cmd: Int) {
        when {
            cmd == CMD_CLEAR -> {
                ddram.fill(' '.code.toByte())
                ddramAddr = 0
                displayShiftOffset = 0
                isCgramMode = false
            }
            (cmd and 0xFE) == CMD_HOME -> {
                ddramAddr = 0
                displayShiftOffset = 0
                isCgramMode = false
            }
            (cmd and CMD_DDRAM) != 0 -> {
                ddramAddr = cmd and 0x7F
                isCgramMode = false
            }
            (cmd and CMD_CGRAM) != 0 -> {
                cgramAddr = cmd and 0x3F
                isCgramMode = true
            }
            (cmd and CMD_FUNCTION) != 0 -> {
                dataLen8Bit = (cmd and 0x10) != 0
                displayLines = if ((cmd and 0x08) != 0) 2 else 1
                font5x10 = (cmd and 0x04) != 0
            }
            (cmd and CMD_SHIFT) != 0 -> {
                val shiftDisplay = (cmd and 0x08) != 0
                val shiftRight = (cmd and 0x04) != 0
                if (shiftDisplay) {
                    displayShiftOffset = if (shiftRight) {
                        (displayShiftOffset + 39) % 40
                    } else {
                        (displayShiftOffset + 1) % 40
                    }
                } else {
                    ddramAddr = (if (shiftRight) ddramAddr + 1 else ddramAddr - 1) and 0x7F
                }
            }
            (cmd and CMD_DISPLAY) != 0 -> {
                displayOn = (cmd and 0x04) != 0
                cursorOn = (cmd and 0x02) != 0
                blinkOn = (cmd and 0x01) != 0
            }
            (cmd and CMD_ENTRY_MODE) != 0 -> {
                entryInc = (cmd and 0x02) != 0
                entryShift = (cmd and 0x01) != 0
            }
        }
    }

    private fun writeData(data: Int) {
        if (isCgramMode) {
            cgram[cgramAddr and 0x3F] = data.toByte()
            cgramAddr = (cgramAddr + 1) and 0x3F
        } else {
            ddram[ddramAddr and 0x7F] = data.toByte()
            if (entryShift) {
                displayShiftOffset = if (entryInc) {
                    (displayShiftOffset + 1) % 40
                } else {
                    (displayShiftOffset + 39) % 40
                }
            }
            ddramAddr = (if (entryInc) ddramAddr + 1 else ddramAddr - 1) and 0x7F
        }
    }

    fun reset() {
        ddram.fill(' '.code.toByte())
        cgram.fill(0)
        displayOn = true
        cursorOn = false
        blinkOn = false
        entryInc = true
        entryShift = false
        dataLen8Bit = false
        displayLines = 1
        font5x10 = false
        ddramAddr = 0
        cgramAddr = 0
        isCgramMode = false
        displayShiftOffset = 0
        prevE = false
        nibblePhase = 0
        pendingNibble = 0
        initialized4Bit = false
    }

    private fun ddramIndexFor(line: Int, col: Int): Int {
        val offset = ROW_OFFSETS.getOrElse(line) { line * 0x40 }
        if (displayShiftOffset == 0) return offset + col
        val bankBase = if ((line and 1) == 0) 0x00 else 0x40
        val relStart = offset - bankBase
        return bankBase + ((relStart + col + displayShiftOffset) % 40)
    }

    fun getDisplayLines(): List<String> {
        val result = mutableListOf<String>()
        for (line in 0 until lines) {
            val sb = StringBuilder(cols)
            for (c in 0 until cols) {
                val idx = ddramIndexFor(line, c)
                val b = if (idx in 0 until DDRAM_SIZE) ddram[idx].toInt() and 0xFF else 0x20
                sb.append(if (b < 0x20 || b > 0x7E) ' ' else b.toChar())
            }
            result.add(sb.toString())
        }
        return result
    }

    fun getRawLines(): List<List<Int>> {
        val result = mutableListOf<List<Int>>()
        for (line in 0 until lines) {
            val row = mutableListOf<Int>()
            for (c in 0 until cols) {
                val idx = ddramIndexFor(line, c)
                row.add(if (idx in 0 until DDRAM_SIZE) ddram[idx].toInt() and 0xFF else 0x20)
            }
            result.add(row)
        }
        return result
    }

    fun cursorPosition(): Pair<Int, Int>? {
        if (isCgramMode) return null
        for (line in 0 until lines) {
            for (c in 0 until cols) {
                if (ddramIndexFor(line, c) == ddramAddr) return line to c
            }
        }
        return null
    }

    fun getCgramCopy(): ByteArray = cgram.copyOf()
}
