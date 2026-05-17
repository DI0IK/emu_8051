package dev.dominikstahl.emu_8051.ui.components.hardware

import dev.dominikstahl.emu_8051.ui.HwComponentConfig
import dev.dominikstahl.emu_8051.ui.Port

abstract class HwComponent(val config: HwComponentConfig) {
    open fun needsTick(): Boolean = false
    open fun tick(portVal: Int) {}

    open fun onUserInput(input: HwUserInput, portCtrl: PortController) {}

    open fun reset() {}

    abstract fun snapshot(): ComponentSnapshot
}

abstract class ComponentSnapshot

object UnitSnapshot : ComponentSnapshot()

open class HwUserInput {
    data class ToggleInput(val port: Port, val pin: Int, val on: Boolean) : HwUserInput()
    data class KeyInput(val compId: String, val row: Int, val col: Int, val pressed: Boolean) : HwUserInput()
    data class SerialInput(val compId: String, val char: Char) : HwUserInput()
}
