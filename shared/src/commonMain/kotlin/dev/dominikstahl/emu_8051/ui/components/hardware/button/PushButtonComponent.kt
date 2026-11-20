package dev.dominikstahl.emu_8051.ui.components.hardware.button

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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

data class PushButtonSnapshot(val pressed: Boolean) : ComponentSnapshot()

class PushButtonComponent(config: HwComponentConfig) : HwComponent(config) {
    private var pressed = false
    private var portCtrl: PortController? = null

    override fun onAttach(portCtrl: PortController) {
        this.portCtrl = portCtrl
        applyPin(portCtrl)
    }

    override fun onUserInput(input: HwUserInput, portCtrl: PortController) {
        this.portCtrl = portCtrl
        if (input is HwUserInput.ButtonInput && input.compId == config.id) {
            pressed = input.pressed
            applyPin(portCtrl)
        }
    }

    private fun applyPin(ctrl: PortController) {
        val mask = 1 shl config.pin
        if (config.activeLow) {
            if (pressed) ctrl.drive(config.port, mask, 0)
            else ctrl.release(config.port, mask)
        } else {
            val valBit = if (pressed) mask else 0
            ctrl.drive(config.port, mask, valBit)
        }
    }

    override fun reset() {
        pressed = false
        portCtrl?.let { applyPin(it) }
    }

    override fun onRemove(portCtrl: PortController) {
        pressed = false
        portCtrl.release(config.port, 1 shl config.pin)
    }

    override fun snapshot(): ComponentSnapshot = PushButtonSnapshot(pressed)
}

object PushButtonFactory : HwComponentFactory {
    override val typeId = "PUSH_BUTTON"
    override fun displayName() = "Push Button"

    override fun createComponent(config: HwComponentConfig): HwComponent = PushButtonComponent(config)

    override fun defaultConfig(id: String) = HwComponentConfig(
        id = id,
        label = "P3.2 Button",
        type = typeId,
        ports = listOf(Port.P3),
        pin = 2,
        activeLow = true,
    )

    override fun labelFor(config: HwComponentConfig) = "${config.port.name}.${config.pin} Button"

    override fun usedPins(config: HwComponentConfig) = listOf(config.port to config.pin)

    @Composable
    override fun Render(
        config: HwComponentConfig,
        snapshot: ComponentSnapshot?,
        portValues: List<Int>,
        onUserInput: (HwUserInput) -> Unit,
    ) {
        val pressed = (snapshot as? PushButtonSnapshot)?.pressed ?: false
        val currentOnUserInput by rememberUpdatedState(onUserInput)

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${config.port.name}.${config.pin}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        1.dp,
                        if (pressed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        RoundedCornerShape(6.dp),
                    )
                    .pointerInput(config.id, config.port, config.pin) {
                        detectTapGestures(
                            onPress = {
                                currentOnUserInput(HwUserInput.ButtonInput(config.id, config.port, config.pin, true))
                                try {
                                    tryAwaitRelease()
                                } finally {
                                    currentOnUserInput(HwUserInput.ButtonInput(config.id, config.port, config.pin, false))
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(if (pressed) 16.dp else 18.dp)
                        .clip(CircleShape)
                        .background(
                            if (pressed) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline
                        )
                )
            }
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
