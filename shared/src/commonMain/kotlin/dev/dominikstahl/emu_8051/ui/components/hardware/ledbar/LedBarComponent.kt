package dev.dominikstahl.emu_8051.ui.components.hardware.ledbar

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
import dev.dominikstahl.emu_8051.ui.components.hardware.PortField

class LedBarComponent(config: HwComponentConfig) : HwComponent(config) {
    override fun snapshot(): ComponentSnapshot = UnitSnapshot
}

object LedBarFactory : HwComponentFactory {
    override val typeId = "LED_BAR"
    override fun displayName() = "LED Bar"

    override fun createComponent(config: HwComponentConfig): HwComponent = LedBarComponent(config)

    override fun defaultConfig(id: String) = HwComponentConfig(id, "P1 LED Bar", typeId, Port.P1)

    override fun labelFor(config: HwComponentConfig) = "${config.port.name} LED Bar"

    @Composable
    override fun Render(
        config: HwComponentConfig,
        snapshot: ComponentSnapshot?,
        portValue: Int,
        onUserInput: (HwUserInput) -> Unit,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                config.port.name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
            LedBar(portValue = portValue, color = Color(0xFFFF4444))
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun ConfigFields(
        config: HwComponentConfig,
        onUpdateComponent: (String, (HwComponentConfig) -> HwComponentConfig) -> Unit,
    ) {
        PortField(config, onUpdateComponent)
    }
}
