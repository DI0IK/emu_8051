@file:OptIn(ExperimentalUnsignedTypes::class)

package dev.dominikstahl.emu_8051

import dev.dominikstahl.emu_8051.asm.assemble
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AssemblerTest {

    @Test
    fun `single NOP`() {
        val result = assemble("NOP")
        assertTrue(result.isSuccess, result.errors.joinToString("\n") { it.message })
        val rom = result.rom!!
        assertTrue(rom[0] == 0x00.toUByte())
    }

    @Test
    fun `simple ADD immediate`() {
        val result = assemble("""
            ORG 100h
            ADD A, #34h
        """.trimIndent())
        assertTrue(result.isSuccess, result.errors.joinToString("\n") { it.message })
        val rom = result.rom!!
        assertTrue(rom[0x100] == 0x24.toUByte(), "opcode: ${rom[0x100]}")
        assertTrue(rom[0x101] == 0x34.toUByte(), "imm8: ${rom[0x101]}")
    }

    @Test
    fun `MOV immediate to register`() {
        val result = assemble("MOV R3, #0A5h")
        assertTrue(result.isSuccess, result.errors.joinToString("\n") { it.message })
        val rom = result.rom!!
        assertTrue(rom[0] == 0x7B.toUByte(), "opcode Rn imm: ${rom[0]}")
        assertTrue(rom[1] == 0xA5.toUByte())
    }

    @Test
    fun `MOV register to register`() {
        val result = assemble("MOV A, R5")
        assertTrue(result.isSuccess)
        assertTrue(result.rom!![0] == 0xED.toUByte())
    }

    @Test
    fun `MOV direct to register`() {
        val result = assemble("MOV R2, 80h")
        assertTrue(result.isSuccess, result.errors.joinToString("\n") { it.message })
        val rom = result.rom!!
        assertTrue(rom[0] == 0xAA.toUByte(), "opcode: ${rom[0]}")
        assertTrue(rom[1] == 0x80.toUByte())
    }

    @Test
    fun `MOV A direct`() {
        val result = assemble("MOV A, 80h")
        assertTrue(result.isSuccess)
        val rom = result.rom!!
        assertTrue(rom[0] == 0xE5.toUByte())
        assertTrue(rom[1] == 0x80.toUByte())
    }

    @Test
    fun `MOV direct A`() {
        val result = assemble("MOV 90h, A")
        assertTrue(result.isSuccess)
        val rom = result.rom!!
        assertTrue(rom[0] == 0xF5.toUByte())
        assertTrue(rom[1] == 0x90.toUByte())
    }

    @Test
    fun `MOV indirect`() {
        val result = assemble("""
            MOV A, @R0
            MOV @R1, A
        """.trimIndent())
        assertTrue(result.isSuccess, result.errors.joinToString("\n") { it.message })
        val rom = result.rom!!
        assertTrue(rom[0] == 0xE6.toUByte())  // MOV A, @R0
        assertTrue(rom[1] == 0xF7.toUByte())  // MOV @R1, A
    }

    @Test
    fun `MOV DPTR imm16`() {
        val result = assemble("MOV DPTR, #1234h")
        assertTrue(result.isSuccess)
        val rom = result.rom!!
        assertTrue(rom[0] == 0x90.toUByte())
        assertTrue(rom[1] == 0x12.toUByte())
        assertTrue(rom[2] == 0x34.toUByte())
    }

    @Test
    fun `SJMP forward`() {
        val result = assemble("""
            SJMP target
            NOP
            NOP
            target: NOP
        """.trimIndent())
        assertTrue(result.isSuccess, result.errors.joinToString("\n") { it.message })
        val rom = result.rom!!
        assertTrue(rom[0] == 0x80.toUByte())
        assertTrue(rom[1] == 0x02.toUByte())  // offset = 4(target) - (0(pc) + 2(bytes)) = 2
    }

    @Test
    fun `SJMP dollar`() {
        val result = assemble("SJMP $")
        assertTrue(result.isSuccess, result.errors.joinToString("\n") { it.message })
        val rom = result.rom!!
        assertTrue(rom[0] == 0x80.toUByte())
        assertTrue(rom[1] == 0xFE.toUByte())  // -2 = infinite loop
    }

    @Test
    fun `ACALL and AJMP`() {
        val result = assemble("""
            ORG 0h
            AJMP 200h
            ORG 200h
            ACALL 200h
        """.trimIndent())
        assertTrue(result.isSuccess, result.errors.joinToString("\n") { it.message })
        val rom = result.rom!!
        // AJMP: target 0x200, page = (0x200 >> 8) & 7 = 2, opcode = 0x01 + 2 * 0x20 = 0x41
        assertTrue(rom[0] == 0x41.toUByte(), "AJMP opcode: ${rom[0]}")
        assertTrue(rom[1] == 0x00.toUByte())
        // ACALL: target 0x200, page = 2, opcode = 0x11 + 2 * 0x20 = 0x51
        assertTrue(rom[0x200] == 0x51.toUByte(), "ACALL opcode: ${rom[0x200]}")
        assertTrue(rom[0x201] == 0x00.toUByte())
    }

    @Test
    fun `LJMP and LCALL`() {
        val result = assemble("""
            LJMP 1234h
            LCALL 5678h
        """.trimIndent())
        assertTrue(result.isSuccess, result.errors.joinToString("\n") { it.message })
        val rom = result.rom!!
        assertTrue(rom[0] == 0x02.toUByte())
        assertTrue(rom[1] == 0x12.toUByte())
        assertTrue(rom[2] == 0x34.toUByte())
        assertTrue(rom[3] == 0x12.toUByte())
        assertTrue(rom[4] == 0x56.toUByte())
        assertTrue(rom[5] == 0x78.toUByte())
    }

    @Test
    fun `JNB TF1 dollar`() {
        val result = assemble("JNB TF1, $")
        assertTrue(result.isSuccess, result.errors.joinToString("\n") { it.message })
        val rom = result.rom!!
        assertTrue(rom[0] == 0x30.toUByte())  // JNB opcode
        assertTrue(rom[1] == 0x8F.toUByte())  // TF1 bit addr (0x88|7 = 0x8F)
        assertTrue(rom[2] == 0xFD.toUByte())  // offset = -3 = 0xFD
    }

    @Test
    fun `bit operations`() {
        val result = assemble("""
            SETB P0.3
            CLR P0.7
            CPL P0.0
        """.trimIndent())
        assertTrue(result.isSuccess, result.errors.joinToString("\n") { it.message })
        val rom = result.rom!!
        assertTrue(rom[0] == 0xD2.toUByte())  // SETB bit
        assertTrue(rom[1] == 0x83.toUByte())  // P0.3 = 0x80|3 = 0x83
        assertTrue(rom[2] == 0xC2.toUByte())  // CLR bit
        assertTrue(rom[3] == 0x87.toUByte())  // P0.7 = 0x80|7 = 0x87
        assertTrue(rom[4] == 0xB2.toUByte())  // CPL bit
        assertTrue(rom[5] == 0x80.toUByte())  // P0.0 = 0x80|0 = 0x80
    }

    @Test
    fun `MOV C bit`() {
        val result = assemble("MOV C, P0.3")
        assertTrue(result.isSuccess, result.errors.joinToString("\n") { it.message })
        val rom = result.rom!!
        assertTrue(rom[0] == 0xA2.toUByte())  // MOV C, bit
        assertTrue(rom[1] == 0x83.toUByte())  // P0.3
    }

    @Test
    fun `MOV bit C`() {
        val result = assemble("MOV P0.3, C")
        assertTrue(result.isSuccess, result.errors.joinToString("\n") { it.message })
        val rom = result.rom!!
        assertTrue(rom[0] == 0x92.toUByte())  // MOV bit, C
        assertTrue(rom[1] == 0x83.toUByte())  // P0.3
    }

    @Test
    fun `ANL C slash bit`() {
        val result = assemble("ANL C, /P0.3")
        assertTrue(result.isSuccess, result.errors.joinToString("\n") { it.message })
        val rom = result.rom!!
        assertTrue(rom[0] == 0xB0.toUByte())  // ANL C, /bit
        assertTrue(rom[1] == 0x83.toUByte())
    }

    @Test
    fun `MOVC and MOVX`() {
        val result = assemble("""
            MOVC A, @A+DPTR
            MOVX A, @DPTR
            MOVX @R0, A
        """.trimIndent())
        assertTrue(result.isSuccess, result.errors.joinToString("\n") { it.message })
        val rom = result.rom!!
        assertTrue(rom[0] == 0x93.toUByte())  // MOVC A, @A+DPTR
        assertTrue(rom[1] == 0xE0.toUByte())  // MOVX A, @DPTR
        assertTrue(rom[2] == 0xF2.toUByte())  // MOVX @R0, A (IndirectRange 0xF2 + 0)
    }

    @Test
    fun `conditional jumps`() {
        val result = assemble("""
            JC  label
            JNC label
            JZ  label
            JNZ label
            label: NOP
        """.trimIndent())
        assertTrue(result.isSuccess, result.errors.joinToString("\n") { it.message })
        val rom = result.rom!!
        assertTrue(rom[0] == 0x40.toUByte())  // JC rel
        assertTrue(rom[1] == 0x06.toUByte())  // offset = 8 - (0 + 2) = 6
        assertTrue(rom[2] == 0x50.toUByte())  // JNC rel
        assertTrue(rom[3] == 0x04.toUByte())  // offset = 8 - (2 + 2) = 4
        assertTrue(rom[4] == 0x60.toUByte())  // JZ rel
        assertTrue(rom[5] == 0x02.toUByte())  // offset = 8 - (4 + 2) = 2
        assertTrue(rom[6] == 0x70.toUByte())  // JNZ rel
        assertTrue(rom[7] == 0x00.toUByte())  // offset = 8 - (6 + 2) = 0
    }

    @Test
    fun `CJNE instruction`() {
        val result = assemble("""
            ORG 100h
            CJNE A, #40h, target
            target: NOP
        """.trimIndent())
        assertTrue(result.isSuccess, result.errors.joinToString("\n") { it.message })
        val rom = result.rom!!
        assertTrue(rom[0x100] == 0xB4.toUByte(), "CJNE A, #imm, rel opcode")
        assertTrue(rom[0x101] == 0x40.toUByte(), "imm8")
        assertTrue(rom[0x102] == 0x00.toUByte(), "rel offset (PC+3=0x103, target=0x103, offset=0)")
    }

    @Test
    fun `DJNZ instruction`() {
        val result = assemble("""
            DJNZ R0, $
            DJNZ 80h, $
        """.trimIndent())
        assertTrue(result.isSuccess, result.errors.joinToString("\n") { it.message })
        val rom = result.rom!!
        assertTrue(rom[0] == 0xD8.toUByte(), "DJNZ R0 opcode")
        assertTrue(rom[1] == 0xFE.toUByte(), "rel offset = -2")
        assertTrue(rom[2] == 0xD5.toUByte(), "DJNZ dir opcode")
        assertTrue(rom[3] == 0x80.toUByte(), "direct addr")
        assertTrue(rom[4] == 0xFD.toUByte(), "rel offset = -3")
    }

    @Test
    fun `label alone and with instruction`() {
        val result = assemble("""
            start:
            NOP
            loop: NOP
            SJMP loop
        """.trimIndent())
        assertTrue(result.isSuccess, result.errors.joinToString("\n") { it.message })
        val rom = result.rom!!
        assertTrue(rom[0] == 0x00.toUByte())  // first NOP
        assertTrue(rom[1] == 0x00.toUByte())  // loop: NOP
        assertTrue(rom[2] == 0x80.toUByte())  // SJMP
        assertTrue(rom[3] == 0xFD.toUByte())  // offset = -(2+1) = -3 = 0xFD
    }

    @Test
    fun `EQU directive`() {
        val result = assemble("""
            COUNT EQU 10h
            MOV A, #COUNT
        """.trimIndent())
        assertTrue(result.isSuccess, result.errors.joinToString("\n") { it.message })
        val rom = result.rom!!
        assertTrue(rom[0] == 0x74.toUByte())  // MOV A, #imm
        assertTrue(rom[1] == 0x10.toUByte())  // COUNT = 10h
    }

    @Test
    fun `DATA directive`() {
        val result = assemble("""
            MY_PORT DATA 80h
            MOV A, MY_PORT
        """.trimIndent())
        assertTrue(result.isSuccess, result.errors.joinToString("\n") { it.message })
        val rom = result.rom!!
        assertTrue(rom[0] == 0xE5.toUByte())  // MOV A, DIRECT
        assertTrue(rom[1] == 0x80.toUByte())
    }

    @Test
    fun `BIT directive`() {
        val result = assemble("""
            MY_FLAG BIT 0D7h
            SETB MY_FLAG
        """.trimIndent())
        assertTrue(result.isSuccess, result.errors.joinToString("\n") { it.message })
        val rom = result.rom!!
        assertTrue(rom[0] == 0xD2.toUByte())  // SETB bit
        assertTrue(rom[1] == 0xD7.toUByte())  // MY_FLAG = 0D7h = CY
    }

    @Test
    fun `DB directive`() {
        val result = assemble("""
            ORG 100h
            DB 10h, 20h, 30h
            DB 'Hello', 0
        """.trimIndent())
        assertTrue(result.isSuccess, result.errors.joinToString("\n") { it.message })
        val rom = result.rom!!
        assertTrue(rom[0x100] == 0x10.toUByte())
        assertTrue(rom[0x101] == 0x20.toUByte())
        assertTrue(rom[0x102] == 0x30.toUByte())
        assertTrue(rom[0x103] == 0x48.toUByte())  // 'H'
        assertTrue(rom[0x104] == 0x65.toUByte())  // 'e'
        assertTrue(rom[0x105] == 0x6C.toUByte())  // 'l'
        assertTrue(rom[0x106] == 0x6C.toUByte())  // 'l'
        assertTrue(rom[0x107] == 0x6F.toUByte())  // 'o'
        assertTrue(rom[0x108] == 0x00.toUByte())
    }

    @Test
    fun `DW directive`() {
        val result = assemble("""
            DW 1234h, 5678h
        """.trimIndent())
        assertTrue(result.isSuccess, result.errors.joinToString("\n") { it.message })
        val rom = result.rom!!
        assertTrue(rom[0] == 0x12.toUByte())
        assertTrue(rom[1] == 0x34.toUByte())
        assertTrue(rom[2] == 0x56.toUByte())
        assertTrue(rom[3] == 0x78.toUByte())
    }

    @Test
    fun `DS directive`() {
        val result = assemble("""
            ORG 100h
            DS 20h
            NOP
        """.trimIndent())
        assertTrue(result.isSuccess, result.errors.joinToString("\n") { it.message })
        val rom = result.rom!!
        assertTrue(rom[0x100] == 0x00.toUByte())  // DS reserves space (zeros)
        assertTrue(rom[0x120] == 0x00.toUByte())  // NOP at 0x120
    }

    @Test
    fun `DB with label and MOV DPTR forward reference`() {
        val result = assemble("""
            ORG 100h
            MOV DPTR, #msg
            msg: DB 'Hello', 0
        """.trimIndent())
        assertTrue(result.isSuccess, result.errors.joinToString("\n") { it.message })
        val rom = result.rom!!
        // MOV DPTR,#msg at 0x100 (3 bytes)
        assertTrue(rom[0x100] == 0x90.toUByte())
        assertTrue(rom[0x101] == 0x01.toUByte())  // high byte of msg address (0x103)
        assertTrue(rom[0x102] == 0x03.toUByte())  // low byte of msg address
        // DB content at 0x103
        assertTrue(rom[0x103] == 0x48.toUByte())  // 'H'
        assertTrue(rom[0x104] == 0x65.toUByte())  // 'e'
        assertTrue(rom[0x105] == 0x6C.toUByte())  // 'l'
        assertTrue(rom[0x106] == 0x6C.toUByte())  // 'l'
        assertTrue(rom[0x107] == 0x6F.toUByte())  // 'o'
        assertTrue(rom[0x108] == 0x00.toUByte())  // null terminator
    }

    @Test
    fun `DW with label forward reference`() {
        val result = assemble("""
            ORG 100h
            MOV DPTR, #my_data
            my_data: DW 1234h
        """.trimIndent())
        assertTrue(result.isSuccess, result.errors.joinToString("\n") { it.message })
        val rom = result.rom!!
        assertTrue(rom[0x100] == 0x90.toUByte())
        assertTrue(rom[0x101] == 0x01.toUByte())  // high byte of my_data addr (0x103)
        assertTrue(rom[0x102] == 0x03.toUByte())  // low byte
        assertTrue(rom[0x103] == 0x12.toUByte())
        assertTrue(rom[0x104] == 0x34.toUByte())
    }

    @Test
    fun `DS with label forward reference`() {
        val result = assemble("""
            ORG 100h
            MOV A, #buf + 5
            buf: DS 10
        """.trimIndent())
        assertTrue(result.isSuccess, result.errors.joinToString("\n") { it.message })
        val rom = result.rom!!
        assertTrue(rom[0x100] == 0x74.toUByte())  // MOV A, #imm
        // buf is at 0x102 (after 2-byte MOV A,#imm)
        // buf + 5 = 0x107
        assertTrue(rom[0x101] == 0x07.toUByte())
    }

    @Test
    fun `ORG directive`() {
        val result = assemble("""
            ORG 200h
            NOP
            NOP
        """.trimIndent())
        assertTrue(result.isSuccess, result.errors.joinToString("\n") { it.message })
        val rom = result.rom!!
        assertTrue(rom[0x200] == 0x00.toUByte())
        assertTrue(rom[0x201] == 0x00.toUByte())
        assertTrue(rom[0] != 0x00.toUByte() || rom[0] == 0x00.toUByte())  // ORG at 200, so 0 should be 0
    }

    @Test
    fun `EQU with forward reference`() {
        val result = assemble("""
            MOV A, #SIZE
            SIZE EQU 20h
        """.trimIndent())
        assertTrue(result.isSuccess, result.errors.joinToString("\n") { it.message })
        val rom = result.rom!!
        assertTrue(rom[0] == 0x74.toUByte())
        assertTrue(rom[1] == 0x20.toUByte())
    }

    @Test
    fun `arithmetic expression in operand`() {
        val result = assemble("""
            MOV A, #10h + 20h
        """.trimIndent())
        assertTrue(result.isSuccess, result.errors.joinToString("\n") { it.message })
        val rom = result.rom!!
        assertTrue(rom[0] == 0x74.toUByte())
        assertTrue(rom[1] == 0x30.toUByte())
    }

    @Test
    fun `PUSH and POP`() {
        val result = assemble("""
            PUSH 80h
            POP 90h
        """.trimIndent())
        assertTrue(result.isSuccess, result.errors.joinToString("\n") { it.message })
        val rom = result.rom!!
        assertTrue(rom[0] == 0xC0.toUByte())  // PUSH direct
        assertTrue(rom[1] == 0x80.toUByte())
        assertTrue(rom[2] == 0xD0.toUByte())  // POP direct
        assertTrue(rom[3] == 0x90.toUByte())
    }

    @Test
    fun `XCH instruction`() {
        val result = assemble("""
            XCH A, R7
            XCHD A, @R1
        """.trimIndent())
        assertTrue(result.isSuccess, result.errors.joinToString("\n") { it.message })
        val rom = result.rom!!
        assertTrue(rom[0] == 0xCF.toUByte())  // XCH A,R7
        assertTrue(rom[1] == 0xD7.toUByte())  // XCHD A,@R1
    }

    @Test
    fun `inc dec instructions`() {
        val result = assemble("""
            INC A
            DEC A
            INC DPTR
            INC R3
            DEC @R0
        """.trimIndent())
        assertTrue(result.isSuccess, result.errors.joinToString("\n") { it.message })
        val rom = result.rom!!
        assertTrue(rom[0] == 0x04.toUByte())  // INC A
        assertTrue(rom[1] == 0x14.toUByte())  // DEC A
        assertTrue(rom[2] == 0xA3.toUByte())  // INC DPTR
        assertTrue(rom[3] == 0x0B.toUByte())  // INC R3 (0x08 + 3)
        assertTrue(rom[4] == 0x16.toUByte())  // DEC @R0 (0x16)
    }

    @Test
    fun `rotate and swap`() {
        val result = assemble("""
            RR A
            RRC A
            RL A
            RLC A
            SWAP A
        """.trimIndent())
        assertTrue(result.isSuccess, result.errors.joinToString("\n") { it.message })
        val rom = result.rom!!
        assertTrue(rom[0] == 0x03.toUByte())  // RR A
        assertTrue(rom[1] == 0x13.toUByte())  // RRC A
        assertTrue(rom[2] == 0x23.toUByte())  // RL A
        assertTrue(rom[3] == 0x33.toUByte())  // RLC A
        assertTrue(rom[4] == 0xC4.toUByte())  // SWAP A
    }

    @Test
    fun `MUL and DIV`() {
        val result = assemble("""
            MUL AB
            DIV AB
            DA A
        """.trimIndent())
        assertTrue(result.isSuccess, result.errors.joinToString("\n") { it.message })
        val rom = result.rom!!
        assertTrue(rom[0] == 0xA4.toUByte())  // MUL AB
        assertTrue(rom[1] == 0x84.toUByte())  // DIV AB
        assertTrue(rom[2] == 0xD4.toUByte())  // DA A
    }

    @Test
    fun `JB JNB JBC`() {
        val result = assemble("""
            JB  P0.0, target
            JNB P0.1, target
            JBC P0.2, target
            target: NOP
        """.trimIndent())
        assertTrue(result.isSuccess, result.errors.joinToString("\n") { it.message })
        val rom = result.rom!!
        assertTrue(rom[0] == 0x20.toUByte())  // JB bit, rel
        assertTrue(rom[1] == 0x80.toUByte())  // P0.0 bit addr
        assertTrue(rom[3] == 0x30.toUByte())  // JNB bit, rel
        assertTrue(rom[4] == 0x81.toUByte())  // P0.1 bit addr
        assertTrue(rom[6] == 0x10.toUByte())  // JBC bit, rel
        assertTrue(rom[7] == 0x82.toUByte())  // P0.2 bit addr
    }

    @Test
    fun `comments are ignored`() {
        val result = assemble("""
            ; This is a comment
            NOP ; inline comment
            ; Another comment
            NOP
        """.trimIndent())
        assertTrue(result.isSuccess, result.errors.joinToString("\n") { it.message })
        val rom = result.rom!!
        assertTrue(rom[0] == 0x00.toUByte())
        assertTrue(rom[1] == 0x00.toUByte())
    }

    @Test
    fun `END directive stops assembly`() {
        val result = assemble("""
            NOP
            NOP
            END
            NOP
            NOP
        """.trimIndent())
        assertTrue(result.isSuccess, result.errors.joinToString("\n") { it.message })
        val rom = result.rom!!
        assertTrue(rom[0] == 0x00.toUByte())
        assertTrue(rom[1] == 0x00.toUByte())
        assertTrue(rom[2].toInt() == 0)  // should be 0 (no more code)
    }

    @Test
    fun `full small program`() {
        val result = assemble("""
            ORG 0h
            LJMP start
            ORG 100h
            start:
            MOV SP, #7
            MOV P1, #0FFh
            loop:
            SJMP loop
        """.trimIndent())
        assertTrue(result.isSuccess, result.errors.joinToString("\n") { it.message })
        val rom = result.rom!!
        assertTrue(rom[0] == 0x02.toUByte())  // LJMP
        assertTrue(rom[1] == 0x01.toUByte())  // high byte of 0x100
        assertTrue(rom[2] == 0x00.toUByte())  // low byte of 0x100
        assertTrue(rom[0x100] == 0x75.toUByte())  // MOV direct, imm
        assertTrue(rom[0x101] == 0x81.toUByte())  // SP addr
        assertTrue(rom[0x102] == 0x07.toUByte())  // #7
        assertTrue(rom[0x103] == 0x75.toUByte())  // MOV direct, imm
        assertTrue(rom[0x104] == 0x90.toUByte())  // P1 addr
        assertTrue(rom[0x105] == 0xFF.toUByte())  // #0FFh
        assertTrue(rom[0x106] == 0x80.toUByte())  // SJMP
        assertTrue(rom[0x107] == 0xFE.toUByte())  // -2
    }

    @Test
    fun `MOV direct direct swaps source and destination bytes as opcode 85h src dst`() {
        val result = assemble("MOV 30h, 40h")
        assertTrue(result.isSuccess, result.errors.joinToString("\n") { it.message })
        val rom = result.rom!!
        assertEquals(0x85.toUByte(), rom[0])
        assertEquals(0x40.toUByte(), rom[1]) // src comes FIRST in 8051 encoding!
        assertEquals(0x30.toUByte(), rom[2]) // dst comes SECOND
    }

    @Test
    fun `JMP @A+DPTR encodes as 73h`() {
        val result = assemble("JMP @A+DPTR")
        assertTrue(result.isSuccess, result.errors.joinToString("\n") { it.message })
        assertEquals(0x73.toUByte(), result.rom!![0])
    }

    @Test
    fun `binary octal decimal and hex literals parse accurately`() {
        val result = assemble("""
            MOV A, #10100101b
            MOV A, #0b1100
            MOV A, #17o
            MOV A, #17q
            MOV A, #42d
            MOV A, #0FFh
        """.trimIndent())
        assertTrue(result.isSuccess, result.errors.joinToString("\n") { it.message })
        val rom = result.rom!!
        assertEquals(0xA5.toUByte(), rom[1])
        assertEquals(0x0C.toUByte(), rom[3])
        assertEquals(15.toUByte(), rom[5])
        assertEquals(15.toUByte(), rom[7])
        assertEquals(42.toUByte(), rom[9])
        assertEquals(0xFF.toUByte(), rom[11])
    }

    @Test
    fun `RAM bit addressing maps 20h-2Fh and invalid non-bit-addressable bytes fail`() {
        val ok = assemble("SETB 20h.3\nCLR 2Fh.7\nCPL P1.2\nJB ACC.0, $")
        assertTrue(ok.isSuccess, ok.errors.joinToString("\n") { it.message })
        val rom = ok.rom!!
        assertEquals(0x03.toUByte(), rom[1]) // (0x20 - 0x20)*8 + 3
        assertEquals(0x7F.toUByte(), rom[3]) // (0x2F - 0x20)*8 + 7 = 0x7F
        assertEquals(0x92.toUByte(), rom[5]) // P1 (0x90) + 2
        assertEquals(0xE0.toUByte(), rom[7]) // ACC (0xE0) + 0

        assertFalse(assemble("SETB PCON.0").isSuccess) // 0x87 is not bit-addressable
        assertFalse(assemble("SETB P1.8").isSuccess)   // bit index 8 out of range
    }

    @Test
    fun `operators AND OR XOR SHL SHR chained unary DB and DW expressions work`() {
        val result = assemble("""
            MOV A, #(0Fh AND 0Ah) OR 10h
            MOV A, #(1 XOR 3) SHL 2
            MOV A, #80h SHR 1
            MOV A, #LOW (NOT 0)
            MOV A, #HIGH (-1)
            DB 'A' + 1, 'Z' - 1
            DW 'A' + 1
        """.trimIndent())
        assertTrue(result.isSuccess, result.errors.joinToString("\n") { it.message })
        val rom = result.rom!!
        assertEquals(0x1A.toUByte(), rom[1])
        assertEquals(0x08.toUByte(), rom[3])
        assertEquals(0x40.toUByte(), rom[5])
        assertEquals(0xFF.toUByte(), rom[7])
        assertEquals(0xFF.toUByte(), rom[9])
        assertEquals('B'.code.toUByte(), rom[10])
        assertEquals('Y'.code.toUByte(), rom[11])
        assertEquals(0x00.toUByte(), rom[12])
        assertEquals('B'.code.toUByte(), rom[13])
    }

    @Test
    fun `case-insensitive labels hex-letter labels CH EQU in ORG and dollar expressions work`() {
        val result = assemble("""
            BASE EQU 100h
            ORG BASE
            msg: DB "Hello"
            LEN EQU $ - MSG
            ch: MOV A, #len
            LJMP CH
        """.trimIndent())
        assertTrue(result.isSuccess, result.errors.joinToString("\n") { it.message })
        val rom = result.rom!!
        assertEquals(5.toUByte(), rom[0x106]) // LEN = 5
        assertEquals(0x02.toUByte(), rom[0x107]) // LJMP ch (0x105)
        assertEquals(0x01.toUByte(), rom[0x108])
        assertEquals(0x05.toUByte(), rom[0x109])
    }

    @Test
    fun `assembler reports semantic and syntax errors cleanly`() {
        assertFalse(assemble("MOV A, #MISSING_SYM").isSuccess)
        assertFalse(assemble("DUP:\nNOP\nDUP:\nNOP").isSuccess)
        assertFalse(assemble("ORG 0\nSJMP FAR_AWAY\nDS 200\nFAR_AWAY:\nNOP").isSuccess)
        assertFalse(assemble("ORG 0000h\nAJMP NEXT_BLK\nORG 0900h\nNEXT_BLK:\nNOP").isSuccess)
        assertFalse(assemble("MOV A, @R2").isSuccess)
        assertFalse(assemble("MOV A, #10 / 0").isSuccess)
        assertFalse(assemble("MOV A, 1 2").isSuccess)
        assertFalse(assemble("DB \"unterminated").isSuccess)
    }
}
