package dev.dominikstahl.emu_8051.engine

@OptIn(ExperimentalUnsignedTypes::class)
fun CpuState.add_a_imm(): Int {
    pc++
    val a = ACC.toInt()
    val b = rom[pc++].toInt()
    val result = a + b
    val acResult = (a and 0x0F) + (b and 0x0F)
    val carry6 = ((a and 0x7F) + (b and 0x7F)) shr 7
    val carry7 = (result shr 8) and 0x01
    setFlag(CY_BIT, carry7 != 0)
    setFlag(AC_BIT, (acResult shr 4) != 0)
    setFlag(OV_BIT, (carry6 xor carry7) != 0)
    ACC = (result and 0xFF).toUByte()
    return 1
}

@OptIn(ExperimentalUnsignedTypes::class)
fun CpuState.add_a_direct(): Int {
    pc++
    val a = ACC.toInt()
    val b = readPin(rom[pc++]).toInt()
    val result = a + b
    val acResult = (a and 0x0F) + (b and 0x0F)
    val carry6 = ((a and 0x7F) + (b and 0x7F)) shr 7
    val carry7 = (result shr 8) and 0x01
    setFlag(CY_BIT, carry7 != 0)
    setFlag(AC_BIT, (acResult shr 4) != 0)
    setFlag(OV_BIT, (carry6 xor carry7) != 0)
    ACC = (result and 0xFF).toUByte()
    return 1
}

fun CpuState.add_a_ri(ri: Int): Int {
    pc++
    val pointer = getRegister(ri)
    val a = ACC.toInt()
    val b = readIndirect(pointer).toInt()
    val result = a + b
    val acResult = (a and 0x0F) + (b and 0x0F)
    val carry6 = ((a and 0x7F) + (b and 0x7F)) shr 7
    val carry7 = (result shr 8) and 0x01
    setFlag(CY_BIT, carry7 != 0)
    setFlag(AC_BIT, (acResult shr 4) != 0)
    setFlag(OV_BIT, (carry6 xor carry7) != 0)
    ACC = (result and 0xFF).toUByte()
    return 1
}

fun CpuState.add_a_rn(rn: Int): Int {
    pc++
    val a = ACC.toInt()
    val b = getRegister(rn).toInt()
    val result = a + b
    val acResult = (a and 0x0F) + (b and 0x0F)
    val carry6 = ((a and 0x7F) + (b and 0x7F)) shr 7
    val carry7 = (result shr 8) and 0x01
    setFlag(CY_BIT, carry7 != 0)
    setFlag(AC_BIT, (acResult shr 4) != 0)
    setFlag(OV_BIT, (carry6 xor carry7) != 0)
    ACC = (result and 0xFF).toUByte()
    return 1
}

@OptIn(ExperimentalUnsignedTypes::class)
fun CpuState.addc_a_imm(): Int {
    pc++
    val a = ACC.toInt()
    val b = rom[pc++].toInt()
    val cyIn = if ((PSW.toInt() and CY_BIT) != 0) 1 else 0
    val result = a + b + cyIn
    val acResult = (a and 0x0F) + (b and 0x0F) + cyIn
    val carry6 = ((a and 0x7F) + (b and 0x7F) + cyIn) shr 7
    val carry7 = (result shr 8) and 0x01
    setFlag(CY_BIT, carry7 != 0)
    setFlag(AC_BIT, (acResult shr 4) != 0)
    setFlag(OV_BIT, (carry6 xor carry7) != 0)
    ACC = (result and 0xFF).toUByte()
    return 1
}

@OptIn(ExperimentalUnsignedTypes::class)
fun CpuState.addc_a_direct(): Int {
    pc++
    val a = ACC.toInt()
    val b = readPin(rom[pc++]).toInt()
    val cyIn = if ((PSW.toInt() and CY_BIT) != 0) 1 else 0
    val result = a + b + cyIn
    val acResult = (a and 0x0F) + (b and 0x0F) + cyIn
    val carry6 = ((a and 0x7F) + (b and 0x7F) + cyIn) shr 7
    val carry7 = (result shr 8) and 0x01
    setFlag(CY_BIT, carry7 != 0)
    setFlag(AC_BIT, (acResult shr 4) != 0)
    setFlag(OV_BIT, (carry6 xor carry7) != 0)
    ACC = (result and 0xFF).toUByte()
    return 1
}

fun CpuState.addc_a_ri(ri: Int): Int {
    pc++
    val pointer = getRegister(ri)
    val a = ACC.toInt()
    val b = readIndirect(pointer).toInt()
    val cyIn = if ((PSW.toInt() and CY_BIT) != 0) 1 else 0
    val result = a + b + cyIn
    val acResult = (a and 0x0F) + (b and 0x0F) + cyIn
    val carry6 = ((a and 0x7F) + (b and 0x7F) + cyIn) shr 7
    val carry7 = (result shr 8) and 0x01
    setFlag(CY_BIT, carry7 != 0)
    setFlag(AC_BIT, (acResult shr 4) != 0)
    setFlag(OV_BIT, (carry6 xor carry7) != 0)
    ACC = (result and 0xFF).toUByte()
    return 1
}

fun CpuState.addc_a_rn(rn: Int): Int {
    pc++
    val a = ACC.toInt()
    val b = getRegister(rn).toInt()
    val cyIn = if ((PSW.toInt() and CY_BIT) != 0) 1 else 0
    val result = a + b + cyIn
    val acResult = (a and 0x0F) + (b and 0x0F) + cyIn
    val carry6 = ((a and 0x7F) + (b and 0x7F) + cyIn) shr 7
    val carry7 = (result shr 8) and 0x01
    setFlag(CY_BIT, carry7 != 0)
    setFlag(AC_BIT, (acResult shr 4) != 0)
    setFlag(OV_BIT, (carry6 xor carry7) != 0)
    ACC = (result and 0xFF).toUByte()
    return 1
}

@OptIn(ExperimentalUnsignedTypes::class)
fun CpuState.subb_a_imm(): Int {
    pc++
    val a = ACC.toInt()
    val b = rom[pc++].toInt()
    val cyIn = if ((PSW.toInt() and CY_BIT) != 0) 1 else 0
    val result = a - b - cyIn
    val acResult = (a and 0x0F) - (b and 0x0F) - cyIn
    val borrow6 = if (((a and 0x7F) - (b and 0x7F) - cyIn) < 0) 1 else 0
    val borrow7 = if (result < 0) 1 else 0
    setFlag(CY_BIT, result < 0)
    setFlag(AC_BIT, acResult < 0)
    setFlag(OV_BIT, (borrow6 xor borrow7) != 0)
    ACC = (result and 0xFF).toUByte()
    return 1
}

@OptIn(ExperimentalUnsignedTypes::class)
fun CpuState.subb_a_direct(): Int {
    pc++
    val a = ACC.toInt()
    val b = readPin(rom[pc++]).toInt()
    val cyIn = if ((PSW.toInt() and CY_BIT) != 0) 1 else 0
    val result = a - b - cyIn
    val acResult = (a and 0x0F) - (b and 0x0F) - cyIn
    val borrow6 = if (((a and 0x7F) - (b and 0x7F) - cyIn) < 0) 1 else 0
    val borrow7 = if (result < 0) 1 else 0
    setFlag(CY_BIT, result < 0)
    setFlag(AC_BIT, acResult < 0)
    setFlag(OV_BIT, (borrow6 xor borrow7) != 0)
    ACC = (result and 0xFF).toUByte()
    return 1
}

fun CpuState.subb_a_ri(ri: Int): Int {
    pc++
    val pointer = getRegister(ri)
    val a = ACC.toInt()
    val b = readIndirect(pointer).toInt()
    val cyIn = if ((PSW.toInt() and CY_BIT) != 0) 1 else 0
    val result = a - b - cyIn
    val acResult = (a and 0x0F) - (b and 0x0F) - cyIn
    val borrow6 = if (((a and 0x7F) - (b and 0x7F) - cyIn) < 0) 1 else 0
    val borrow7 = if (result < 0) 1 else 0
    setFlag(CY_BIT, result < 0)
    setFlag(AC_BIT, acResult < 0)
    setFlag(OV_BIT, (borrow6 xor borrow7) != 0)
    ACC = (result and 0xFF).toUByte()
    return 1
}

fun CpuState.subb_a_rn(rn: Int): Int {
    pc++
    val a = ACC.toInt()
    val b = getRegister(rn).toInt()
    val cyIn = if ((PSW.toInt() and CY_BIT) != 0) 1 else 0
    val result = a - b - cyIn
    val acResult = (a and 0x0F) - (b and 0x0F) - cyIn
    val borrow6 = if (((a and 0x7F) - (b and 0x7F) - cyIn) < 0) 1 else 0
    val borrow7 = if (result < 0) 1 else 0
    setFlag(CY_BIT, result < 0)
    setFlag(AC_BIT, acResult < 0)
    setFlag(OV_BIT, (borrow6 xor borrow7) != 0)
    ACC = (result and 0xFF).toUByte()
    return 1
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
    setFlag(CY_BIT, false)
    setFlag(OV_BIT, product > 0xFF)
    ACC = (product and 0xFF).toUByte()
    B = ((product shr 8) and 0xFF).toUByte()
    return 4
}

fun CpuState.div_ab(): Int {
    pc++
    val a = ACC.toInt()
    val b = B.toInt()
    setFlag(CY_BIT, false)
    if (b == 0) {
        setFlag(OV_BIT, true)
    } else {
        ACC = (a / b).toUByte()
        B = (a % b).toUByte()
        setFlag(OV_BIT, false)
    }
    return 4
}

@OptIn(ExperimentalUnsignedTypes::class)
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
