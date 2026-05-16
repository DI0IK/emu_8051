@file:OptIn(ExperimentalUnsignedTypes::class)

package dev.dominikstahl.emu_8051.engine

fun CpuState.nop(): Int {
    pc++
    return 1
}

fun CpuState.push_direct(): Int {
    pc++
    val addr = rom[pc++]
    pushStack(readPin(addr))
    return 2
}

fun CpuState.pop_direct(): Int {
    pc++
    val addr = rom[pc++]
    writeDirect(addr, popStack())
    return 2
}

fun CpuState.xch_a_direct(): Int {
    pc++
    val addr = rom[pc++]
    val oldAcc = ACC
    ACC = readPin(addr)
    writeDirect(addr, oldAcc)
    return 1
}

fun CpuState.xch_a_ri(ri: Int): Int {
    pc++
    val pointer = getRegister(ri)
    val oldAcc = ACC
    ACC = readIndirect(pointer)
    writeIndirect(pointer, oldAcc)
    return 1
}

fun CpuState.xch_a_rn(rn: Int): Int {
    pc++
    val oldAcc = ACC
    ACC = getRegister(rn)
    setRegister(rn, oldAcc)
    return 1
}

fun CpuState.xchd_a_ri(ri: Int): Int {
    pc++
    val pointer = getRegister(ri)
    val memValue = readIndirect(pointer).toInt()
    val accValue = ACC.toInt()
    val newAcc = (accValue and 0xF0) or (memValue and 0x0F)
    val newMem = (memValue and 0xF0) or (accValue and 0x0F)
    ACC = newAcc.toUByte()
    writeIndirect(pointer, newMem.toUByte())
    return 1
}

fun CpuState.swap_a(): Int {
    pc++
    val acc = ACC.toInt()
    val lowNibble = acc and 0x0F
    val highNibble = acc and 0xF0
    ACC = ((lowNibble shl 4) or (highNibble shr 4)).toUByte()
    return 1
}

fun CpuState.rr_a(): Int {
    pc++
    val acc = ACC.toInt()
    val bit0 = acc and 0x01
    ACC = (((acc shr 1) or (bit0 shl 7)) and 0xFF).toUByte()
    return 1
}

fun CpuState.rrc_a(): Int {
    pc++
    val acc = ACC.toInt()
    val cy = if ((PSW.toInt() and CY_BIT) != 0) 1 else 0
    val bit0 = acc and 0x01
    ACC = (((acc shr 1) or (cy shl 7)) and 0xFF).toUByte()
    setFlag(CY_BIT, bit0 == 1)
    return 1
}

fun CpuState.rl_a(): Int {
    pc++
    val acc = ACC.toInt()
    val bit7 = (acc shr 7) and 0x01
    ACC = (((acc shl 1) or bit7) and 0xFF).toUByte()
    return 1
}

fun CpuState.rlc_a(): Int {
    pc++
    val acc = ACC.toInt()
    val cy = if ((PSW.toInt() and CY_BIT) != 0) 1 else 0
    val bit7 = (acc shr 7) and 0x01
    ACC = (((acc shl 1) or cy) and 0xFF).toUByte()
    setFlag(CY_BIT, bit7 == 1)
    return 1
}
