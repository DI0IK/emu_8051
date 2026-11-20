@file:OptIn(ExperimentalUnsignedTypes::class)

package dev.dominikstahl.emu_8051

import dev.dominikstahl.emu_8051.asm.assemble
import dev.dominikstahl.emu_8051.engine.CpuState
import dev.dominikstahl.emu_8051.engine.Interpreter
import dev.dominikstahl.emu_8051.engine.REN_BIT
import dev.dominikstahl.emu_8051.engine.RI_BIT
import dev.dominikstahl.emu_8051.engine.TI_BIT
import dev.dominikstahl.emu_8051.examples.BuiltinExamples
import dev.dominikstahl.emu_8051.ui.Port
import dev.dominikstahl.emu_8051.ui.components.hardware.HwComponent
import dev.dominikstahl.emu_8051.ui.components.hardware.HwRegistry
import dev.dominikstahl.emu_8051.ui.components.hardware.HwUserInput
import dev.dominikstahl.emu_8051.ui.components.hardware.PortController
import dev.dominikstahl.emu_8051.ui.components.hardware.buzzer.BuzzerSnapshot
import dev.dominikstahl.emu_8051.ui.components.hardware.hd44780.LcdSnapshot
import dev.dominikstahl.emu_8051.ui.components.hardware.ledmatrix.LedMatrixSnapshot
import dev.dominikstahl.emu_8051.ui.components.hardware.registerBuiltinHardwareComponents
import dev.dominikstahl.emu_8051.ui.components.hardware.servo.ServoSnapshot
import dev.dominikstahl.emu_8051.ui.components.hardware.sevenseg.MultiSevenSegSnapshot
import dev.dominikstahl.emu_8051.ui.components.hardware.stepper.StepperSnapshot
import dev.dominikstahl.emu_8051.ui.components.hardware.uart.UartSnapshot
import dev.dominikstahl.emu_8051.ui.components.hardware.uart.UartTerminalComponent
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ExamplesValidationTest {

    private class CpuPortController(private val state: CpuState) : PortController {
        override fun drive(port: Port, mask: Int, value: Int) {
            val idx = port.ordinal
            state.externalDriven[idx] = state.externalDriven[idx] or mask
            state.externalValue[idx] = (state.externalValue[idx] and mask.inv()) or (value and mask)
        }

        override fun release(port: Port, mask: Int) {
            val idx = port.ordinal
            state.externalDriven[idx] = state.externalDriven[idx] and mask.inv()
        }
    }

    private fun runExample(
        exampleId: String,
        maxSteps: Int,
        afterInit: ((CpuState, List<HwComponent>, PortController) -> Unit)? = null,
    ): Pair<CpuState, List<HwComponent>> {
        registerBuiltinHardwareComponents()
        val example = BuiltinExamples.all.first { it.id == exampleId }
        val assembled = assemble(example.sourceCode)
        assertTrue(assembled.isSuccess, "$exampleId assembly failed: ${assembled.errors}")

        val state = CpuState()
        val rom = assembled.rom!!
        rom.copyInto(state.rom)
        val cpu = Interpreter(state)
        val portCtrl = CpuPortController(state)
        val components = example.buildHwConfig().map { cfg ->
            HwRegistry.get(cfg.type)!!.createComponent(cfg).also { it.onAttach(portCtrl) }
        }

        fun tickComponents() {
            for (comp in components) {
                if (comp.needsTick()) {
                    val values = comp.config.ports.map { state.getEffectivePort(it.ordinal) }
                    comp.tick(values, state.totalCycles)
                }
            }
        }

        var injected = false
        for (stepIdx in 0 until maxSteps) {
            cpu.step()
            tickComponents()
            if (!injected && stepIdx == maxSteps / 3 && afterInit != null) {
                injected = true
                afterInit(state, components, portCtrl)
            }
        }
        return state to components
    }

    @Test
    fun `all 14 example files on disk and BuiltinExamples assemble with zero errors`() {
        val examplesDir = File("../examples").takeIf { it.isDirectory } ?: File("examples")
        assertTrue(examplesDir.isDirectory, "Could not locate examples/ directory from ${File("").absolutePath}")

        val asmFiles = examplesDir.listFiles { f -> f.isDirectory }
            ?.sortedBy { it.name }
            ?.mapNotNull { dir -> dir.listFiles { f -> f.extension == "asm" }?.firstOrNull() }
            ?: emptyList()

        assertEquals(14, asmFiles.size, "Expected 14 example .asm files")
        for (file in asmFiles) {
            val result = assemble(file.readText())
            assertTrue(
                result.isSuccess && result.errors.isEmpty(),
                "Example ${file.name} failed assembly: ${result.errors}",
            )
            assertTrue(result.sourceMap.isNotEmpty(), "Expected non-empty sourceMap for ${file.name}")
        }

        assertEquals(14, BuiltinExamples.all.size)
        for (ex in BuiltinExamples.all) {
            val res = assemble(ex.sourceCode)
            assertTrue(res.isSuccess && res.errors.isEmpty(), "Builtin ${ex.id} failed: ${res.errors}")
            assertTrue(ex.buildHwConfig().isNotEmpty(), "Builtin ${ex.id} should define default hardware")
        }
    }

    @Test
    fun `LCD example prints HELLO on line 1`() {
        val (_, comps) = runExample("06_lcd", maxSteps = 75_000)
        val snap = comps.first().snapshot() as LcdSnapshot
        assertEquals("HELLO           ", snap.lines[0])
    }

    @Test
    fun `UART example transmits HELLO and echoes injected RX character`() {
        registerBuiltinHardwareComponents()
        val example = BuiltinExamples.all.first { it.id == "07_uart" }
        val assembled = assemble(example.sourceCode)
        assertTrue(assembled.isSuccess)

        val state = CpuState()
        assembled.rom!!.copyInto(state.rom)
        val cpu = Interpreter(state)
        val uart = UartTerminalComponent(example.buildHwConfig().first())
        var txTimer = 0
        state.onSbufTx = { byte ->
            uart.appendTx(byte)
            txTimer = 5
        }

        var rxQueued = false
        for (stepIdx in 0 until 2_500) {
            cpu.step()
            if (txTimer > 0 && --txTimer == 0) {
                state.SCON = (state.SCON.toInt() or TI_BIT).toUByte()
            }
            if (!rxQueued && stepIdx == 1_500) {
                rxQueued = true
                uart.enqueueRx('Z')
            }
            val rxByte = uart.dequeueRxIfReady(state.SCON.toInt())
            if (rxByte != null) {
                state.receiveSerialByte(rxByte)
                state.SCON = (state.SCON.toInt() or RI_BIT).toUByte()
            }
        }

        val tx = (uart.snapshot() as UartSnapshot).txBuffer
        assertEquals("HELLO\r\nZ", tx)
    }

    @Test
    fun `Stepper Buzzer Servo and LedMatrix examples produce valid outputs`() {
        val (_, stepperComps) = runExample("08_stepper", maxSteps = 8_000)
        val stepperSnap = stepperComps.first().snapshot() as StepperSnapshot
        assertTrue(stepperSnap.positionDegrees > 20f, "Expected stepper rotation, got ${stepperSnap.positionDegrees}")

        val (_, buzzerComps) = runExample("09_buzzer", maxSteps = 20_000)
        val buzzerSnap = buzzerComps.first().snapshot() as BuzzerSnapshot
        assertTrue(buzzerSnap.on, "Expected buzzer to be active")
        assertTrue(buzzerSnap.frequencyHz in 200..4000, "Unexpected frequency ${buzzerSnap.frequencyHz}")

        val (_, servoComps) = runExample("10_servo", maxSteps = 30_000)
        val servoSnap = servoComps.first().snapshot() as ServoSnapshot
        assertTrue(servoSnap.angle in 0..5, "Expected ~0 deg servo angle on frame 1, got ${servoSnap.angle}")

        val (_, matrixComps) = runExample("11_ledmatrix", maxSteps = 20_000)
        val matrixSnap = matrixComps.first().snapshot() as LedMatrixSnapshot
        val litCount = matrixSnap.brightness.flatten().count { it > 0.1f }
        assertTrue(litCount > 5, "Expected lit pixels in LED matrix smile pattern, got $litCount")
    }

    @Test
    fun `Button DipSwitch and MultiSevenSeg examples behave as documented`() {
        val (btnState, _) = runExample("12_button", maxSteps = 500) { _, comps, ctrl ->
            val btn = comps.first { it.config.type == "PUSH_BUTTON" }
            btn.onUserInput(HwUserInput.ButtonInput(btn.config.id, Port.P3, 2, true), ctrl)
        }
        // Pressing active-low INT0 button toggles P1.7 from 0 to 1 (bit 7 = 0x80)
        assertTrue((btnState.P1.toInt() and 0x80) != 0, "Expected INT0 ISR to toggle P1.7 HIGH")

        val (dipState, _) = runExample("13_dipswitch", maxSteps = 500) { _, comps, ctrl ->
            val dip = comps.first { it.config.type == "DIP_SWITCH" }
            dip.onUserInput(HwUserInput.DipSwitchInput(dip.config.id, Port.P0, 0x3C), ctrl)
        }
        assertEquals(0x3Cu, dipState.P1, "Expected P0 DIP switch mask mirrored to P1 LED bar")

        val (_, m7Comps) = runExample("14_multisevenseg", maxSteps = 6_000)
        val m7Snap = m7Comps.first().snapshot() as MultiSevenSegSnapshot
        // Timer 0 ISR refreshes digits with '0' pattern (bits 0..5) across multiplexed digits
        assertTrue(m7Snap.digits.any { d -> d[0] > 0.1f }, "Expected active multiplexed 7-segment digits")
    }
}
