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
import dev.dominikstahl.emu_8051.ui.components.hardware.PinField
import dev.dominikstahl.emu_8051.ui.components.hardware.PolarityField
import dev.dominikstahl.emu_8051.ui.components.hardware.PortController
import dev.dominikstahl.emu_8051.ui.components.hardware.PortField

data class ToggleSnapshot(val on: Boolean) : ComponentSnapshot()

class ToggleComponent(config: HwComponentConfig) : HwComponent(config) {
    private var isOn = false
    private var portCtrl: PortController? = null

    override fun onAttach(portCtrl: PortController) {
        this.portCtrl = portCtrl
        applyPin(portCtrl)
    }

    override fun onUserInput(input: HwUserInput, portCtrl: PortController) {
        this.portCtrl = portCtrl
        if (input is HwUserInput.ToggleInput && input.compId == config.id) {
            isOn = input.on
            applyPin(portCtrl)
        }
    }

    private fun applyPin(ctrl: PortController) {
        val mask = 1 shl config.pin
        val effectiveHigh = if (config.activeLow) !isOn else isOn
        ctrl.drive(config.port, mask, if (effectiveHigh) mask else 0)
    }

    override fun reset() {
        isOn = false
        portCtrl?.let { applyPin(it) }
    }

    override fun onRemove(portCtrl: PortController) {
        isOn = false
        portCtrl.release(config.port, 1 shl config.pin)
    }

    override fun snapshot(): ComponentSnapshot = ToggleSnapshot(isOn)
}

object ToggleFactory : HwComponentFactory {
    override val typeId = "TOGGLE"
    override fun displayName() = "Toggle Switch"

    override fun createComponent(config: HwComponentConfig): HwComponent = ToggleComponent(config)

    override fun defaultConfig(id: String) = HwComponentConfig(id, "P3.0 Toggle", typeId, ports = listOf(Port.P3), pin = 0)

    override fun labelFor(config: HwComponentConfig) = "${config.port.name}.${config.pin} Toggle"

    override fun usedPins(config: HwComponentConfig) = listOf(config.port to config.pin)

    @Composable
    override fun Render(
        config: HwComponentConfig,
        snapshot: ComponentSnapshot?,
        portValues: List<Int>,
        onUserInput: (HwUserInput) -> Unit,
    ) {
        val on = (snapshot as? ToggleSnapshot)?.on ?: false
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${config.port.name}.${config.pin}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ToggleSwitch(
                on = on,
                onClick = { onUserInput(HwUserInput.ToggleInput(config.id, config.port, config.pin, !on)) },
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
        PolarityField(config, onUpdateComponent)
    }
}
