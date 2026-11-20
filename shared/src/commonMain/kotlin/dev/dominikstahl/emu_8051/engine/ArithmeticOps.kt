@file:OptIn(ExperimentalUnsignedTypes::class)

package dev.dominikstahl.emu_8051.engine

private fun CpuState.addCore(operand: Int, cyIn: Int): Int {
    val a = ACC.toInt()
    val result = a + operand + cyIn
    val acResult = (a and 0x0F) + (operand and 0x0F) + cyIn
    val carry6 = ((a and 0x7F) + (operand and 0x7F) + cyIn) shr 7
    val carry7 = (result shr 8) and 0x01
    var psw = PSW.toInt() and (CY_BIT or AC_BIT or OV_BIT).inv()
    if (carry7 != 0) psw = psw or CY_BIT
    if ((acResult shr 4) != 0) psw = psw or AC_BIT
    if ((carry6 xor carry7) != 0) psw = psw or OV_BIT
    PSW = psw.toUByte()
    ACC = (result and 0xFF).toUByte()
    return 1
}

private fun CpuState.subbCore(operand: Int): Int {
    val a = ACC.toInt()
    val pswOld = PSW.toInt()
    val cyIn = if ((pswOld and CY_BIT) != 0) 1 else 0
    val result = a - operand - cyIn
    val acResult = (a and 0x0F) - (operand and 0x0F) - cyIn
    val borrow6 = if (((a and 0x7F) - (operand and 0x7F) - cyIn) < 0) 1 else 0
    val borrow7 = if (result < 0) 1 else 0
    var psw = pswOld and (CY_BIT or AC_BIT or OV_BIT).inv()
    if (result < 0) psw = psw or CY_BIT
    if (acResult < 0) psw = psw or AC_BIT
    if ((borrow6 xor borrow7) != 0) psw = psw or OV_BIT
    PSW = psw.toUByte()
    ACC = (result and 0xFF).toUByte()
    return 1
}

fun CpuState.add_a_imm(): Int {
    pc++
    return addCore(rom[pc++].toInt(), 0)
}

fun CpuState.add_a_direct(): Int {
    pc++
    return addCore(readPin(rom[pc++]).toInt(), 0)
}

fun CpuState.add_a_ri(ri: Int): Int {
    pc++
    return addCore(readIndirect(getRegister(ri)).toInt(), 0)
}

fun CpuState.add_a_rn(rn: Int): Int {
    pc++
    return addCore(getRegister(rn).toInt(), 0)
}

fun CpuState.addc_a_imm(): Int {
    pc++
    val cyIn = if ((PSW.toInt() and CY_BIT) != 0) 1 else 0
    return addCore(rom[pc++].toInt(), cyIn)
}

fun CpuState.addc_a_direct(): Int {
    pc++
    val cyIn = if ((PSW.toInt() and CY_BIT) != 0) 1 else 0
    return addCore(readPin(rom[pc++]).toInt(), cyIn)
}

fun CpuState.addc_a_ri(ri: Int): Int {
    pc++
    val cyIn = if ((PSW.toInt() and CY_BIT) != 0) 1 else 0
    return addCore(readIndirect(getRegister(ri)).toInt(), cyIn)
}

fun CpuState.addc_a_rn(rn: Int): Int {
    pc++
    val cyIn = if ((PSW.toInt() and CY_BIT) != 0) 1 else 0
    return addCore(getRegister(rn).toInt(), cyIn)
}

fun CpuState.subb_a_imm(): Int {
    pc++
    return subbCore(rom[pc++].toInt())
}

fun CpuState.subb_a_direct(): Int {
    pc++
    return subbCore(readPin(rom[pc++]).toInt())
}

fun CpuState.subb_a_ri(ri: Int): Int {
    pc++
    return subbCore(readIndirect(getRegister(ri)).toInt())
}

fun CpuState.subb_a_rn(rn: Int): Int {
    pc++
    return subbCore(getRegister(rn).toInt())
}

fun CpuState.inc_a(): Int {
    pc++
    ACC = (ACC + 1u).toUByte()
    return 1
}

fun CpuState.inc_direct(): Int {
    pc++
    val addr = rom[pc++]
    writeDirect(addr, (readDirect(addr) + 1u).toUByte())
    return 1
}

fun CpuState.inc_ri(ri: Int): Int {
    pc++
    val pointer = getRegister(ri)
    writeIndirect(pointer, (readIndirect(pointer) + 1u).toUByte())
    return 1
}

fun CpuState.inc_rn(rn: Int): Int {
    pc++
    val value = getRegister(rn)
    setRegister(rn, (value + 1u).toUByte())
    return 1
}

fun CpuState.inc_dptr(): Int {
    pc++
    DPTR = (DPTR + 1) and 0xFFFF
    return 2
}

fun CpuState.dec_a(): Int {
    pc++
    ACC = (ACC - 1u).toUByte()
    return 1
}

fun CpuState.dec_direct(): Int {
    pc++
    val addr = rom[pc++]
    writeDirect(addr, (readDirect(addr) - 1u).toUByte())
    return 1
}

fun CpuState.dec_ri(ri: Int): Int {
    pc++
    val pointer = getRegister(ri)
    writeIndirect(pointer, (readIndirect(pointer) - 1u).toUByte())
    return 1
}

fun CpuState.dec_rn(rn: Int): Int {
    pc++
    val value = getRegister(rn)
    setRegister(rn, (value - 1u).toUByte())
    return 1
}

fun CpuState.mul_ab(): Int {
    pc++
    val a = ACC.toInt()
    val b = B.toInt()
    val product = a * b
    var psw = PSW.toInt() and (CY_BIT or OV_BIT).inv()
    if (product > 0xFF) psw = psw or OV_BIT
    PSW = psw.toUByte()
    ACC = (product and 0xFF).toUByte()
    B = ((product shr 8) and 0xFF).toUByte()
    return 4
}

fun CpuState.div_ab(): Int {
    pc++
    val a = ACC.toInt()
    val b = B.toInt()
    var psw = PSW.toInt() and (CY_BIT or OV_BIT).inv()
    if (b == 0) {
        PSW = (psw or OV_BIT).toUByte()
    } else {
        PSW = psw.toUByte()
        ACC = (a / b).toUByte()
        B = (a % b).toUByte()
    }
    return 4
}

fun CpuState.da_a(): Int {
    pc++
    var accValue = ACC.toInt()
    val psw = PSW.toInt()
    val initialCy = (psw and CY_BIT) != 0
    val initialAc = (psw and AC_BIT) != 0
    var carryOut = initialCy
    if ((accValue and 0x0F) > 0x09 || initialAc) {
        accValue += 0x06
    }
    if ((accValue shr 4) > 0x09 || initialCy) {
        accValue += 0x60
        carryOut = true
    }
    setFlag(CY_BIT, carryOut)
    ACC = (accValue and 0xFF).toUByte()
    return 1
}
