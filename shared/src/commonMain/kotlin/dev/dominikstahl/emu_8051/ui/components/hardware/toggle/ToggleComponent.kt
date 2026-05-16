package dev.dominikstahl.emu_8051.ui.components.hardware.toggle

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import dev.dominikstahl.emu_8051.ui.HwComponentConfig
import dev.dominikstahl.emu_8051.ui.Port
import dev.dominikstahl.emu_8051.ui.components.hardware.ComponentSnapshot
import dev.dominikstahl.emu_8051.ui.components.hardware.HwComponent
import dev.dominikstahl.emu_8051.ui.components.hardware.HwComponentFactory
import dev.dominikstahl.emu_8051.ui.components.hardware.HwUserInput
import dev.dominikstahl.emu_8051.ui.components.hardware.UnitSnapshot
import dev.dominikstahl.emu_8051.ui.components.hardware.PinField
import dev.dominikstahl.emu_8051.ui.components.hardware.PortField

class ToggleComponent(config: HwComponentConfig) : HwComponent(config) {
    override fun snapshot(): ComponentSnapshot = UnitSnapshot
}

object ToggleFactory : HwComponentFactory {
    override val typeId = "TOGGLE"
    override fun displayName() = "Toggle Switch"

    override fun createComponent(config: HwComponentConfig): HwComponent = ToggleComponent(config)

    override fun defaultConfig(id: String) = HwComponentConfig(id, "P3.0 Toggle", typeId, Port.P3, pin = 0)

    override fun labelFor(config: HwComponentConfig) = "${config.port.name}.${config.pin} Toggle"

    @Composable
    override fun Render(
        config: HwComponentConfig,
        snapshot: ComponentSnapshot?,
        portValue: Int,
        onUserInput: (HwUserInput) -> Unit,
    ) {
        val on = (portValue and (1 shl config.pin)) != 0
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${config.port.name}.${config.pin}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ToggleSwitch(
                on = on,
                onClick = { onUserInput(HwUserInput.ToggleInput(config.port, config.pin, !on)) },
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
