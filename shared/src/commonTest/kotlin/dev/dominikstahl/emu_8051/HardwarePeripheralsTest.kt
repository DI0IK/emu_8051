@file:OptIn(ExperimentalUnsignedTypes::class)

package dev.dominikstahl.emu_8051

import dev.dominikstahl.emu_8051.engine.REN_BIT
import dev.dominikstahl.emu_8051.engine.RI_BIT
import dev.dominikstahl.emu_8051.platform.FileFormat
import dev.dominikstahl.emu_8051.ui.HwComponentConfig
import dev.dominikstahl.emu_8051.ui.Port
import dev.dominikstahl.emu_8051.ui.components.hardware.HwRegistry
import dev.dominikstahl.emu_8051.ui.components.hardware.HwUserInput
import dev.dominikstahl.emu_8051.ui.components.hardware.PortController
import dev.dominikstahl.emu_8051.ui.components.hardware.button.PushButtonComponent
import dev.dominikstahl.emu_8051.ui.components.hardware.button.PushButtonSnapshot
import dev.dominikstahl.emu_8051.ui.components.hardware.buzzer.BuzzerComponent
import dev.dominikstahl.emu_8051.ui.components.hardware.buzzer.BuzzerFactory
import dev.dominikstahl.emu_8051.ui.components.hardware.buzzer.BuzzerSnapshot
import dev.dominikstahl.emu_8051.ui.components.hardware.dipswitch.DipSwitchComponent
import dev.dominikstahl.emu_8051.ui.components.hardware.dipswitch.DipSwitchSnapshot
import dev.dominikstahl.emu_8051.ui.components.hardware.hd44780.Hd44780Controller
import dev.dominikstahl.emu_8051.ui.components.hardware.keypad.KeypadSnapshot
import dev.dominikstahl.emu_8051.ui.components.hardware.keypad.MatrixKeypadComponent
import dev.dominikstahl.emu_8051.ui.components.hardware.ledmatrix.LedMatrixComponent
import dev.dominikstahl.emu_8051.ui.components.hardware.ledmatrix.LedMatrixSnapshot
import dev.dominikstahl.emu_8051.ui.components.hardware.registerBuiltinHardwareComponents
import dev.dominikstahl.emu_8051.ui.components.hardware.servo.ServoComponent
import dev.dominikstahl.emu_8051.ui.components.hardware.servo.ServoFactory
import dev.dominikstahl.emu_8051.ui.components.hardware.servo.ServoSnapshot
import dev.dominikstahl.emu_8051.ui.components.hardware.sevenseg.MultiSevenSegComponent
import dev.dominikstahl.emu_8051.ui.components.hardware.sevenseg.MultiSevenSegSnapshot
import dev.dominikstahl.emu_8051.ui.components.hardware.stepper.StepperComponent
import dev.dominikstahl.emu_8051.ui.components.hardware.stepper.StepperFactory
import dev.dominikstahl.emu_8051.ui.components.hardware.stepper.StepperSnapshot
import dev.dominikstahl.emu_8051.ui.components.hardware.toggle.ToggleComponent
import dev.dominikstahl.emu_8051.ui.components.hardware.toggle.ToggleSnapshot
import dev.dominikstahl.emu_8051.ui.components.hardware.uart.UartSnapshot
import dev.dominikstahl.emu_8051.ui.components.hardware.uart.UartTerminalComponent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HardwarePeripheralsTest {

    private class FakePortController : PortController {
        val driven = IntArray(4)
        val values = IntArray(4)

        override fun drive(port: Port, mask: Int, value: Int) {
            val idx = port.ordinal
            driven[idx] = driven[idx] or (mask and 0xFF)
            values[idx] = (values[idx] and mask.inv()) or (value and mask)
            values[idx] = values[idx] and 0xFF
        }

        override fun release(port: Port, mask: Int) {
            val idx = port.ordinal
            driven[idx] = driven[idx] and mask.inv()
            values[idx] = values[idx] and driven[idx]
        }

        fun effective(port: Port, latch: Int = 0xFF): Int {
            val idx = port.ordinal
            return (latch and (driven[idx].inv() or values[idx])) and 0xFF
        }
    }

    private fun pulseLcdNibble(lcd: Hd44780Controller, nibble: Int, rs: Boolean) {
        val rsBit = if (rs) 0x01 else 0x00
        val high = ((nibble and 0x0F) shl 4) or 0x04 or rsBit
        val low = ((nibble and 0x0F) shl 4) or rsBit
        lcd.tick(high)
        lcd.tick(low)
    }

    private fun sendLcdByte(lcd: Hd44780Controller, byte: Int, rs: Boolean) {
        pulseLcdNibble(lcd, (byte ushr 4) and 0x0F, rs)
        pulseLcdNibble(lcd, byte and 0x0F, rs)
    }

    @Test
    fun `Hd44780Controller initializes 4-bit mode writes 20x4 Line 4 and supports shift home clear`() {
        val lcd = Hd44780Controller(cols = 20, lines = 4)
        // Standard 4-bit init sequence (single nibble writes before 4-bit mode locks)
        pulseLcdNibble(lcd, 0x03, rs = false)
        pulseLcdNibble(lcd, 0x03, rs = false)
        pulseLcdNibble(lcd, 0x03, rs = false)
        pulseLcdNibble(lcd, 0x02, rs = false)

        sendLcdByte(lcd, 0x28, rs = false) // 4-bit, 2+ lines
        sendLcdByte(lcd, 0x0F, rs = false) // display ON, cursor ON, blink ON
        assertTrue(lcd.displayOn)
        assertTrue(lcd.cursorOn)
        assertTrue(lcd.blinkOn)

        // Line 1 & Line 4 (DDRAM 0x54 -> command 0xD4)
        sendLcdByte(lcd, 0x80, rs = false)
        "TOP".forEach { sendLcdByte(lcd, it.code, rs = true) }
        sendLcdByte(lcd, 0xD4, rs = false)
        "L4".forEach { sendLcdByte(lcd, it.code, rs = true) }

        val lines = lcd.getDisplayLines()
        assertTrue(lines[0].startsWith("TOP"))
        assertTrue(lines[3].startsWith("L4"))
        assertEquals(3 to 2, lcd.cursorPosition())

        // Shift left then home resets shift & cursor
        sendLcdByte(lcd, 0x18, rs = false)
        assertEquals(1, lcd.displayShiftOffset)
        sendLcdByte(lcd, 0x02, rs = false)
        assertEquals(0, lcd.displayShiftOffset)
        assertEquals(0 to 0, lcd.cursorPosition())

        // Clear wipes DDRAM
        sendLcdByte(lcd, 0x01, rs = false)
        assertEquals("                    ", lcd.getDisplayLines()[0])
    }

    @Test
    fun `BuzzerComponent detects toggle frequency handles mute times out and resets`() {
        val cfg = HwComponentConfig("b1", "Buzzer", "BUZZER", ports = listOf(Port.P1), pin = 0)
        assertTrue(BuzzerFactory.needsTick(cfg))
        val buzzer = BuzzerComponent(cfg)

        // Initial low (inactive for active-high)
        buzzer.tick(listOf(0x00), totalCycles = 100L)
        assertFalse((buzzer.snapshot() as BuzzerSnapshot).on)

        // Edges 500 cycles apart -> 500_000 / 500 = 1000 Hz
        buzzer.tick(listOf(0x01), totalCycles = 600L)
        buzzer.tick(listOf(0x00), totalCycles = 1100L)
        val snap = buzzer.snapshot() as BuzzerSnapshot
        assertTrue(snap.on)
        assertEquals(1000, snap.frequencyHz)

        // Mute toggle updates snapshot without turning tone flag off
        buzzer.onUserInput(HwUserInput.BuzzerMuteInput("b1", true), FakePortController())
        assertTrue((buzzer.snapshot() as BuzzerSnapshot).muted)

        // Idle timeout (> 50_000 cycles inactive) stops buzzer
        buzzer.tick(listOf(0x00), totalCycles = 60_000L)
        val stopped = buzzer.snapshot() as BuzzerSnapshot
        assertFalse(stopped.on)
        assertEquals(0, stopped.frequencyHz)

        buzzer.reset()
        assertFalse((buzzer.snapshot() as BuzzerSnapshot).on)
    }

    @Test
    fun `ServoComponent ignores initial 0xFF and decodes 1000 1500 2000 us PWM pulses`() {
        val cfg = HwComponentConfig("s1", "Servo", "SERVO", ports = listOf(Port.P1), pin = 0)
        assertTrue(ServoFactory.needsTick(cfg))
        val servo = ServoComponent(cfg)

        // Initial 0xFF must not glitch angle
        servo.tick(listOf(0xFF), totalCycles = 0L)
        assertEquals(90, (servo.snapshot() as ServoSnapshot).angle)

        // Drop low, then pulse 1000 us -> 0 deg
        servo.tick(listOf(0x00), totalCycles = 100L)
        servo.tick(listOf(0x01), totalCycles = 200L)
        servo.tick(listOf(0x00), totalCycles = 1200L)
        assertEquals(0, (servo.snapshot() as ServoSnapshot).angle)

        // Pulse 2000 us -> 180 deg
        servo.tick(listOf(0x01), totalCycles = 2000L)
        servo.tick(listOf(0x00), totalCycles = 4000L)
        assertEquals(180, (servo.snapshot() as ServoSnapshot).angle)

        // Pulse 1500 us -> 90 deg
        servo.tick(listOf(0x01), totalCycles = 5000L)
        servo.tick(listOf(0x00), totalCycles = 6500L)
        assertEquals(90, (servo.snapshot() as ServoSnapshot).angle)
    }

    @Test
    fun `StepperComponent tracks half-steps CW CCW coilBits and resets cleanly`() {
        val cfg = HwComponentConfig("st1", "Stepper", "STEPPER", ports = listOf(Port.P1))
        assertTrue(StepperFactory.needsTick(cfg))
        val stepper = StepperComponent(cfg)

        stepper.tick(listOf(0b0001), 10L)
        stepper.tick(listOf(0b0011), 20L)
        stepper.tick(listOf(0b0010), 30L)
        val cw = stepper.snapshot() as StepperSnapshot
        assertEquals(11.25f, cw.positionDegrees)
        assertEquals(0b0010, cw.coilBits)

        stepper.tick(listOf(0b0011), 40L)
        stepper.tick(listOf(0b0001), 50L)
        assertEquals(0f, (stepper.snapshot() as StepperSnapshot).positionDegrees)

        stepper.reset()
        val resetSnap = stepper.snapshot() as StepperSnapshot
        assertEquals(0f, resetSnap.positionDegrees)
        assertEquals(0, resetSnap.coilBits)
    }

    @Test
    fun `LedMatrixComponent handles cycle persistence activeLow and reset`() {
        val cfg = HwComponentConfig("m1", "Matrix", "LED_MATRIX", ports = listOf(Port.P1, Port.P2))
        val matrix = LedMatrixComponent(cfg)
        matrix.tick(listOf(0x01, 0x02), totalCycles = 1000L)
        assertEquals(1f, (matrix.snapshot() as LedMatrixSnapshot).brightness[0][1])

        // 20,000 cycles later with no LED lit -> decay = 20_000 / 40_000 = 0.5
        matrix.tick(listOf(0x00, 0x00), totalCycles = 21_000L)
        assertEquals(0.5f, (matrix.snapshot() as LedMatrixSnapshot).brightness[0][1])

        // Active-low polarity
        val lowCfg = cfg.copy(activeLow = true)
        val lowMatrix = LedMatrixComponent(lowCfg)
        lowMatrix.tick(listOf(0xFE, 0xFD), totalCycles = 100L)
        assertEquals(1f, (lowMatrix.snapshot() as LedMatrixSnapshot).brightness[0][1])

        lowMatrix.reset()
        assertEquals(0f, (lowMatrix.snapshot() as LedMatrixSnapshot).brightness[0][1])
    }

    @Test
    fun `MatrixKeypadComponent scans 4x4 and 8x8 keypads and releases pins on reset and remove`() {
        val ctrl4 = FakePortController()
        val cfg4 = HwComponentConfig("k4", "Keypad 4x4", "KEYPAD", ports = listOf(Port.P1), rows = 4, cols = 4)
        val kp4 = MatrixKeypadComponent(cfg4)
        kp4.onUserInput(HwUserInput.KeyInput("k4", row = 1, col = 2, pressed = true), ctrl4)
        // Row 1 driven LOW (bit 1 = 0 -> 0xFD), Col 2 should be pulled LOW (bit 6 = 0)
        kp4.tick(listOf(0xFD), totalCycles = 10L)
        assertEquals(0, ctrl4.effective(Port.P1) and (1 shl 6))
        assertEquals(setOf((1 shl 4) or 2), (kp4.snapshot() as KeypadSnapshot).pressedKeys)

        kp4.reset()
        assertEquals(0, ctrl4.driven[Port.P1.ordinal])
        assertTrue((kp4.snapshot() as KeypadSnapshot).pressedKeys.isEmpty())

        // 8x8 two-port keypad (Rows on P1, Cols on P2)
        val ctrl8 = FakePortController()
        val cfg8 = HwComponentConfig("k8", "Keypad 8x8", "KEYPAD", ports = listOf(Port.P1, Port.P2), rows = 8, cols = 8)
        val kp8 = MatrixKeypadComponent(cfg8)
        kp8.onUserInput(HwUserInput.KeyInput("k8", row = 6, col = 7, pressed = true), ctrl8)
        kp8.tick(listOf(0xFF xor (1 shl 6), 0xFF), totalCycles = 10L)
        assertEquals(0, ctrl8.effective(Port.P2) and (1 shl 7))

        kp8.onRemove(ctrl8)
        assertEquals(0, ctrl8.driven[Port.P2.ordinal])
    }

    @Test
    fun `UartTerminalComponent queues RX gated by REN and RI and resets cleanly`() {
        val uart = UartTerminalComponent(HwComponentConfig("u1", "UART", "UART_TERMINAL", ports = listOf(Port.P3)))
        uart.appendTx('H'.code.toUByte())
        uart.appendTx('i'.code.toUByte())
        uart.enqueueRx('A')
        uart.enqueueRxString("BC")

        val snap = uart.snapshot() as UartSnapshot
        assertEquals("Hi", snap.txBuffer)
        assertEquals(3, snap.rxPendingCount)

        // REN=0 -> cannot dequeue
        assertNull(uart.dequeueRxIfReady(0))
        // REN=1, RI=1 -> busy, cannot overwrite SBUF
        assertNull(uart.dequeueRxIfReady(REN_BIT or RI_BIT))
        // REN=1, RI=0 -> delivers 'A'
        assertEquals('A'.code.toUByte(), uart.dequeueRxIfReady(REN_BIT))

        uart.clearTx()
        assertEquals("", (uart.snapshot() as UartSnapshot).txBuffer)

        uart.reset()
        val cleared = uart.snapshot() as UartSnapshot
        assertEquals("", cleared.txBuffer)
        assertEquals(0, cleared.rxPendingCount)
    }

    @Test
    fun `Toggle PushButton DipSwitch and MultiSevenSeg behave and release pins properly`() {
        val ctrl = FakePortController()

        // Toggle (active high)
        val toggle = ToggleComponent(HwComponentConfig("t1", "Tog", "TOGGLE", ports = listOf(Port.P3), pin = 0))
        toggle.onAttach(ctrl)
        assertEquals(0, ctrl.effective(Port.P3) and 0x01)
        toggle.onUserInput(HwUserInput.ToggleInput("t1", Port.P3, 0, true), ctrl)
        assertTrue((toggle.snapshot() as ToggleSnapshot).on)
        assertEquals(0x01, ctrl.effective(Port.P3) and 0x01)
        toggle.onRemove(ctrl)
        assertEquals(0, ctrl.driven[Port.P3.ordinal])

        // PushButton (active LOW)
        val btn = PushButtonComponent(HwComponentConfig("pb1", "Btn", "PUSH_BUTTON", ports = listOf(Port.P3), pin = 2, activeLow = true))
        btn.onAttach(ctrl)
        assertEquals(0, ctrl.driven[Port.P3.ordinal] and 0x04) // unpressed = released (high via pull-up)
        btn.onUserInput(HwUserInput.ButtonInput("pb1", Port.P3, 2, true), ctrl)
        assertTrue((btn.snapshot() as PushButtonSnapshot).pressed)
        assertEquals(0, ctrl.effective(Port.P3) and 0x04)
        btn.reset()
        assertFalse((btn.snapshot() as PushButtonSnapshot).pressed)
        assertEquals(0, ctrl.driven[Port.P3.ordinal] and 0x04)

        // DipSwitch (active low)
        val dip = DipSwitchComponent(HwComponentConfig("d1", "DIP", "DIP_SWITCH", ports = listOf(Port.P0), activeLow = true))
        dip.onAttach(ctrl)
        assertEquals(0xFF, ctrl.effective(Port.P0))
        dip.onUserInput(HwUserInput.DipSwitchInput("d1", Port.P0, 0xA5), ctrl)
        assertEquals(0xA5, (dip.snapshot() as DipSwitchSnapshot).switchMask)
        assertEquals(0x5A, ctrl.effective(Port.P0))
        dip.onRemove(ctrl)
        assertEquals(0, ctrl.driven[Port.P0.ordinal])

        // MultiSevenSeg multiplexing & persistence
        val m7 = MultiSevenSegComponent(HwComponentConfig("ms", "4-Dig", "MULTI_SEVEN_SEG", ports = listOf(Port.P2, Port.P0), cols = 4))
        m7.tick(listOf(0x3F, 0x01), totalCycles = 100L) // '0' (a..f = bits 0..5) on digit 0
        m7.tick(listOf(0x06, 0x02), totalCycles = 200L) // '1' (b,c = bits 1,2) on digit 1
        val mSnap = m7.snapshot() as MultiSevenSegSnapshot
        assertEquals(4, mSnap.digits.size)
        assertTrue(mSnap.digits[0][0] > 0.9f)
        assertEquals(0f, mSnap.digits[0][6])
        assertTrue(mSnap.digits[1][1] > 0.9f)
    }

    @Test
    fun `FileFormat round-trips hardware configs and built-in registry contains all components`() {
        registerBuiltinHardwareComponents()
        for (typeId in listOf(
            "LED", "LED_BAR", "SEVEN_SEG", "MULTI_SEVEN_SEG", "TOGGLE", "PUSH_BUTTON",
            "DIP_SWITCH", "MATRIX_KEYPAD", "HD44780_LCD", "UART_TERMINAL", "STEPPER", "BUZZER",
            "SERVO", "LED_MATRIX",
        )) {
            assertNotNull(HwRegistry.get(typeId), "Missing factory for $typeId")
        }

        val configs = listOf(
            HwComponentConfig("c1", "Btn", "PUSH_BUTTON", ports = listOf(Port.P3), pin = 2, activeLow = true),
            HwComponentConfig("c2", "4-Dig", "MULTI_SEVEN_SEG", ports = listOf(Port.P2, Port.P0), cols = 4),
        )
        val encoded = FileFormat.encode("NOP\nSJMP $", configs)
        val (decodedSrc, decodedCfgs) = FileFormat.decode(encoded)
        assertEquals("NOP\nSJMP $", decodedSrc)
        assertEquals(configs, decodedCfgs)
    }
}
