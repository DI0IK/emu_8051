package dev.dominikstahl.emu_8051.engine

@OptIn(ExperimentalUnsignedTypes::class)
fun CpuState.mov_a_imm(): Int {
    pc++
    ACC = rom[pc++]
    return 1
}

@OptIn(ExperimentalUnsignedTypes::class)
fun CpuState.mov_a_direct(): Int {
    pc++
    ACC = readPin(rom[pc++])
    return 1
}

fun CpuState.mov_a_ri(ri: Int): Int {
    pc++
    val pointer = getRegister(ri)
    ACC = readIndirect(pointer)
    return 1
}

fun CpuState.mov_a_rn(rn: Int): Int {
    pc++
    ACC = getRegister(rn)
    return 1
}

fun CpuState.mov_rn_a(rn: Int): Int {
    pc++
    setRegister(rn, ACC)
    return 1
}

@OptIn(ExperimentalUnsignedTypes::class)
fun CpuState.mov_rn_direct(rn: Int): Int {
    pc++
    setRegister(rn, readPin(rom[pc++]))
    return 2
}

@OptIn(ExperimentalUnsignedTypes::class)
fun CpuState.mov_rn_imm(rn: Int): Int {
    pc++
    setRegister(rn, rom[pc++])
    return 1
}

@OptIn(ExperimentalUnsignedTypes::class)
fun CpuState.mov_direct_a(): Int {
    pc++
    writeDirect(rom[pc++], ACC)
    return 1
}

@OptIn(ExperimentalUnsignedTypes::class)
fun CpuState.mov_direct_rn(rn: Int): Int {
    pc++
    val addr = rom[pc++]
    writeDirect(addr, getRegister(rn))
    return 2
}

@OptIn(ExperimentalUnsignedTypes::class)
fun CpuState.mov_direct_direct(): Int {
    pc++
    val srcAddr = rom[pc++]
    val destAddr = rom[pc++]
    writeDirect(destAddr, readPin(srcAddr))
    return 2
}

fun CpuState.mov_direct_ri(ri: Int): Int {
    pc++
    val pointer = getRegister(ri)
    val destAddr = rom[pc++]
    writeDirect(destAddr, readIndirect(pointer))
    return 2
}

@OptIn(ExperimentalUnsignedTypes::class)
fun CpuState.mov_direct_imm(): Int {
    pc++
    val addr = rom[pc++]
    val data = rom[pc++]
    writeDirect(addr, data)
    return 2
}

fun CpuState.mov_ri_a(ri: Int): Int {
    pc++
    val pointer = getRegister(ri)
    writeIndirect(pointer, ACC)
    return 1
}

fun CpuState.mov_ri_direct(ri: Int): Int {
    pc++
    val pointer = getRegister(ri)
    val addr = rom[pc++]
    writeIndirect(pointer, readPin(addr))
    return 2
}

@OptIn(ExperimentalUnsignedTypes::class)
fun CpuState.mov_ri_imm(ri: Int): Int {
    pc++
    val pointer = getRegister(ri)
    val data = rom[pc++]
    writeIndirect(pointer, data)
    return 1
}

fun CpuState.mov_c_bit(): Int {
    pc++
    val bitAddr = rom[pc++]
    setFlag(CY_BIT, getBit(bitAddr))
    return 1
}

fun CpuState.mov_bit_c(): Int {
    pc++
    val bitAddr = rom[pc++]
    val carry = (PSW.toInt() and CY_BIT) != 0
    setBit(bitAddr, carry)
    return 2
}

@OptIn(ExperimentalUnsignedTypes::class)
fun CpuState.mov_dptr_imm16(): Int {
    pc++
    val high = rom[pc++].toInt()
    val low = rom[pc++].toInt()
    DPTR = (high shl 8) or low
    return 2
}

@OptIn(ExperimentalUnsignedTypes::class)
fun CpuState.movx_a_at_dptr(): Int {
    pc++
    ACC = xram[DPTR]
    return 2
}

fun CpuState.movx_a_at_ri(ri: Int): Int {
    pc++
    val pointer = getRegister(ri)
    ACC = xram[pointer.toInt()]
    return 2
}

fun CpuState.movx_at_dptr_a(): Int {
    pc++
    xram[DPTR] = ACC
    return 2
}

fun CpuState.movx_at_ri_a(ri: Int): Int {
    pc++
    val pointer = getRegister(ri)
    xram[pointer.toInt()] = ACC
    return 2
}

@OptIn(ExperimentalUnsignedTypes::class)
fun CpuState.movc_a_at_a_dptr(): Int {
    pc++
    val targetAddr = (ACC.toInt() + DPTR) and 0xFFFF
    ACC = rom[targetAddr]
    return 2
}

@OptIn(ExperimentalUnsignedTypes::class)
fun CpuState.movc_a_at_a_pc(): Int {
    pc++
    val targetAddr = (ACC.toInt() + pc) and 0xFFFF
    ACC = rom[targetAddr]
    return 2
}
