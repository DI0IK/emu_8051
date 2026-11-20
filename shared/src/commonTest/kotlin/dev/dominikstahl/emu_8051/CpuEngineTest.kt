@file:OptIn(ExperimentalUnsignedTypes::class)

package dev.dominikstahl.emu_8051

import dev.dominikstahl.emu_8051.engine.AC_BIT
import dev.dominikstahl.emu_8051.engine.CY_BIT
import dev.dominikstahl.emu_8051.engine.CpuState
import dev.dominikstahl.emu_8051.engine.Interpreter
import dev.dominikstahl.emu_8051.engine.OV_BIT
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CpuEngineTest {

    private fun loadProgram(state: CpuState, vararg bytes: Int, startAddr: Int = 0) {
        bytes.forEachIndexed { idx, b ->
            state.rom[(startAddr + idx) and 0xFFFF] = b.toUByte()
        }
    }

    @Test
    fun `CpuState initializes SFR defaults and clears memories on reset`() {
        val state = CpuState()
        assertEquals(0x07u, state.SP)
        assertEquals(0xFFu, state.P0)
        assertEquals(0xFFu, state.P1)
        assertEquals(0xFFu, state.P2)
        assertEquals(0xFFu, state.P3)
        assertEquals(0, state.pc)

        state.ram[0x30] = 0xAAu
        state.xram[0x1234] = 0x55u
        state.P1 = 0x12u
        state.SP = 0x40u
        state.pc = 0x1234
        state.totalCycles = 999L
        state.receiveSerialByte(0x7Eu)

        state.reset()

        assertEquals(0u, state.ram[0x30])
        assertEquals(0u, state.xram[0x1234])
        assertEquals(0xFFu, state.P1)
        assertEquals(0x07u, state.SP)
        assertEquals(0, state.pc)
        assertEquals(0L, state.totalCycles)
        assertEquals(0u, state.sbufRx)
        assertEquals(0u, state.sbufTx)
    }

    @Test
    fun `PSW parity bit is read-only and follows ACC bit count`() {
        val state = CpuState()
        val cpu = Interpreter(state)
        // MOV PSW, #01h (try forcing P=1 while A=0) ; MOV A, #03h ; MOV A, #07h
        loadProgram(
            state,
            0x75, 0xD0, 0x01,
            0x74, 0x03,
            0x74, 0x07,
        )

        cpu.step() // MOV PSW, #01h
        assertEquals(0, state.PSW.toInt() and 0x01) // P must remain 0 when ACC=0

        cpu.step() // MOV A, #03h (two 1 bits -> even -> P=0)
        assertEquals(0, state.PSW.toInt() and 0x01)

        cpu.step() // MOV A, #07h (three 1 bits -> odd -> P=1)
        assertEquals(1, state.PSW.toInt() and 0x01)
    }

    @Test
    fun `ADD ADDC SUBB DA A MUL and DIV set ACC and flags accurately`() {
        val state = CpuState()
        val cpu = Interpreter(state)

        // ADD 0x7F + 0x01 -> 0x80 (OV=1, AC=1, CY=0)
        state.ACC = 0x7Fu
        loadProgram(state, 0x24, 0x01)
        cpu.step()
        assertEquals(0x80u, state.ACC)
        assertTrue((state.PSW.toInt() and OV_BIT) != 0)
        assertTrue((state.PSW.toInt() and AC_BIT) != 0)
        assertEquals(0, state.PSW.toInt() and CY_BIT)

        // ADDC with carry set: 0xFF + 0x00 + C(1) -> 0x00, CY=1, AC=1, OV=0
        state.reset()
        state.ACC = 0xFFu
        state.PSW = CY_BIT.toUByte()
        loadProgram(state, 0x34, 0x00)
        cpu.step()
        assertEquals(0x00u, state.ACC)
        assertTrue((state.PSW.toInt() and CY_BIT) != 0)
        assertTrue((state.PSW.toInt() and AC_BIT) != 0)
        assertEquals(0, state.PSW.toInt() and OV_BIT)

        // SUBB without borrow: 0x10 - 0x20 -> 0xF0, CY=1, AC=0, OV=0
        state.reset()
        state.ACC = 0x10u
        loadProgram(state, 0x94, 0x20)
        cpu.step()
        assertEquals(0xF0u, state.ACC)
        assertTrue((state.PSW.toInt() and CY_BIT) != 0)

        // BCD DA A after ADD 0x19 + 0x28 = 0x41 (AC=1) -> DA A -> 0x47
        state.reset()
        state.ACC = 0x19u
        loadProgram(state, 0x24, 0x28, 0xD4)
        cpu.step()
        cpu.step()
        assertEquals(0x47u, state.ACC)

        // MUL AB: 20 * 20 = 400 = 0x0190 -> B=0x01, A=0x90, OV=1, CY=0
        state.reset()
        state.ACC = 20u
        state.B = 20u
        loadProgram(state, 0xA4)
        cpu.step()
        assertEquals(0x90u, state.ACC)
        assertEquals(0x01u, state.B)
        assertTrue((state.PSW.toInt() and OV_BIT) != 0)
        assertEquals(0, state.PSW.toInt() and CY_BIT)

        // DIV AB: 25 / 4 -> A=6, B=1, OV=0; then DIV by 0 -> OV=1
        state.reset()
        state.ACC = 25u
        state.B = 4u
        loadProgram(state, 0x84, 0x75, 0xF0, 0x00, 0x84)
        cpu.step()
        assertEquals(6u, state.ACC)
        assertEquals(1u, state.B)
        assertEquals(0, state.PSW.toInt() and OV_BIT)
        cpu.step()
        cpu.step()
        assertTrue((state.PSW.toInt() and OV_BIT) != 0)
    }

    @Test
    fun `MOVX @Ri uses P2 as high address byte and marks xramDirty`() {
        val state = CpuState()
        val cpu = Interpreter(state)
        state.xramDirty = false
        state.P2 = 0x34u
        state.ram[0] = 0x56u // R0
        state.ACC = 0xABu

        // MOVX @R0, A ; MOV A, #00h ; MOVX A, @R0
        loadProgram(state, 0xF2, 0x74, 0x00, 0xE2)
        cpu.step()
        assertEquals(0xABu, state.xram[0x3456])
        assertTrue(state.xramDirty)

        cpu.step()
        assertEquals(0x00u, state.ACC)
        cpu.step()
        assertEquals(0xABu, state.ACC)
    }

    @Test
    fun `MOVC reads from code ROM at A plus DPTR`() {
        val state = CpuState()
        val cpu = Interpreter(state)
        state.DPTR = 0x0200
        state.ACC = 0x05u
        state.rom[0x0205] = 0xC3u
        loadProgram(state, 0x93) // MOVC A, @A+DPTR
        cpu.step()
        assertEquals(0xC3u, state.ACC)
    }

    @Test
    fun `PUSH SP pushes incremented SP and POP works below 08h with wraparound`() {
        val state = CpuState()
        val cpu = Interpreter(state)
        state.SP = 0x07u
        state.ram[0x07] = 0x5Au

        // PUSH SP (0xC0 0x81) -> SP becomes 0x08, ram[0x08] gets 0x08
        // POP ACC (0xD0 0xE0) -> reads ram[0x08]=0x08, SP=0x07
        // POP ACC (0xD0 0xE0) -> reads ram[0x07]=0x5A, SP=0x06 (allowed!)
        loadProgram(state, 0xC0, 0x81, 0xD0, 0xE0, 0xD0, 0xE0)
        cpu.step()
        assertEquals(0x08u, state.SP)
        assertEquals(0x08u, state.ram[0x08])

        cpu.step()
        assertEquals(0x08u, state.ACC)
        assertEquals(0x07u, state.SP)

        cpu.step()
        assertEquals(0x5Au, state.ACC)
        assertEquals(0x06u, state.SP)

        // Wraparound SP = 0x00 -> POP ACC reads ram[0x00], SP becomes 0xFF
        state.ram[0x00] = 0x77u
        state.SP = 0x00u
        loadProgram(state, 0xD0, 0xE0, startAddr = state.pc)
        cpu.step()
        assertEquals(0x77u, state.ACC)
        assertEquals(0xFFu, state.SP)
    }

    @Test
    fun `JBC tests and clears port latch rather than external pin`() {
        val state = CpuState()
        val cpu = Interpreter(state)
        // P1.0 latch = 1, but external hardware drives P1.0 LOW -> pin reads 0, latch is 1
        state.P1 = 0xFFu
        state.externalDriven[1] = 0x01
        state.externalValue[1] = 0x00
        assertEquals(0xFE, state.getEffectivePort(1))

        // JBC P1.0 (bit 0x90), +4 -> should jump because latch bit 0 is 1, and clear P1.0 latch!
        loadProgram(state, 0x10, 0x90, 0x04)
        cpu.step()
        assertEquals(3 + 4, state.pc)
        assertEquals(0xFEu, state.P1)
    }

    @Test
    fun `LJMP and LCALL wrap ROM reads cleanly at FFFEh`() {
        val state = CpuState()
        val cpu = Interpreter(state)
        state.pc = 0xFFFE
        state.rom[0xFFFE] = 0x02u // LJMP
        state.rom[0xFFFF] = 0x12u
        state.rom[0x0000] = 0x34u
        cpu.step()
        assertEquals(0x1234, state.pc)

        state.reset()
        state.pc = 0xFFFE
        state.rom[0xFFFE] = 0x12u // LCALL
        state.rom[0xFFFF] = 0x20u
        state.rom[0x0000] = 0x00u
        cpu.step()
        assertEquals(0x2000, state.pc)
        // Return PC is (0xFFFE + 3) and 0xFFFF = 0x0001
        assertEquals(0x01u, state.ram[state.SP.toInt() - 1])
        assertEquals(0x00u, state.ram[state.SP.toInt()])
    }

    @Test
    fun `SBUF TX and RX registers are physically separate`() {
        val state = CpuState()
        val cpu = Interpreter(state)
        val transmitted = mutableListOf<UByte>()
        state.onSbufTx = { transmitted += it }

        state.receiveSerialByte(0x41u) // 'A' arrives in RX buffer
        state.ACC = 0x5Au
        // MOV SBUF, A ; MOV A, SBUF
        loadProgram(state, 0xF5, 0x99, 0xE5, 0x99)

        cpu.step()
        assertEquals(listOf<UByte>(0x5Au), transmitted)
        assertEquals(0x5Au, state.sbufTx)
        assertEquals(0x41u, state.sbufRx)

        cpu.step()
        assertEquals(0x41u, state.ACC) // reading SBUF returns RX buffer, not TX!
        assertNotEquals(state.sbufTx, state.ACC)
    }
}
