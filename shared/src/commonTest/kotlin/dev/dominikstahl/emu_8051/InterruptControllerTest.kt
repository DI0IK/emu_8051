@file:OptIn(ExperimentalUnsignedTypes::class)

package dev.dominikstahl.emu_8051

import dev.dominikstahl.emu_8051.engine.CpuState
import dev.dominikstahl.emu_8051.engine.EA_BIT
import dev.dominikstahl.emu_8051.engine.ET0_BIT
import dev.dominikstahl.emu_8051.engine.EX0_BIT
import dev.dominikstahl.emu_8051.engine.EX1_BIT
import dev.dominikstahl.emu_8051.engine.IE0_BIT
import dev.dominikstahl.emu_8051.engine.IE1_BIT
import dev.dominikstahl.emu_8051.engine.IT0_BIT
import dev.dominikstahl.emu_8051.engine.IT1_BIT
import dev.dominikstahl.emu_8051.engine.Interpreter
import dev.dominikstahl.emu_8051.engine.PT0_BIT
import dev.dominikstahl.emu_8051.engine.PX1_BIT
import dev.dominikstahl.emu_8051.engine.TF0_BIT
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InterruptControllerTest {

    private fun drivePin(state: CpuState, port: Int, mask: Int, high: Boolean) {
        state.externalDriven[port] = state.externalDriven[port] or mask
        state.externalValue[port] = if (high) {
            state.externalValue[port] or mask
        } else {
            state.externalValue[port] and mask.inv()
        }
    }

    @Test
    fun `edge-triggered INT0 latches on falling edge and IT1 level mode does not clobber IE0`() {
        val state = CpuState()
        val ic = InterruptControllerHelper(state)

        // IT0 = 1 (edge triggered INT0), IT1 = 0 (level triggered INT1)
        state.TCON = IT0_BIT.toUByte()
        drivePin(state, 3, 0x04, high = true)
        drivePin(state, 3, 0x08, high = true)
        ic.sample()
        assertEquals(0, state.TCON.toInt() and IE0_BIT)

        // Falling edge on INT0 sets IE0
        drivePin(state, 3, 0x04, high = false)
        ic.sample()
        assertTrue((state.TCON.toInt() and IE0_BIT) != 0)
        assertEquals(0, state.TCON.toInt() and IE1_BIT)

        // Polling edge-triggered INT0 auto-clears IE0 and vectors to 0x0003 (2 interrupt cycles)
        state.IE = (EA_BIT or EX0_BIT).toUByte()
        assertEquals(2, ic.poll())
        assertEquals(0x0003, state.pc)
        assertEquals(0, state.TCON.toInt() and IE0_BIT)
    }

    @Test
    fun `level-triggered INT0 clears IE0 when pin returns HIGH without affecting other flags`() {
        val state = CpuState()
        val ic = InterruptControllerHelper(state)
        state.TCON = IE1_BIT.toUByte() // IT0=0, IT1=0, pre-existing IE1 bit
        drivePin(state, 3, 0x04, high = false)
        drivePin(state, 3, 0x08, high = false)
        ic.sample()
        assertTrue((state.TCON.toInt() and IE0_BIT) != 0)
        assertTrue((state.TCON.toInt() and IE1_BIT) != 0)

        drivePin(state, 3, 0x04, high = true)
        ic.sample()
        assertEquals(0, state.TCON.toInt() and IE0_BIT)
        assertTrue((state.TCON.toInt() and IE1_BIT) != 0)
    }

    @Test
    fun `high priority interrupt wins over earlier low priority vector and preemption rules hold`() {
        val state = CpuState()
        val cpu = Interpreter(state)
        val ic = cpu.interruptController

        // Both TF0 (vector 0x000B) and IE1 (vector 0x0013) pending; IE1 has HIGH priority
        state.IE = (EA_BIT or ET0_BIT or EX1_BIT).toUByte()
        state.IP = PX1_BIT.toUByte()
        state.TCON = (TF0_BIT or IE1_BIT or IT1_BIT).toUByte()

        // First poll must dispatch IE1 (0x0013), NOT TF0 (0x000B)
        assertEquals(2, ic.poll())
        assertEquals(0x0013, state.pc)

        // While HIGH-priority ISR is active, neither LOW-priority TF0 nor same-priority INT0 can preempt
        state.IE = (EA_BIT or ET0_BIT or EX0_BIT or EX1_BIT).toUByte()
        state.TCON = (state.TCON.toInt() or TF0_BIT or IE0_BIT).toUByte()
        state.IP = (PX1_BIT or 0x01).toUByte() // PX0 also high
        assertEquals(0, ic.poll())

        // Clear high-priority in-service (RETI), test low-priority preemption blocking
        ic.clearInService()
        state.IP = 0u
        state.TCON = TF0_BIT.toUByte()
        assertEquals(2, ic.poll())
        assertEquals(0x000B, state.pc) // TF0 low-priority enters

        // Same-priority (low) IE1 must be blocked while TF0 low-priority is active
        state.TCON = IE1_BIT.toUByte()
        assertEquals(0, ic.poll())

        // High-priority IE1 CAN preempt active low-priority TF0
        state.IP = PX1_BIT.toUByte()
        assertEquals(2, ic.poll())
        assertEquals(0x0013, state.pc)
        ic.clearInService() // exits high ISR -> still inside low ISR!
        state.TCON = IE0_BIT.toUByte()
        state.IP = 0u
        assertEquals(0, ic.poll()) // low still blocked until second RETI!
        ic.clearInService()
        assertEquals(2, ic.poll())
        assertEquals(0x0003, state.pc)
    }

    @Test
    fun `write or read to IE or IP delays interrupt dispatch for one instruction`() {
        val state = CpuState()
        val cpu = Interpreter(state)
        // Put TF0 pending before enabling EA+ET0 via MOV IE, #82h followed by NOP, NOP
        state.TCON = TF0_BIT.toUByte()
        state.rom[0x0000] = 0x75u // MOV IE, #82h
        state.rom[0x0001] = 0xA8u
        state.rom[0x0002] = (EA_BIT or ET0_BIT).toUByte()
        state.rom[0x0003] = 0x00u // NOP
        state.rom[0x0004] = 0x00u // NOP
        state.rom[0x000B] = 0x32u // RETI

        cpu.step() // executes MOV IE -> interruptControllerAccessFlag was set -> must execute 0x0003 NOP next!
        assertEquals(0x0003, state.pc)

        cpu.step() // executes NOP at 0x0003 -> THEN dispatches TF0 after NOP? Actually Interpreter polls before fetching or after?
        // In Interpreter: step() checks pending interrupt after access-inhibit instruction completes, on next step or immediately:
        assertEquals(0x000B, state.pc)
    }

    private fun InterruptControllerHelper(state: CpuState) =
        dev.dominikstahl.emu_8051.engine.InterruptController(state)
}
