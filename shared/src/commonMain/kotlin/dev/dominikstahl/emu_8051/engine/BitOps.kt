package dev.dominikstahl.emu_8051.engine

fun CpuState.clr_a(): Int {
    pc++
    ACC = 0x00u
    return 1
}

fun CpuState.clr_c(): Int {
    pc++
    setFlag(CY_BIT, false)
    return 1
}

fun CpuState.clr_bit(): Int {
    pc++
    val bitAddr = rom[pc++]
    setBit(bitAddr, false)
    return 1
}

fun CpuState.setb_c(): Int {
    pc++
    setFlag(CY_BIT, true)
    return 1
}

fun CpuState.setb_bit(): Int {
    pc++
    val bitAddr = rom[pc++]
    setBit(bitAddr, true)
    return 1
}

fun CpuState.cpl_a(): Int {
    pc++
    ACC = ACC.inv()
    return 1
}

fun CpuState.cpl_c(): Int {
    pc++
    val currentCarry = (PSW.toInt() and CY_BIT) != 0
    setFlag(CY_BIT, !currentCarry)
    return 1
}

fun CpuState.cpl_bit(): Int {
    pc++
    val bitAddr = rom[pc++]
    complementBit(bitAddr)
    return 1
}
