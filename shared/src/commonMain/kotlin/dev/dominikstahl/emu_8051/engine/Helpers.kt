package dev.dominikstahl.emu_8051.engine

@OptIn(ExperimentalUnsignedTypes::class)
fun CpuState.setFlag(bitMask: Int, value: Boolean) {
    PSW = if (value) (PSW.toInt() or bitMask).toUByte()
    else (PSW.toInt() and bitMask.inv()).toUByte()
}

fun CpuState.getRegister(index: Int): UByte = readDirect((registerBankBase + index).toUByte())

fun CpuState.setRegister(index: Int, value: UByte) = writeDirect((registerBankBase + index).toUByte(), value)

@OptIn(ExperimentalUnsignedTypes::class)
fun CpuState.pushStack(value: UByte) {
    SP = (SP + 1u).toUByte()
    ram[SP.toInt()] = value
}

@OptIn(ExperimentalUnsignedTypes::class)
fun CpuState.popStack(): UByte {
    if (SP < 0x08u) return 0u
    val value = ram[SP.toInt()]
    SP = (SP - 1u).toUByte()
    return value
}

@OptIn(ExperimentalUnsignedTypes::class)
fun CpuState.getBit(bitAddress: UByte): Boolean {
    val addr = bitAddress.toInt()
    return if (addr < 0x80) {
        val byteAddr = 0x20 + (addr / 8)
        val bitPos = addr % 8
        ((ram[byteAddr].toInt() shr bitPos) and 0x01) != 0
    } else {
        val byteAddr = (addr and 0xF8).toUByte()
        val bitPos = addr and 0x07
        ((readPin(byteAddr).toInt() shr bitPos) and 0x01) != 0
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
fun CpuState.setBit(bitAddress: UByte, value: Boolean) {
    val addr = bitAddress.toInt()
    if (addr < 0x80) {
        val byteAddr = 0x20 + (addr / 8)
        val bitPos = addr % 8
        val current = ram[byteAddr].toInt()
        ram[byteAddr] = if (value) (current or (1 shl bitPos)).toUByte()
        else (current and (1 shl bitPos).inv()).toUByte()
    } else {
        val byteAddr = (addr and 0xF8).toUByte()
        val bitPos = addr and 0x07
        val current = readDirect(byteAddr).toInt()
        val newVal = if (value) (current or (1 shl bitPos)).toUByte()
        else (current and (1 shl bitPos).inv()).toUByte()
        writeDirect(byteAddr, newVal)
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
fun CpuState.complementBit(bitAddress: UByte) {
    val addr = bitAddress.toInt()
    if (addr < 0x80) {
        val byteAddr = 0x20 + (addr / 8)
        val bitPos = addr % 8
        val current = ram[byteAddr].toInt()
        ram[byteAddr] = (current xor (1 shl bitPos)).toUByte()
    } else {
        val byteAddr = (addr and 0xF8).toUByte()
        val bitPos = addr and 0x07
        val current = readDirect(byteAddr).toInt()
        writeDirect(byteAddr, (current xor (1 shl bitPos)).toUByte())
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
fun CpuState.handle_illegal(): Int {
    pc++
    return 1
}
