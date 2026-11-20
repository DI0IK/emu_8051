package dev.dominikstahl.emu_8051.ui.components.hardware.uart

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.dominikstahl.emu_8051.engine.REN_BIT
import dev.dominikstahl.emu_8051.engine.RI_BIT
import dev.dominikstahl.emu_8051.ui.HwComponentConfig
import dev.dominikstahl.emu_8051.ui.Port
import dev.dominikstahl.emu_8051.ui.components.hardware.ComponentSnapshot
import dev.dominikstahl.emu_8051.ui.components.hardware.HwComponent
import dev.dominikstahl.emu_8051.ui.components.hardware.HwComponentFactory
import dev.dominikstahl.emu_8051.ui.components.hardware.HwUserInput

data class UartSnapshot(val txBuffer: String, val rxPendingCount: Int = 0) : ComponentSnapshot()

class UartTerminalComponent(config: HwComponentConfig) : HwComponent(config) {
    private var txBuffer = StringBuilder()
    private val rxQueue = ArrayDeque<UByte>()
    private val maxChars = 4096

    fun appendTx(byte: UByte) {
        txBuffer.append(byte.toInt().toChar())
        if (txBuffer.length > maxChars) {
            txBuffer = StringBuilder(txBuffer.substring(txBuffer.length - maxChars))
        }
    }

    fun clearTx() {
        txBuffer.setLength(0)
    }

    fun enqueueRx(char: Char) {
        rxQueue.addLast((char.code and 0xFF).toUByte())
    }

    fun enqueueRxString(text: String) {
        for (ch in text) {
            rxQueue.addLast((ch.code and 0xFF).toUByte())
        }
    }

    fun dequeueRxIfReady(scon: Int): UByte? {
        if (rxQueue.isEmpty()) return null
        if ((scon and REN_BIT) == 0 || (scon and RI_BIT) != 0) return null
        return rxQueue.removeFirst()
    }

    override fun reset() {
        txBuffer.setLength(0)
        rxQueue.clear()
    }

    override fun snapshot(): ComponentSnapshot {
        return UartSnapshot(txBuffer.toString(), rxQueue.size)
    }
}

object UartFactory : HwComponentFactory {
    override val typeId = "UART_TERMINAL"
    override fun displayName() = "UART Terminal"

    override fun createComponent(config: HwComponentConfig): HwComponent = UartTerminalComponent(config)

    override fun defaultConfig(id: String) = HwComponentConfig(id, "UART Terminal (P3)", typeId, ports = listOf(Port.P3))

    override fun labelFor(config: HwComponentConfig) = "UART Terminal"

    override fun usedPins(config: HwComponentConfig) = listOf(Port.P3 to 0, Port.P3 to 1)

    @Composable
    override fun Render(
        config: HwComponentConfig,
        snapshot: ComponentSnapshot?,
        portValues: List<Int>,
        onUserInput: (HwUserInput) -> Unit,
    ) {
        val snap = snapshot as? UartSnapshot
        UartTerminalView(
            txText = snap?.txBuffer ?: "",
            rxPendingCount = snap?.rxPendingCount ?: 0,
            onSendText = { onUserInput(HwUserInput.SerialStringInput(config.id, it)) },
            onClearTx = { onUserInput(HwUserInput.ClearUartInput(config.id)) },
            modifier = Modifier.widthIn(min = 180.dp, max = 320.dp).height(190.dp),
        )
    }

    @Composable
    override fun ConfigFields(
        config: HwComponentConfig,
        onUpdateComponent: (String, (HwComponentConfig) -> HwComponentConfig) -> Unit,
    ) {
    }
}
