@file:OptIn(ExperimentalUnsignedTypes::class)

package dev.dominikstahl.emu_8051

import dev.dominikstahl.emu_8051.engine.CPRL2_BIT
import dev.dominikstahl.emu_8051.engine.CT0_BIT
import dev.dominikstahl.emu_8051.engine.CpuState
import dev.dominikstahl.emu_8051.engine.DCEN_BIT
import dev.dominikstahl.emu_8051.engine.EXEN2_BIT
import dev.dominikstahl.emu_8051.engine.EXF2_BIT
import dev.dominikstahl.emu_8051.engine.GATE0_BIT
import dev.dominikstahl.emu_8051.engine.RCLK_BIT
import dev.dominikstahl.emu_8051.engine.TF0_BIT
import dev.dominikstahl.emu_8051.engine.TF1_BIT
import dev.dominikstahl.emu_8051.engine.TF2_BIT
import dev.dominikstahl.emu_8051.engine.TR0_BIT
import dev.dominikstahl.emu_8051.engine.TR1_BIT
import dev.dominikstahl.emu_8051.engine.TR2_BIT
import dev.dominikstahl.emu_8051.engine.TimerController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TimerControllerTest {

    private fun drivePin(state: CpuState, port: Int, mask: Int, high: Boolean) {
        state.externalDriven[port] = state.externalDriven[port] or mask
        state.externalValue[port] = if (high) {
            state.externalValue[port] or mask
        } else {
            state.externalValue[port] and mask.inv()
        }
    }

    @Test
    fun `Timer 0 Mode 0 wraps 5-bit TL0 at 0x1F and masks upper bits`() {
        val state = CpuState()
        val tc = TimerController(state)
        state.TMOD = 0x00u // T0 Mode 0
        state.TL0 = 0xFFu // Mode 0 treats TL0 as 5-bit (0x1F)
        state.TH0 = 0xFFu
        state.TCON = TR0_BIT.toUByte()

        tc.tick()
        assertEquals(0x00u, state.TL0)
        assertEquals(0x00u, state.TH0)
        assertTrue((state.TCON.toInt() and TF0_BIT) != 0)
    }

    @Test
    fun `Timer 0 Modes 1 and 2 count 16-bit and auto-reload accurately`() {
        val state = CpuState()
        val tc = TimerController(state)

        // Mode 1: 16-bit overflow
        state.TMOD = 0x01u
        state.TH0 = 0xFFu
        state.TL0 = 0xFFu
        state.TCON = TR0_BIT.toUByte()
        tc.tick()
        assertEquals(0x00u, state.TL0)
        assertEquals(0x00u, state.TH0)
        assertTrue((state.TCON.toInt() and TF0_BIT) != 0)

        // Mode 2: 8-bit auto-reload
        state.reset()
        tc.reset()
        state.TMOD = 0x02u
        state.TH0 = 0xABu
        state.TL0 = 0xFFu
        state.TCON = TR0_BIT.toUByte()
        tc.tick()
        assertEquals(0xABu, state.TL0)
        assertTrue((state.TCON.toInt() and TF0_BIT) != 0)
    }

    @Test
    fun `Timer 0 Mode 3 controls TH0 via TR1 while Timer 1 runs Mode 2 ungated with TF1 suppressed`() {
        val state = CpuState()
        val tc = TimerController(state)
        // T0 Mode 3 (0x03), T1 Mode 2 (0x20) -> TMOD = 0x23
        state.TMOD = 0x23u
        state.TL0 = 0xFEu
        state.TH0 = 0xFFu
        state.TH1 = 0x42u
        state.TL1 = 0xFFu
        // TR0=1, TR1=0 -> TL0 ticks, TH0 stopped, Timer 1 STILL ticks (baud gen)!
        state.TCON = TR0_BIT.toUByte()
        tc.tick()
        assertEquals(0xFFu, state.TL0)
        assertEquals(0xFFu, state.TH0)
        assertEquals(0x42u, state.TL1) // reloaded from TH1 even with TR1=0!
        assertEquals(0, state.TCON.toInt() and TF1_BIT) // TF1 belongs to TH0 now

        // Now set TR1=1 -> TH0 ticks and sets TF1 on overflow
        state.TCON = TR1_BIT.toUByte()
        tc.tick()
        assertEquals(0x00u, state.TH0)
        assertTrue((state.TCON.toInt() and TF1_BIT) != 0)
    }

    @Test
    fun `GATE0 only counts while INT0 pin is HIGH`() {
        val state = CpuState()
        val tc = TimerController(state)
        state.TMOD = (GATE0_BIT or 0x01).toUByte()
        state.TCON = TR0_BIT.toUByte()
        state.TL0 = 0x10u

        drivePin(state, 3, 0x04, high = false)
        tc.tick()
        assertEquals(0x10u, state.TL0)

        drivePin(state, 3, 0x04, high = true)
        tc.tick()
        assertEquals(0x11u, state.TL0)
    }

    @Test
    fun `CT0 counter mode counts falling edges only and has no spurious overflow when TL0 is zero`() {
        val state = CpuState()
        val tc = TimerController(state)
        state.TMOD = (CT0_BIT or 0x02).toUByte() // Counter Mode 2
        state.TH0 = 0x55u
        state.TL0 = 0x00u
        state.TCON = TR0_BIT.toUByte()

        // Steady HIGH -> must NOT trigger spurious auto-reload or TF0 even when TL0 == 0!
        drivePin(state, 3, 0x10, high = true)
        tc.tick()
        assertEquals(0x00u, state.TL0)
        assertEquals(0, state.TCON.toInt() and TF0_BIT)

        // Falling edge HIGH -> LOW -> counts once (TL0 becomes 1)
        drivePin(state, 3, 0x10, high = false)
        tc.tick()
        assertEquals(0x01u, state.TL0)

        // Steady LOW -> no count
        tc.tick()
        assertEquals(0x01u, state.TL0)

        // Rising edge -> no count
        drivePin(state, 3, 0x10, high = true)
        tc.tick()
        assertEquals(0x01u, state.TL0)

        // Second falling edge -> counts once
        drivePin(state, 3, 0x10, high = false)
        tc.tick()
        assertEquals(0x02u, state.TL0)
    }

    @Test
    fun `Timer 2 supports auto-reload post-tick capture DCEN up-down and EXEN2 in baud mode`() {
        val state = CpuState()
        val tc = TimerController(state)

        // 1. Auto-reload mode (CPRL2 = 0)
        state.RCAP2H = 0x12u
        state.RCAP2L = 0x34u
        state.TH2 = 0xFFu
        state.TL2 = 0xFFu
        state.T2CON = TR2_BIT.toUByte()
        tc.tick()
        assertEquals(0x34u, state.TL2)
        assertEquals(0x12u, state.TH2)
        assertTrue((state.T2CON.toInt() and TF2_BIT) != 0)

        // 2. Capture mode (CPRL2 = 1, EXEN2 = 1): captures POST-tick value when T2EX falls
        state.reset()
        tc.reset()
        drivePin(state, 1, 0x02, high = true) // T2EX starts high
        state.TH2 = 0x00u
        state.TL2 = 0x10u
        state.T2CON = (TR2_BIT or CPRL2_BIT or EXEN2_BIT).toUByte()
        tc.tick() // pre-sync prevT2EXHigh=true, TL2 becomes 0x11

        drivePin(state, 1, 0x02, high = false)
        tc.tick() // TL2 increments to 0x12 BEFORE capture -> RCAP2L must capture 0x12
        assertEquals(0x12u, state.RCAP2L)
        assertEquals(0x00u, state.RCAP2H)
        assertTrue((state.T2CON.toInt() and EXF2_BIT) != 0)

        // 3. DCEN down-counting (T2EX = 0 -> counts down; matching RCAP2 reloads 0xFFFF, sets TF2, toggles EXF2)
        state.reset()
        tc.reset()
        state.T2MOD = DCEN_BIT.toUByte()
        state.RCAP2H = 0x10u
        state.RCAP2L = 0x00u
        state.TH2 = 0x10u
        state.TL2 = 0x01u
        drivePin(state, 1, 0x02, high = false) // T2EX = 0 -> count down
        state.T2CON = TR2_BIT.toUByte()
        tc.tick() // decrements to 0x1000
        assertEquals(0x00u, state.TL2)
        assertEquals(0x10u, state.TH2)
        assertEquals(0, state.T2CON.toInt() and TF2_BIT)

        tc.tick() // matches RCAP2 -> reloads 0xFFFF, sets TF2, toggles EXF2 (0->1)
        assertEquals(0xFFu, state.TL2)
        assertEquals(0xFFu, state.TH2)
        assertTrue((state.T2CON.toInt() and TF2_BIT) != 0)
        assertTrue((state.T2CON.toInt() and EXF2_BIT) != 0)

        // 4. EXEN2 sets EXF2 in baud-rate generator mode (RCLK = 1) without altering TL2/TH2
        state.reset()
        tc.reset()
        drivePin(state, 1, 0x02, high = true)
        state.TH2 = 0xFEu
        state.TL2 = 0x50u
        state.T2CON = (TR2_BIT or RCLK_BIT or EXEN2_BIT).toUByte()
        tc.tick()
        drivePin(state, 1, 0x02, high = false)
        tc.tick()
        assertEquals(0x52u, state.TL2)
        assertTrue((state.T2CON.toInt() and EXF2_BIT) != 0)
    }
}
