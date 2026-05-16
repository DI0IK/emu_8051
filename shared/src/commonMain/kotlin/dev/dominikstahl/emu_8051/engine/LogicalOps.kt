@file:OptIn(ExperimentalUnsignedTypes::class)

package dev.dominikstahl.emu_8051.engine

fun CpuState.anl_a_rn(rn: Int): Int {
    pc++
    ACC = ACC and getRegister(rn)
    return 1
}

fun CpuState.anl_a_direct(): Int {
    pc++
    ACC = ACC and readPin(rom[pc++])
    return 1
}

fun CpuState.anl_a_ri(ri: Int): Int {
    pc++
    val pointer = getRegister(ri)
    ACC = ACC and readIndirect(pointer)
    return 1
}

fun CpuState.anl_a_imm(): Int {
    pc++
    ACC = ACC and rom[pc++]
    return 1
}

@OptIn(ExperimentalUnsignedTypes::class)
fun CpuState.anl_direct_a(): Int {
    pc++
    val addr = rom[pc++]
    writeDirect(addr, readDirect(addr) and ACC)
    return 1
}

@OptIn(ExperimentalUnsignedTypes::class)
fun CpuState.anl_direct_imm(): Int {
    pc++
    val addr = rom[pc++]
    val data = rom[pc++]
    writeDirect(addr, readDirect(addr) and data)
    return 2
}

fun CpuState.anl_c_bit(): Int {
    pc++
    val bitAddr = rom[pc++]
    val bitValue = getBit(bitAddr)
    val currentCarry = (PSW.toInt() and CY_BIT) != 0
    setFlag(CY_BIT, currentCarry && bitValue)
    return 2
}

fun CpuState.anl_c_not_bit(): Int {
    pc++
    val bitAddr = rom[pc++]
    val bitValue = getBit(bitAddr)
    val currentCarry = (PSW.toInt() and CY_BIT) != 0
    setFlag(CY_BIT, currentCarry && !bitValue)
    return 2
}

fun CpuState.orl_a_rn(rn: Int): Int {
    pc++
    ACC = ACC or getRegister(rn)
    return 1
}

fun CpuState.orl_a_direct(): Int {
    pc++
    ACC = ACC or readPin(rom[pc++])
    return 1
}

fun CpuState.orl_a_ri(ri: Int): Int {
    pc++
    val pointer = getRegister(ri)
    ACC = ACC or readIndirect(pointer)
    return 1
}

fun CpuState.orl_a_imm(): Int {
    pc++
    ACC = ACC or rom[pc++]
    return 1
}

@OptIn(ExperimentalUnsignedTypes::class)
fun CpuState.orl_direct_a(): Int {
    pc++
    val addr = rom[pc++]
    writeDirect(addr, readDirect(addr) or ACC)
    return 1
}

@OptIn(ExperimentalUnsignedTypes::class)
fun CpuState.orl_direct_imm(): Int {
    pc++
    val addr = rom[pc++]
    val data = rom[pc++]
    writeDirect(addr, readDirect(addr) or data)
    return 2
}

fun CpuState.orl_c_bit(): Int {
    pc++
    val bitAddr = rom[pc++]
    val bitValue = getBit(bitAddr)
    val currentCarry = (PSW.toInt() and CY_BIT) != 0
    setFlag(CY_BIT, currentCarry || bitValue)
    return 2
}

fun CpuState.orl_c_not_bit(): Int {
    pc++
    val bitAddr = rom[pc++]
    val bitValue = getBit(bitAddr)
    val currentCarry = (PSW.toInt() and CY_BIT) != 0
    setFlag(CY_BIT, currentCarry || !bitValue)
    return 2
}

fun CpuState.xrl_a_rn(rn: Int): Int {
    pc++
    ACC = ACC xor getRegister(rn)
    return 1
}

fun CpuState.xrl_a_direct(): Int {
    pc++
    ACC = ACC xor readPin(rom[pc++])
    return 1
}

fun CpuState.xrl_a_ri(ri: Int): Int {
    pc++
    val pointer = getRegister(ri)
    ACC = ACC xor readIndirect(pointer)
    return 1
}

fun CpuState.xrl_a_imm(): Int {
    pc++
    ACC = ACC xor rom[pc++]
    return 1
}

@OptIn(ExperimentalUnsignedTypes::class)
fun CpuState.xrl_direct_a(): Int {
    pc++
    val addr = rom[pc++]
    writeDirect(addr, readDirect(addr) xor ACC)
    return 1
}

@OptIn(ExperimentalUnsignedTypes::class)
fun CpuState.xrl_direct_imm(): Int {
    pc++
    val addr = rom[pc++]
    val data = rom[pc++]
    writeDirect(addr, readDirect(addr) xor data)
    return 2
}
