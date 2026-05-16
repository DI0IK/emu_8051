package dev.dominikstahl.emu_8051.ui.components.hardware.hd44780

class Hd44780Controller(
    val cols: Int = 16,
    val lines: Int = 2,
) {
    companion object {
        private const val RS_CMD = 0
        private const val RS_DATA = 1
        private const val RW_WRITE = 0
        private const val RW_READ = 1
        private const val PIN_RS = 0
        private const val PIN_RW = 1
        private const val PIN_E = 2

        private const val CMD_CLEAR = 0x01u
        private const val CMD_HOME = 0x02u
        private const val CMD_ENTRY_MODE = 0x04u
        private const val CMD_DISPLAY = 0x08u
        private const val CMD_SHIFT = 0x10u
        private const val CMD_FUNCTION = 0x20u
        private const val CMD_CGRAM = 0x40u
        private const val CMD_DDRAM = 0x80u

        private val ROW_OFFSETS = intArrayOf(0x00, 0x40, 0x14, 0x54)
    }

    private val ddram = ByteArray(80) { ' '.code.toByte() }
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
            println("LCD tick (init): data=$data byte=$byte rs=$rs")
            if (!rs) {
                execCmd(byte)
                if (byte and CMD_FUNCTION.toInt() != 0 && (byte and 0x10) == 0) {
                    initialized4Bit = true
                    println("LCD: initialized4Bit=true")
                }
            } else {
                writeData(byte)
            }
        } else {
            if (nibblePhase == 0) {
                pendingNibble = data shl 4
                nibblePhase = 1
                println("LCD tick (4bit): first nibble data=$data pendingNibble=$pendingNibble rs=$rs")
            } else {
                val byte = pendingNibble or data
                nibblePhase = 0
                println("LCD tick (4bit): second nibble data=$data byte=$byte rs=$rs")
                if (!rs) execCmd(byte)
                else writeData(byte)
            }
        }
    }

    private fun execCmd(cmd: Int) {
        println("LCD execCmd: $cmd (0x${cmd.toString(16)})")
        when {
            cmd == CMD_CLEAR.toInt() -> {
                ddram.fill(' '.code.toByte())
                ddramAddr = 0
                isCgramMode = false
            }
            cmd == CMD_HOME.toInt() -> {
                ddramAddr = 0
                isCgramMode = false
            }
            cmd and CMD_DDRAM.toInt() != 0 -> {
                ddramAddr = cmd and 0x7F
                isCgramMode = false
            }
            cmd and CMD_CGRAM.toInt() != 0 -> {
                cgramAddr = cmd and 0x3F
                isCgramMode = true
            }
            cmd and CMD_FUNCTION.toInt() != 0 -> {
                dataLen8Bit = (cmd and 0x10) != 0
                displayLines = if ((cmd and 0x08) != 0) 2 else 1
                font5x10 = (cmd and 0x04) != 0
            }
            cmd and CMD_SHIFT.toInt() != 0 -> {
                val shiftDisplay = (cmd and 0x08) != 0
                val shiftRight = (cmd and 0x04) != 0
                if (shiftDisplay) {
                    if (shiftRight) shiftRight() else shiftLeft()
                } else {
                    ddramAddr = if (shiftRight) ddramAddr + 1 else ddramAddr - 1
                    ddramAddr = ddramAddr.coerceIn(0, 127)
                }
            }
            cmd and CMD_DISPLAY.toInt() != 0 -> {
                displayOn = (cmd and 0x04) != 0
                cursorOn = (cmd and 0x02) != 0
                blinkOn = (cmd and 0x01) != 0
            }
            cmd and CMD_ENTRY_MODE.toInt() != 0 -> {
                entryInc = (cmd and 0x02) != 0
                entryShift = (cmd and 0x01) != 0
            }
        }
    }

    private fun writeData(data: Int) {
        println("LCD writeData: $data (0x${data.toString(16)}) ddramAddr=$ddramAddr isCgramMode=$isCgramMode entryInc=$entryInc")
        if (isCgramMode) {
            if (cgramAddr < 64) cgram[cgramAddr] = data.toByte()
            cgramAddr = (cgramAddr + 1) and 0x3F
        } else {
            if (ddramAddr < 80) ddram[ddramAddr] = data.toByte()
            ddramAddr = if (entryInc) ddramAddr + 1 else ddramAddr - 1
            ddramAddr = ddramAddr.coerceIn(0, 127)
        }
    }

    private fun shiftLeft() {
        for (line in 0 until lines) {
            val offset = ROW_OFFSETS.getOrElse(line) { line * 0x40 }
            val first = ddram.getOrElse(offset) { 0x20.toByte() }
            for (c in 0 until cols - 1) {
                val idx = offset + c
                if (idx < 80) ddram[idx] = ddram.getOrElse(idx + 1) { 0x20.toByte() }
            }
            val lastIdx = offset + cols - 1
            if (lastIdx < 80) ddram[lastIdx] = first
        }
    }

    private fun shiftRight() {
        for (line in 0 until lines) {
            val offset = ROW_OFFSETS.getOrElse(line) { line * 0x40 }
            val last = ddram.getOrElse(offset + cols - 1) { 0x20.toByte() }
            for (c in cols - 1 downTo 1) {
                val idx = offset + c
                if (idx < 80) ddram[idx] = ddram.getOrElse(idx - 1) { 0x20.toByte() }
            }
            if (offset < 80) ddram[offset] = last
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
        prevE = false
        nibblePhase = 0
        pendingNibble = 0
        initialized4Bit = false
    }

    fun getDisplayLines(): List<String> {
        val result = mutableListOf<String>()
        for (line in 0 until lines) {
            val offset = ROW_OFFSETS.getOrElse(line) { line * 0x40 }
            val sb = StringBuilder(cols)
            for (c in 0 until cols) {
                val idx = offset + c
                val b = if (idx < 80) ddram[idx].toInt() and 0xFF else 0x20
                sb.append(if (b < 0x20 || b > 0x7E) ' ' else b.toChar())
            }
            result.add(sb.toString())
        }
        return result
    }

    fun getRawLines(): List<List<Int>> {
        val result = mutableListOf<List<Int>>()
        for (line in 0 until lines) {
            val offset = ROW_OFFSETS.getOrElse(line) { line * 0x40 }
            val row = mutableListOf<Int>()
            for (c in 0 until cols) {
                val idx = offset + c
                row.add(if (idx < 80) ddram[idx].toInt() and 0xFF else 0x20)
            }
            result.add(row)
        }
        return result
    }

    fun getCgramCopy(): ByteArray = cgram.copyOf()
}
