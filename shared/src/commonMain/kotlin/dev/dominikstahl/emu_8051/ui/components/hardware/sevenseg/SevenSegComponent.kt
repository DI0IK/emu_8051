package dev.dominikstahl.emu_8051.ui.components.hardware.sevenseg

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import dev.dominikstahl.emu_8051.ui.HwComponentConfig
import dev.dominikstahl.emu_8051.ui.Port
import dev.dominikstahl.emu_8051.ui.components.hardware.ComponentSnapshot
import dev.dominikstahl.emu_8051.ui.components.hardware.HwComponent
import dev.dominikstahl.emu_8051.ui.components.hardware.HwComponentFactory
import dev.dominikstahl.emu_8051.ui.components.hardware.HwUserInput
import dev.dominikstahl.emu_8051.ui.components.hardware.UnitSnapshot
import dev.dominikstahl.emu_8051.ui.components.hardware.PortField

class SevenSegComponent(config: HwComponentConfig) : HwComponent(config) {
    override fun snapshot(): ComponentSnapshot = UnitSnapshot
}

object SevenSegFactory : HwComponentFactory {
    override val typeId = "SEVEN_SEG"
    override fun displayName() = "7-Segment"

    override fun createComponent(config: HwComponentConfig): HwComponent = SevenSegComponent(config)

    override fun defaultConfig(id: String) = HwComponentConfig(id, "P2 7-Seg", typeId, Port.P2)

    override fun labelFor(config: HwComponentConfig) = "${config.port.name} 7-Seg"

    @Composable
    override fun Render(
        config: HwComponentConfig,
        snapshot: ComponentSnapshot?,
        portValue: Int,
        onUserInput: (HwUserInput) -> Unit,
    ) {
        Column {
            Text(
                "${config.port.name} 7-Seg",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SevenSegmentDisplay(portValue, color = Color(0xFFFF4444))
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
