package dev.dominikstahl.emu_8051.engine

@OptIn(ExperimentalUnsignedTypes::class)
class Interpreter(val state: CpuState) {

    val interruptController = InterruptController(state).also { state.interruptController = it }
    val timerController = TimerController(state).also { state.timerController = it }

    fun step(): Int {
        val opcode = state.rom[state.pc].toInt()

        state.interruptControllerAccessFlag = false
        interruptController.lastInterruptCycles = 0

        val cycles = when (opcode) {
            0x00 -> state.nop()
            0x01, 0x21, 0x41, 0x61, 0x81, 0xA1, 0xC1, 0xE1 -> state.ajmp(opcode)
            0x02 -> state.ljmp()
            0x03 -> state.rr_a()
            0x04 -> state.inc_a()
            0x05 -> state.inc_direct()
            0x06 -> state.inc_ri(0)
            0x07 -> state.inc_ri(1)
            0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F -> state.inc_rn(opcode and 0x07)
            0x10 -> state.jbc()
            0x11, 0x31, 0x51, 0x71, 0x91, 0xB1, 0xD1, 0xF1 -> state.acall(opcode)
            0x12 -> state.lcall()
            0x13 -> state.rrc_a()
            0x14 -> state.dec_a()
            0x15 -> state.dec_direct()
            0x16 -> state.dec_ri(0)
            0x17 -> state.dec_ri(1)
            0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x1F -> state.dec_rn(opcode and 0x07)
            0x20 -> state.jb()
            0x22 -> state.ret()
            0x23 -> state.rl_a()
            0x24 -> state.add_a_imm()
            0x25 -> state.add_a_direct()
            0x26 -> state.add_a_ri(0)
            0x27 -> state.add_a_ri(1)
            0x28, 0x29, 0x2A, 0x2B, 0x2C, 0x2D, 0x2E, 0x2F -> state.add_a_rn(opcode and 0x07)
            0x30 -> state.jnb()
            0x32 -> state.reti()
            0x33 -> state.rlc_a()
            0x34 -> state.addc_a_imm()
            0x35 -> state.addc_a_direct()
            0x36 -> state.addc_a_ri(0)
            0x37 -> state.addc_a_ri(1)
            0x38, 0x39, 0x3A, 0x3B, 0x3C, 0x3D, 0x3E, 0x3F -> state.addc_a_rn(opcode and 0x07)
            0x40 -> state.jc()
            0x42 -> state.orl_direct_a()
            0x43 -> state.orl_direct_imm()
            0x44 -> state.orl_a_imm()
            0x45 -> state.orl_a_direct()
            0x46 -> state.orl_a_ri(0)
            0x47 -> state.orl_a_ri(1)
            0x48, 0x49, 0x4A, 0x4B, 0x4C, 0x4D, 0x4E, 0x4F -> state.orl_a_rn(opcode and 0x07)
            0x50 -> state.jnc()
            0x52 -> state.anl_direct_a()
            0x53 -> state.anl_direct_imm()
            0x54 -> state.anl_a_imm()
            0x55 -> state.anl_a_direct()
            0x56 -> state.anl_a_ri(0)
            0x57 -> state.anl_a_ri(1)
            0x58, 0x59, 0x5A, 0x5B, 0x5C, 0x5D, 0x5E, 0x5F -> state.anl_a_rn(opcode and 0x07)
            0x60 -> state.jz()
            0x62 -> state.xrl_direct_a()
            0x63 -> state.xrl_direct_imm()
            0x64 -> state.xrl_a_imm()
            0x65 -> state.xrl_a_direct()
            0x66 -> state.xrl_a_ri(0)
            0x67 -> state.xrl_a_ri(1)
            0x68, 0x69, 0x6A, 0x6B, 0x6C, 0x6D, 0x6E, 0x6F -> state.xrl_a_rn(opcode and 0x07)
            0x70 -> state.jnz()
            0x72 -> state.orl_c_bit()
            0x73 -> state.jmp_at_a_dptr()
            0x74 -> state.mov_a_imm()
            0x75 -> state.mov_direct_imm()
            0x76 -> state.mov_ri_imm(0)
            0x77 -> state.mov_ri_imm(1)
            0x78, 0x79, 0x7A, 0x7B, 0x7C, 0x7D, 0x7E, 0x7F -> state.mov_rn_imm(opcode and 0x07)
            0x80 -> state.sjmp()
            0x82 -> state.anl_c_bit()
            0x83 -> state.movc_a_at_a_pc()
            0x84 -> state.div_ab()
            0x85 -> state.mov_direct_direct()
            0x86 -> state.mov_direct_ri(0)
            0x87 -> state.mov_direct_ri(1)
            0x88, 0x89, 0x8A, 0x8B, 0x8C, 0x8D, 0x8E, 0x8F -> state.mov_direct_rn(opcode and 0x07)
            0x90 -> state.mov_dptr_imm16()
            0x92 -> state.mov_bit_c()
            0x93 -> state.movc_a_at_a_dptr()
            0x94 -> state.subb_a_imm()
            0x95 -> state.subb_a_direct()
            0x96 -> state.subb_a_ri(0)
            0x97 -> state.subb_a_ri(1)
            0x98, 0x99, 0x9A, 0x9B, 0x9C, 0x9D, 0x9E, 0x9F -> state.subb_a_rn(opcode and 0x07)
            0xA0 -> state.orl_c_not_bit()
            0xA2 -> state.mov_c_bit()
            0xA3 -> state.inc_dptr()
            0xA4 -> state.mul_ab()
            0xA6 -> state.mov_ri_direct(0)
            0xA7 -> state.mov_ri_direct(1)
            0xA8, 0xA9, 0xAA, 0xAB, 0xAC, 0xAD, 0xAE, 0xAF -> state.mov_rn_direct(opcode and 0x07)
            0xB0 -> state.anl_c_not_bit()
            0xB2 -> state.cpl_bit()
            0xB3 -> state.cpl_c()
            0xB4 -> state.cjne_a_imm()
            0xB5 -> state.cjne_a_direct()
            0xB6 -> state.cjne_ri_imm(0)
            0xB7 -> state.cjne_ri_imm(1)
            0xB8, 0xB9, 0xBA, 0xBB, 0xBC, 0xBD, 0xBE, 0xBF -> state.cjne_rn_imm(opcode and 0x07)
            0xC0 -> state.push_direct()
            0xC2 -> state.clr_bit()
            0xC3 -> state.clr_c()
            0xC4 -> state.swap_a()
            0xC5 -> state.xch_a_direct()
            0xC6 -> state.xch_a_ri(0)
            0xC7 -> state.xch_a_ri(1)
            0xC8, 0xC9, 0xCA, 0xCB, 0xCC, 0xCD, 0xCE, 0xCF -> state.xch_a_rn(opcode and 0x07)
            0xD0 -> state.pop_direct()
            0xD2 -> state.setb_bit()
            0xD3 -> state.setb_c()
            0xD4 -> state.da_a()
            0xD5 -> state.djnz_direct()
            0xD6 -> state.xchd_a_ri(0)
            0xD7 -> state.xchd_a_ri(1)
            0xD8, 0xD9, 0xDA, 0xDB, 0xDC, 0xDD, 0xDE, 0xDF -> state.djnz_rn(opcode and 0x07)
            0xE0 -> state.movx_a_at_dptr()
            0xE2 -> state.movx_a_at_ri(0)
            0xE3 -> state.movx_a_at_ri(1)
            0xE4 -> state.clr_a()
            0xE5 -> state.mov_a_direct()
            0xE6 -> state.mov_a_ri(0)
            0xE7 -> state.mov_a_ri(1)
            0xE8, 0xE9, 0xEA, 0xEB, 0xEC, 0xED, 0xEE, 0xEF -> state.mov_a_rn(opcode and 0x07)
            0xF0 -> state.movx_at_dptr_a()
            0xF2 -> state.movx_at_ri_a(0)
            0xF3 -> state.movx_at_ri_a(1)
            0xF4 -> state.cpl_a()
            0xF5 -> state.mov_direct_a()
            0xF6 -> state.mov_ri_a(0)
            0xF7 -> state.mov_ri_a(1)
            0xF8, 0xF9, 0xFA, 0xFB, 0xFC, 0xFD, 0xFE, 0xFF -> state.mov_rn_a(opcode and 0x07)

            else -> state.handle_illegal()
        }

        state.totalCycles += cycles

        if ((state.TCON.toInt() and (TR0_BIT or TR1_BIT)) != 0 ||
            (state.T2CON.toInt() and TR2_BIT) != 0
        ) {
            repeat(cycles) { timerController.tick() }
        }

        interruptController.sample()

        if (opcode != 0x32 && !state.interruptControllerAccessFlag) {
            interruptController.poll()
        }

        state.totalCycles += interruptController.lastInterruptCycles

        return cycles + interruptController.lastInterruptCycles
    }
}
