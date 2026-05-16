@file:OptIn(ExperimentalUnsignedTypes::class)

package dev.dominikstahl.emu_8051.engine

@OptIn(ExperimentalUnsignedTypes::class)
fun CpuState.sjmp(): Int {
    pc++
    val relOffset = rom[pc].toByte().toInt()
    pc++
    pc = (pc + relOffset) and 0xFFFF
    return 2
}

@OptIn(ExperimentalUnsignedTypes::class)
fun CpuState.ajmp(rawOpcode: Int): Int {
    pc++
    val addrLow = rom[pc].toInt()
    pc++
    val addrHigh3Bits = (rawOpcode shr 5) and 0x07
    val addr11 = (addrHigh3Bits shl 8) or addrLow
    val pcPage = pc and 0xF800
    pc = pcPage or addr11
    return 2
}

@OptIn(ExperimentalUnsignedTypes::class)
fun CpuState.ljmp(): Int {
    pc++
    val addrHigh = rom[pc].toInt()
    val addrLow = rom[pc + 1].toInt()
    pc = (addrHigh shl 8) or addrLow
    return 2
}

fun CpuState.jmp_at_a_dptr(): Int {
    pc++
    pc = (ACC.toInt() + DPTR) and 0xFFFF
    return 2
}

fun CpuState.jc(): Int {
    pc++
    val relOffset = rom[pc].toByte().toInt()
    pc++
    if ((PSW.toInt() and CY_BIT) != 0) pc = (pc + relOffset) and 0xFFFF
    return 2
}

fun CpuState.jnc(): Int {
    pc++
    val relOffset = rom[pc].toByte().toInt()
    pc++
    if ((PSW.toInt() and CY_BIT) == 0) pc = (pc + relOffset) and 0xFFFF
    return 2
}

fun CpuState.jz(): Int {
    pc++
    val relOffset = rom[pc].toByte().toInt()
    pc++
    if (ACC.toInt() == 0) pc = (pc + relOffset) and 0xFFFF
    return 2
}

fun CpuState.jnz(): Int {
    pc++
    val relOffset = rom[pc].toByte().toInt()
    pc++
    if (ACC.toInt() != 0) pc = (pc + relOffset) and 0xFFFF
    return 2
}

fun CpuState.jb(): Int {
    pc++
    val bitAddr = rom[pc++]
    val relOffset = rom[pc++].toByte().toInt()
    if (getBit(bitAddr)) pc = (pc + relOffset) and 0xFFFF
    return 2
}

fun CpuState.jnb(): Int {
    pc++
    val bitAddr = rom[pc++]
    val relOffset = rom[pc++].toByte().toInt()
    if (!getBit(bitAddr)) pc = (pc + relOffset) and 0xFFFF
    return 2
}

fun CpuState.jbc(): Int {
    pc++
    val bitAddr = rom[pc++]
    val relOffset = rom[pc++].toByte().toInt()
    if (getBit(bitAddr)) {
        setBit(bitAddr, false)
        pc = (pc + relOffset) and 0xFFFF
    }
    return 2
}

@OptIn(ExperimentalUnsignedTypes::class)
fun CpuState.cjne_a_imm(): Int {
    pc++
    val destValue = ACC.toInt()
    val srcValue = rom[pc++].toInt()
    val relOffset = rom[pc++].toByte().toInt()
    val nextInstructionAddr = pc
    setFlag(CY_BIT, destValue < srcValue)
    if (destValue != srcValue) pc = (nextInstructionAddr + relOffset) and 0xFFFF
    return 2
}

@OptIn(ExperimentalUnsignedTypes::class)
fun CpuState.cjne_a_direct(): Int {
    pc++
    val destValue = ACC.toInt()
    val srcValue = readPin(rom[pc++]).toInt()
    val relOffset = rom[pc++].toByte().toInt()
    val nextInstructionAddr = pc
    setFlag(CY_BIT, destValue < srcValue)
    if (destValue != srcValue) pc = (nextInstructionAddr + relOffset) and 0xFFFF
    return 2
}

fun CpuState.cjne_ri_imm(ri: Int): Int {
    pc++
    val pointer = getRegister(ri)
    val destValue = readIndirect(pointer).toInt()
    val srcValue = rom[pc++].toInt()
    val relOffset = rom[pc++].toByte().toInt()
    val nextInstructionAddr = pc
    setFlag(CY_BIT, destValue < srcValue)
    if (destValue != srcValue) pc = (nextInstructionAddr + relOffset) and 0xFFFF
    return 2
}

fun CpuState.cjne_rn_imm(rn: Int): Int {
    pc++
    val destValue = getRegister(rn).toInt()
    val srcValue = rom[pc++].toInt()
    val relOffset = rom[pc++].toByte().toInt()
    val nextInstructionAddr = pc
    setFlag(CY_BIT, destValue < srcValue)
    if (destValue != srcValue) pc = (nextInstructionAddr + relOffset) and 0xFFFF
    return 2
}

fun CpuState.djnz_direct(): Int {
    pc++
    val directAddr = rom[pc++]
    val relOffset = rom[pc++].toByte().toInt()
    val value = readDirect(directAddr)
    val decValue = (value - 1u).toUByte()
    writeDirect(directAddr, decValue)
    if (decValue.toInt() != 0) pc = (pc + relOffset) and 0xFFFF
    return 2
}

fun CpuState.djnz_rn(rn: Int): Int {
    pc++
    val relOffset = rom[pc++].toByte().toInt()
    val value = getRegister(rn)
    val decValue = (value - 1u).toUByte()
    setRegister(rn, decValue)
    if (decValue.toInt() != 0) pc = (pc + relOffset) and 0xFFFF
    return 2
}

@OptIn(ExperimentalUnsignedTypes::class)
fun CpuState.acall(rawOpcode: Int): Int {
    pc++
    val addrLow = rom[pc].toInt()
    pc++
    val returnAddr = pc
    pushStack((returnAddr and 0xFF).toUByte())
    pushStack(((returnAddr shr 8) and 0xFF).toUByte())
    val addrHigh3Bits = (rawOpcode shr 5) and 0x07
    val addr11 = (addrHigh3Bits shl 8) or addrLow
    val pcPage = pc and 0xF800
    pc = pcPage or addr11
    return 2
}

@OptIn(ExperimentalUnsignedTypes::class)
fun CpuState.lcall(): Int {
    val returnAddr = pc + 3
    pc++
    val addrHigh = rom[pc++].toInt()
    val addrLow = rom[pc].toInt()
    pushStack((returnAddr and 0xFF).toUByte())
    pushStack(((returnAddr shr 8) and 0xFF).toUByte())
    pc = (addrHigh shl 8) or addrLow
    return 2
}

fun CpuState.ret(): Int {
    val high = popStack().toInt()
    val low = popStack().toInt()
    pc = (high shl 8) or low
    return 2
}

fun CpuState.reti(): Int {
    val high = popStack().toInt()
    val low = popStack().toInt()
    pc = (high shl 8) or low
    interruptController?.clearInService()
    return 2
}
