package dev.dominikstahl.emu_8051.ui.components.hardware.led

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import dev.dominikstahl.emu_8051.ui.HwComponentConfig
import dev.dominikstahl.emu_8051.ui.Port
import dev.dominikstahl.emu_8051.ui.components.hardware.ComponentSnapshot
import dev.dominikstahl.emu_8051.ui.components.hardware.HwComponent
import dev.dominikstahl.emu_8051.ui.components.hardware.HwComponentFactory
import dev.dominikstahl.emu_8051.ui.components.hardware.HwUserInput
import dev.dominikstahl.emu_8051.ui.components.hardware.UnitSnapshot
import dev.dominikstahl.emu_8051.ui.components.hardware.PinField
import dev.dominikstahl.emu_8051.ui.components.hardware.PortField

class LedComponent(config: HwComponentConfig) : HwComponent(config) {
    override fun snapshot(): ComponentSnapshot = UnitSnapshot
}

object LedFactory : HwComponentFactory {
    override val typeId = "LED"
    override fun displayName() = "LED"

    override fun createComponent(config: HwComponentConfig): HwComponent = LedComponent(config)

    override fun defaultConfig(id: String) = HwComponentConfig(id, "P1.7 LED", typeId, ports = listOf(Port.P1), pin = 7)

    override fun labelFor(config: HwComponentConfig) = "${config.port.name}.${config.pin} LED"

    @Composable
    override fun Render(
        config: HwComponentConfig,
        snapshot: ComponentSnapshot?,
        portValues: List<Int>,
        onUserInput: (HwUserInput) -> Unit,
    ) {
        val portValue = portValues.firstOrNull() ?: 0
        val on = (portValue and (1 shl config.pin)) != 0
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LedIndicator(on = on, color = Color(0xFFFF4444))
            Text(
                "${config.port.name}.${config.pin}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun ConfigFields(
        config: HwComponentConfig,
        onUpdateComponent: (String, (HwComponentConfig) -> HwComponentConfig) -> Unit,
    ) {
        PortField(config, onUpdateComponent)
        PinField(config) { newPin ->
            onUpdateComponent(config.id) { it.copy(pin = newPin) }
        }
    }
}
