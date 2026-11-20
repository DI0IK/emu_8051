package dev.dominikstahl.emu_8051.ui.components.hardware.dipswitch

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.dominikstahl.emu_8051.ui.HwComponentConfig
import dev.dominikstahl.emu_8051.ui.Port
import dev.dominikstahl.emu_8051.ui.components.hardware.ComponentSnapshot
import dev.dominikstahl.emu_8051.ui.components.hardware.HwComponent
import dev.dominikstahl.emu_8051.ui.components.hardware.HwComponentFactory
import dev.dominikstahl.emu_8051.ui.components.hardware.HwUserInput
import dev.dominikstahl.emu_8051.ui.components.hardware.PolarityField
import dev.dominikstahl.emu_8051.ui.components.hardware.PortController
import dev.dominikstahl.emu_8051.ui.components.hardware.PortField

data class DipSwitchSnapshot(val switchMask: Int) : ComponentSnapshot()

class DipSwitchComponent(config: HwComponentConfig) : HwComponent(config) {
    private var switchMask = 0
    private var portCtrl: PortController? = null

    override fun onAttach(portCtrl: PortController) {
        this.portCtrl = portCtrl
        drivePort(portCtrl)
    }

    override fun onUserInput(input: HwUserInput, portCtrl: PortController) {
        this.portCtrl = portCtrl
        if (input is HwUserInput.DipSwitchInput && input.compId == config.id) {
            switchMask = input.bitMask and 0xFF
            drivePort(portCtrl)
        }
    }

    private fun drivePort(ctrl: PortController) {
        val portVal = if (config.activeLow) (switchMask xor 0xFF) and 0xFF else switchMask
        ctrl.drive(config.port, 0xFF, portVal)
    }

    override fun reset() {
        switchMask = 0
        portCtrl?.let { drivePort(it) }
    }

    override fun onRemove(portCtrl: PortController) {
        switchMask = 0
        portCtrl.release(config.port, 0xFF)
    }

    override fun snapshot(): ComponentSnapshot = DipSwitchSnapshot(switchMask)
}

object DipSwitchFactory : HwComponentFactory {
    override val typeId = "DIP_SWITCH"
    override fun displayName() = "DIP Switch (8-bit)"

    override fun createComponent(config: HwComponentConfig): HwComponent = DipSwitchComponent(config)

    override fun defaultConfig(id: String) = HwComponentConfig(
        id = id,
        label = "P0 DIP Switch",
        type = typeId,
        ports = listOf(Port.P0),
    )

    override fun labelFor(config: HwComponentConfig) = "${config.port.name} DIP Switch"

    override fun usedPins(config: HwComponentConfig) = (0..7).map { config.port to it }

    @Composable
    override fun Render(
        config: HwComponentConfig,
        snapshot: ComponentSnapshot?,
        portValues: List<Int>,
        onUserInput: (HwUserInput) -> Unit,
    ) {
        val switchMask = (snapshot as? DipSwitchSnapshot)?.switchMask ?: 0
        val drivenVal = if (config.activeLow) (switchMask xor 0xFF) and 0xFF else switchMask

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${config.port.name} DIP = 0x${drivenVal.toString(16).padStart(2, '0').uppercase()}",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                for (bit in 7 downTo 0) {
                    val isOn = (switchMask and (1 shl bit)) != 0
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(width = 16.dp, height = 28.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                                .clickable {
                                    val nextMask = switchMask xor (1 shl bit)
                                    onUserInput(HwUserInput.DipSwitchInput(config.id, config.port, nextMask))
                                },
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 12.dp, height = 11.dp)
                                    .align(if (isOn) Alignment.TopCenter else Alignment.BottomCenter)
                                    .padding(vertical = 2.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (isOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                            )
                        }
                        Text(
                            bit.toString(),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
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
        PolarityField(config, onUpdateComponent)
    }
}
