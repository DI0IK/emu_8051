package dev.dominikstahl.emu_8051.ui.components.hardware.uart

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.dominikstahl.emu_8051.ui.HwComponentConfig
import dev.dominikstahl.emu_8051.ui.Port
import dev.dominikstahl.emu_8051.ui.components.hardware.ComponentSnapshot
import dev.dominikstahl.emu_8051.ui.components.hardware.HwComponent
import dev.dominikstahl.emu_8051.ui.components.hardware.HwComponentFactory
import dev.dominikstahl.emu_8051.ui.components.hardware.HwUserInput

data class UartSnapshot(val txBuffer: String) : ComponentSnapshot()

class UartTerminalComponent(config: HwComponentConfig) : HwComponent(config) {
    private val txBuffer = StringBuilder()
    private val maxChars = 4096

    fun appendTx(byte: UByte) {
        txBuffer.append(byte.toInt().toChar())
        if (txBuffer.length > maxChars) {
            txBuffer.delete(0, txBuffer.length - maxChars)
        }
    }

    override fun snapshot(): ComponentSnapshot {
        return UartSnapshot(txBuffer.toString())
    }
}

object UartFactory : HwComponentFactory {
    override val typeId = "UART_TERMINAL"
    override fun displayName() = "UART Terminal"

    override fun createComponent(config: HwComponentConfig): HwComponent = UartTerminalComponent(config)

    override fun defaultConfig(id: String) = HwComponentConfig(id, "UART Terminal", typeId, Port.P1)

    override fun labelFor(config: HwComponentConfig) = "UART Terminal"

    @Composable
    override fun Render(
        config: HwComponentConfig,
        snapshot: ComponentSnapshot?,
        portValue: Int,
        onUserInput: (HwUserInput) -> Unit,
    ) {
        val txText = (snapshot as? UartSnapshot)?.txBuffer ?: ""
        UartTerminalView(
            txText = txText,
            onSendChar = { onUserInput(HwUserInput.SerialInput(config.id, it)) },
            modifier = Modifier.widthIn(min = 120.dp, max = 320.dp).height(180.dp),
        )
    }

    @Composable
    override fun ConfigFields(
        config: HwComponentConfig,
        onUpdateComponent: (String, (HwComponentConfig) -> HwComponentConfig) -> Unit,
    ) {
    }
}
