package dev.dominikstahl.emu_8051

import kotlin.test.Test
import kotlin.test.assertEquals
import dev.dominikstahl.emu_8051.engine.CpuState

class SharedCommonTest {

    @Test
    fun example() {
        assertEquals(3, 1 + 2)
    }

    @Test
    fun at89s52PinResolutionHonorsOpenDrainAndExternalLow() {
        val cpu = CpuState()
        cpu.reset()
        cpu.P0 = 0xFFu
        assertEquals(0, cpu.getEffectivePort(0))
        cpu.P1 = 0xFFu
        assertEquals(0xFF, cpu.getEffectivePort(1))

        cpu.externalDriven[1] = 1
        cpu.externalValue[1] = 0
        assertEquals(0xFE, cpu.getEffectivePort(1))

        cpu.P1 = 0xFFu
        cpu.externalValue[1] = 1
        assertEquals(1, cpu.getEffectivePort(1) and 1)

        cpu.P1 = 0xFEu
        assertEquals(0, cpu.getEffectivePort(1))
    }
}