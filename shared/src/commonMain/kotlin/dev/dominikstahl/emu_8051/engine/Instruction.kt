package dev.dominikstahl.emu_8051.engine

sealed class OpcodePattern {

    class Fixed(val opcode: Int) : OpcodePattern()

    class RegisterRange(val base: Int) : OpcodePattern()

    class IndirectRange(val base: Int) : OpcodePattern()

    class PageRange(val base: Int, val step: Int = 0x20) : OpcodePattern()
}

enum class Operand {
    A, C, AB, DPTR,
    RN, RI,
    DIRECT, IMM8, IMM16,
    REL, ADDR11, ADDR16,
    BIT, NOT_BIT,
    AT_DPTR, AT_A_DPTR, AT_A_PC,
}

enum class Instruction(
    val mnemonic: String,
    val bytes: Int,
    val cycles: Int,
    val operands: List<Operand>,
    val pattern: OpcodePattern,
) {
    // ========================================================================
    // NO OPERATION
    // ========================================================================
    NOP("NOP", 1, 1, listOf(), OpcodePattern.Fixed(0x00)),

    // ========================================================================
    // ARITHMETIC: ADD, ADDC, SUBB, INC, DEC, MUL, DIV, DA
    // ========================================================================
    ADD_A_IMM("ADD", 2, 1, listOf(Operand.A, Operand.IMM8), OpcodePattern.Fixed(0x24)),
    ADD_A_DIRECT("ADD", 2, 1, listOf(Operand.A, Operand.DIRECT), OpcodePattern.Fixed(0x25)),
    ADD_A_RI("ADD", 1, 1, listOf(Operand.A, Operand.RI), OpcodePattern.IndirectRange(0x26)),
    ADD_A_RN("ADD", 1, 1, listOf(Operand.A, Operand.RN), OpcodePattern.RegisterRange(0x28)),

    ADDC_A_IMM("ADDC", 2, 1, listOf(Operand.A, Operand.IMM8), OpcodePattern.Fixed(0x34)),
    ADDC_A_DIRECT("ADDC", 2, 1, listOf(Operand.A, Operand.DIRECT), OpcodePattern.Fixed(0x35)),
    ADDC_A_RI("ADDC", 1, 1, listOf(Operand.A, Operand.RI), OpcodePattern.IndirectRange(0x36)),
    ADDC_A_RN("ADDC", 1, 1, listOf(Operand.A, Operand.RN), OpcodePattern.RegisterRange(0x38)),

    SUBB_A_IMM("SUBB", 2, 1, listOf(Operand.A, Operand.IMM8), OpcodePattern.Fixed(0x94)),
    SUBB_A_DIRECT("SUBB", 2, 1, listOf(Operand.A, Operand.DIRECT), OpcodePattern.Fixed(0x95)),
    SUBB_A_RI("SUBB", 1, 1, listOf(Operand.A, Operand.RI), OpcodePattern.IndirectRange(0x96)),
    SUBB_A_RN("SUBB", 1, 1, listOf(Operand.A, Operand.RN), OpcodePattern.RegisterRange(0x98)),

    INC_A("INC", 1, 1, listOf(Operand.A), OpcodePattern.Fixed(0x04)),
    INC_DIRECT("INC", 2, 1, listOf(Operand.DIRECT), OpcodePattern.Fixed(0x05)),
    INC_RI("INC", 1, 1, listOf(Operand.RI), OpcodePattern.IndirectRange(0x06)),
    INC_RN("INC", 1, 1, listOf(Operand.RN), OpcodePattern.RegisterRange(0x08)),
    INC_DPTR("INC", 1, 2, listOf(Operand.DPTR), OpcodePattern.Fixed(0xA3)),

    DEC_A("DEC", 1, 1, listOf(Operand.A), OpcodePattern.Fixed(0x14)),
    DEC_DIRECT("DEC", 2, 1, listOf(Operand.DIRECT), OpcodePattern.Fixed(0x15)),
    DEC_RI("DEC", 1, 1, listOf(Operand.RI), OpcodePattern.IndirectRange(0x16)),
    DEC_RN("DEC", 1, 1, listOf(Operand.RN), OpcodePattern.RegisterRange(0x18)),

    MUL_AB("MUL", 1, 4, listOf(Operand.AB), OpcodePattern.Fixed(0xA4)),
    DIV_AB("DIV", 1, 4, listOf(Operand.AB), OpcodePattern.Fixed(0x84)),
    DA_A("DA", 1, 1, listOf(Operand.A), OpcodePattern.Fixed(0xD4)),

    // ========================================================================
    // LOGICAL: ANL, ORL, XRL
    // ========================================================================
    ANL_A_IMM("ANL", 2, 1, listOf(Operand.A, Operand.IMM8), OpcodePattern.Fixed(0x54)),
    ANL_A_DIRECT("ANL", 2, 1, listOf(Operand.A, Operand.DIRECT), OpcodePattern.Fixed(0x55)),
    ANL_A_RI("ANL", 1, 1, listOf(Operand.A, Operand.RI), OpcodePattern.IndirectRange(0x56)),
    ANL_A_RN("ANL", 1, 1, listOf(Operand.A, Operand.RN), OpcodePattern.RegisterRange(0x58)),
    ANL_DIRECT_A("ANL", 2, 1, listOf(Operand.DIRECT, Operand.A), OpcodePattern.Fixed(0x52)),
    ANL_DIRECT_IMM("ANL", 3, 2, listOf(Operand.DIRECT, Operand.IMM8), OpcodePattern.Fixed(0x53)),
    ANL_C_BIT("ANL", 2, 2, listOf(Operand.C, Operand.BIT), OpcodePattern.Fixed(0x82)),
    ANL_C_NOT_BIT("ANL", 2, 2, listOf(Operand.C, Operand.NOT_BIT), OpcodePattern.Fixed(0xB0)),

    ORL_A_IMM("ORL", 2, 1, listOf(Operand.A, Operand.IMM8), OpcodePattern.Fixed(0x44)),
    ORL_A_DIRECT("ORL", 2, 1, listOf(Operand.A, Operand.DIRECT), OpcodePattern.Fixed(0x45)),
    ORL_A_RI("ORL", 1, 1, listOf(Operand.A, Operand.RI), OpcodePattern.IndirectRange(0x46)),
    ORL_A_RN("ORL", 1, 1, listOf(Operand.A, Operand.RN), OpcodePattern.RegisterRange(0x48)),
    ORL_DIRECT_A("ORL", 2, 1, listOf(Operand.DIRECT, Operand.A), OpcodePattern.Fixed(0x42)),
    ORL_DIRECT_IMM("ORL", 3, 2, listOf(Operand.DIRECT, Operand.IMM8), OpcodePattern.Fixed(0x43)),
    ORL_C_BIT("ORL", 2, 2, listOf(Operand.C, Operand.BIT), OpcodePattern.Fixed(0x72)),
    ORL_C_NOT_BIT("ORL", 2, 2, listOf(Operand.C, Operand.NOT_BIT), OpcodePattern.Fixed(0xA0)),

    XRL_A_IMM("XRL", 2, 1, listOf(Operand.A, Operand.IMM8), OpcodePattern.Fixed(0x64)),
    XRL_A_DIRECT("XRL", 2, 1, listOf(Operand.A, Operand.DIRECT), OpcodePattern.Fixed(0x65)),
    XRL_A_RI("XRL", 1, 1, listOf(Operand.A, Operand.RI), OpcodePattern.IndirectRange(0x66)),
    XRL_A_RN("XRL", 1, 1, listOf(Operand.A, Operand.RN), OpcodePattern.RegisterRange(0x68)),
    XRL_DIRECT_A("XRL", 2, 1, listOf(Operand.DIRECT, Operand.A), OpcodePattern.Fixed(0x62)),
    XRL_DIRECT_IMM("XRL", 3, 2, listOf(Operand.DIRECT, Operand.IMM8), OpcodePattern.Fixed(0x63)),

    // ========================================================================
    // DATA MOVEMENT: MOV, MOVC, MOVX
    // ========================================================================
    MOV_A_IMM("MOV", 2, 1, listOf(Operand.A, Operand.IMM8), OpcodePattern.Fixed(0x74)),
    MOV_A_DIRECT("MOV", 2, 1, listOf(Operand.A, Operand.DIRECT), OpcodePattern.Fixed(0xE5)),
    MOV_A_RI("MOV", 1, 1, listOf(Operand.A, Operand.RI), OpcodePattern.IndirectRange(0xE6)),
    MOV_A_RN("MOV", 1, 1, listOf(Operand.A, Operand.RN), OpcodePattern.RegisterRange(0xE8)),

    MOV_RN_A("MOV", 1, 1, listOf(Operand.RN, Operand.A), OpcodePattern.RegisterRange(0xF8)),
    MOV_RN_DIRECT("MOV", 2, 2, listOf(Operand.RN, Operand.DIRECT), OpcodePattern.RegisterRange(0xA8)),
    MOV_RN_IMM("MOV", 2, 1, listOf(Operand.RN, Operand.IMM8), OpcodePattern.RegisterRange(0x78)),

    MOV_DIRECT_A("MOV", 2, 1, listOf(Operand.DIRECT, Operand.A), OpcodePattern.Fixed(0xF5)),
    MOV_DIRECT_RN("MOV", 2, 2, listOf(Operand.DIRECT, Operand.RN), OpcodePattern.RegisterRange(0x88)),
    MOV_DIRECT_DIRECT("MOV", 3, 2, listOf(Operand.DIRECT, Operand.DIRECT), OpcodePattern.Fixed(0x85)),
    MOV_DIRECT_RI("MOV", 2, 2, listOf(Operand.DIRECT, Operand.RI), OpcodePattern.IndirectRange(0x86)),
    MOV_DIRECT_IMM("MOV", 3, 2, listOf(Operand.DIRECT, Operand.IMM8), OpcodePattern.Fixed(0x75)),

    MOV_RI_A("MOV", 1, 1, listOf(Operand.RI, Operand.A), OpcodePattern.IndirectRange(0xF6)),
    MOV_RI_DIRECT("MOV", 2, 2, listOf(Operand.RI, Operand.DIRECT), OpcodePattern.IndirectRange(0xA6)),
    MOV_RI_IMM("MOV", 2, 1, listOf(Operand.RI, Operand.IMM8), OpcodePattern.IndirectRange(0x76)),

    MOV_C_BIT("MOV", 2, 1, listOf(Operand.C, Operand.BIT), OpcodePattern.Fixed(0xA2)),
    MOV_BIT_C("MOV", 2, 2, listOf(Operand.BIT, Operand.C), OpcodePattern.Fixed(0x92)),
    MOV_DPTR_IMM16("MOV", 3, 2, listOf(Operand.DPTR, Operand.IMM16), OpcodePattern.Fixed(0x90)),

    MOVC_A_AT_A_DPTR("MOVC", 1, 2, listOf(Operand.A, Operand.AT_A_DPTR), OpcodePattern.Fixed(0x93)),
    MOVC_A_AT_A_PC("MOVC", 1, 2, listOf(Operand.A, Operand.AT_A_PC), OpcodePattern.Fixed(0x83)),

    MOVX_A_AT_DPTR("MOVX", 1, 2, listOf(Operand.A, Operand.AT_DPTR), OpcodePattern.Fixed(0xE0)),
    MOVX_A_RI("MOVX", 1, 2, listOf(Operand.A, Operand.RI), OpcodePattern.IndirectRange(0xE2)),
    MOVX_AT_DPTR_A("MOVX", 1, 2, listOf(Operand.AT_DPTR, Operand.A), OpcodePattern.Fixed(0xF0)),
    MOVX_RI_A("MOVX", 1, 2, listOf(Operand.RI, Operand.A), OpcodePattern.IndirectRange(0xF2)),

    // ========================================================================
    // UNCONDITIONAL JUMPS: SJMP, AJMP, LJMP, JMP @A+DPTR
    // ========================================================================
    SJMP("SJMP", 2, 2, listOf(Operand.REL), OpcodePattern.Fixed(0x80)),
    AJMP("AJMP", 2, 2, listOf(Operand.ADDR11), OpcodePattern.PageRange(0x01)),
    LJMP("LJMP", 3, 2, listOf(Operand.ADDR16), OpcodePattern.Fixed(0x02)),
    JMP_AT_A_DPTR("JMP", 1, 2, listOf(), OpcodePattern.Fixed(0x73)),

    // ========================================================================
    // CONDITIONAL JUMPS: JC, JNC, JZ, JNZ, JB, JNB, JBC
    // ========================================================================
    JC("JC", 2, 2, listOf(Operand.REL), OpcodePattern.Fixed(0x40)),
    JNC("JNC", 2, 2, listOf(Operand.REL), OpcodePattern.Fixed(0x50)),
    JZ("JZ", 2, 2, listOf(Operand.REL), OpcodePattern.Fixed(0x60)),
    JNZ("JNZ", 2, 2, listOf(Operand.REL), OpcodePattern.Fixed(0x70)),
    JB("JB", 3, 2, listOf(Operand.BIT, Operand.REL), OpcodePattern.Fixed(0x20)),
    JNB("JNB", 3, 2, listOf(Operand.BIT, Operand.REL), OpcodePattern.Fixed(0x30)),
    JBC("JBC", 3, 2, listOf(Operand.BIT, Operand.REL), OpcodePattern.Fixed(0x10)),

    // ========================================================================
    // COMPARE & JUMP: CJNE, DJNZ
    // ========================================================================
    CJNE_A_IMM("CJNE", 3, 2, listOf(Operand.A, Operand.IMM8, Operand.REL), OpcodePattern.Fixed(0xB4)),
    CJNE_A_DIRECT("CJNE", 3, 2, listOf(Operand.A, Operand.DIRECT, Operand.REL), OpcodePattern.Fixed(0xB5)),
    CJNE_RI_IMM("CJNE", 3, 2, listOf(Operand.RI, Operand.IMM8, Operand.REL), OpcodePattern.IndirectRange(0xB6)),
    CJNE_RN_IMM("CJNE", 3, 2, listOf(Operand.RN, Operand.IMM8, Operand.REL), OpcodePattern.RegisterRange(0xB8)),

    DJNZ_DIRECT("DJNZ", 3, 2, listOf(Operand.DIRECT, Operand.REL), OpcodePattern.Fixed(0xD5)),
    DJNZ_RN("DJNZ", 2, 2, listOf(Operand.RN, Operand.REL), OpcodePattern.RegisterRange(0xD8)),

    // ========================================================================
    // CALLS & RETURNS: ACALL, LCALL, RET, RETI
    // ========================================================================
    ACALL("ACALL", 2, 2, listOf(Operand.ADDR11), OpcodePattern.PageRange(0x11)),
    LCALL("LCALL", 3, 2, listOf(Operand.ADDR16), OpcodePattern.Fixed(0x12)),
    RET("RET", 1, 2, listOf(), OpcodePattern.Fixed(0x22)),
    RETI("RETI", 1, 2, listOf(), OpcodePattern.Fixed(0x32)),

    // ========================================================================
    // STACK: PUSH, POP
    // ========================================================================
    PUSH_DIRECT("PUSH", 2, 2, listOf(Operand.DIRECT), OpcodePattern.Fixed(0xC0)),
    POP_DIRECT("POP", 2, 2, listOf(Operand.DIRECT), OpcodePattern.Fixed(0xD0)),

    // ========================================================================
    // BIT OPERATIONS: CLR, SETB, CPL
    // ========================================================================
    CLR_A("CLR", 1, 1, listOf(Operand.A), OpcodePattern.Fixed(0xE4)),
    CLR_C("CLR", 1, 1, listOf(Operand.C), OpcodePattern.Fixed(0xC3)),
    CLR_BIT("CLR", 2, 1, listOf(Operand.BIT), OpcodePattern.Fixed(0xC2)),

    SETB_C("SETB", 1, 1, listOf(Operand.C), OpcodePattern.Fixed(0xD3)),
    SETB_BIT("SETB", 2, 1, listOf(Operand.BIT), OpcodePattern.Fixed(0xD2)),

    CPL_A("CPL", 1, 1, listOf(Operand.A), OpcodePattern.Fixed(0xF4)),
    CPL_C("CPL", 1, 1, listOf(Operand.C), OpcodePattern.Fixed(0xB3)),
    CPL_BIT("CPL", 2, 1, listOf(Operand.BIT), OpcodePattern.Fixed(0xB2)),

    // ========================================================================
    // ROTATE & SWAP: RR, RRC, RL, RLC, SWAP
    // ========================================================================
    RR_A("RR", 1, 1, listOf(Operand.A), OpcodePattern.Fixed(0x03)),
    RRC_A("RRC", 1, 1, listOf(Operand.A), OpcodePattern.Fixed(0x13)),
    RL_A("RL", 1, 1, listOf(Operand.A), OpcodePattern.Fixed(0x23)),
    RLC_A("RLC", 1, 1, listOf(Operand.A), OpcodePattern.Fixed(0x33)),
    SWAP_A("SWAP", 1, 1, listOf(Operand.A), OpcodePattern.Fixed(0xC4)),

    // ========================================================================
    // EXCHANGE: XCH, XCHD
    // ========================================================================
    XCH_A_DIRECT("XCH", 2, 1, listOf(Operand.A, Operand.DIRECT), OpcodePattern.Fixed(0xC5)),
    XCH_A_RI("XCH", 1, 1, listOf(Operand.A, Operand.RI), OpcodePattern.IndirectRange(0xC6)),
    XCH_A_RN("XCH", 1, 1, listOf(Operand.A, Operand.RN), OpcodePattern.RegisterRange(0xC8)),
    XCHD_A_RI("XCHD", 1, 1, listOf(Operand.A, Operand.RI), OpcodePattern.IndirectRange(0xD6)),
    ;

    companion object {
        val byMnemonic: Map<String, List<Instruction>> = entries.groupBy { it.mnemonic }

        fun lookup(mnemonic: String, vararg operands: Operand): Instruction? =
            byMnemonic[mnemonic]?.find { inst ->
                inst.operands == operands.toList()
            }
    }
}
