package dev.dominikstahl.emu_8051.ui.components.hardware

import dev.dominikstahl.emu_8051.ui.HwComponentConfig
import dev.dominikstahl.emu_8051.ui.Port

abstract class HwComponent(val config: HwComponentConfig) {
    open fun needsTick(): Boolean = false
    open fun tick(portValues: List<Int>, totalCycles: Long = 0L) {}

    open fun onAttach(portCtrl: PortController) {}

    open fun onUserInput(input: HwUserInput, portCtrl: PortController) {}

    open fun reset() {}

    open fun onRemove(portCtrl: PortController) {}

    abstract fun snapshot(): ComponentSnapshot
}

abstract class ComponentSnapshot

object UnitSnapshot : ComponentSnapshot()

open class HwUserInput {
    data class ToggleInput(val compId: String, val port: Port, val pin: Int, val on: Boolean) : HwUserInput()
    data class ButtonInput(val compId: String, val port: Port, val pin: Int, val pressed: Boolean) : HwUserInput()
    data class DipSwitchInput(val compId: String, val port: Port, val bitMask: Int) : HwUserInput()
    data class KeyInput(val compId: String, val row: Int, val col: Int, val pressed: Boolean) : HwUserInput()
    data class SerialInput(val compId: String, val char: Char) : HwUserInput()
    data class SerialStringInput(val compId: String, val text: String) : HwUserInput()
    data class ClearUartInput(val compId: String) : HwUserInput()
    data class BuzzerMuteInput(val compId: String, val muted: Boolean) : HwUserInput()
}
