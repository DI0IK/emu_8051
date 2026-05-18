package dev.dominikstahl.emu_8051.ui.components.hardware.hd44780

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.dominikstahl.emu_8051.ui.HwComponentConfig
import dev.dominikstahl.emu_8051.ui.Port
import dev.dominikstahl.emu_8051.ui.components.hardware.ComponentSnapshot
import dev.dominikstahl.emu_8051.ui.components.hardware.HwComponent
import dev.dominikstahl.emu_8051.ui.components.hardware.HwComponentFactory
import dev.dominikstahl.emu_8051.ui.components.hardware.HwUserInput
import dev.dominikstahl.emu_8051.ui.components.hardware.PortField

class Hd44780Component(config: HwComponentConfig) : HwComponent(config) {
    override fun needsTick() = true

    val ctrl = Hd44780Controller(config.lcdCols, config.lcdLines)

    override fun tick(portValues: List<Int>) = ctrl.tick(portValues.firstOrNull() ?: 0)

    override fun reset() { ctrl.reset() }

    override fun snapshot(): ComponentSnapshot = LcdSnapshot(
        lines = ctrl.getDisplayLines(),
        rawBytes = ctrl.getRawLines(),
        cgram = ctrl.getCgramCopy().toList().map { it.toInt() and 0xFF },
    )
}

data class LcdSnapshot(
    val lines: List<String>,
    val rawBytes: List<List<Int>>,
    val cgram: List<Int>,
) : ComponentSnapshot()

object Hd44780Factory : HwComponentFactory {
    override val typeId = "HD44780_LCD"
    override fun displayName() = "HD44780 LCD"

    override fun createComponent(config: HwComponentConfig): HwComponent = Hd44780Component(config)

    override fun defaultConfig(id: String) = HwComponentConfig(id, "P2 LCD (16x2)", typeId, ports = listOf(Port.P2), lcdCols = 16, lcdLines = 2)

    override fun labelFor(config: HwComponentConfig) = "${config.port.name} LCD (${config.lcdCols}x${config.lcdLines})"

    override fun needsTick(config: HwComponentConfig) = true

    @Composable
    override fun Render(
        config: HwComponentConfig,
        snapshot: ComponentSnapshot?,
        portValues: List<Int>,
        onUserInput: (HwUserInput) -> Unit,
    ) {
        val s = snapshot as? LcdSnapshot
        Text(
            "${config.port.name} LCD (${config.lcdCols}x${config.lcdLines})",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Hd44780Lcd(
            lines = s?.lines ?: emptyList(),
            cols = config.lcdCols,
            rawBytes = s?.rawBytes ?: emptyList(),
            cgramBytes = s?.cgram ?: emptyList(),
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun ConfigFields(
        config: HwComponentConfig,
        onUpdateComponent: (String, (HwComponentConfig) -> HwComponentConfig) -> Unit,
    ) {
        PortField(config, onUpdateComponent)

        var colsExpanded by remember { mutableStateOf(false) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Cols", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(40.dp))
            ExposedDropdownMenuBox(
                expanded = colsExpanded,
                onExpandedChange = { colsExpanded = it },
                modifier = Modifier.widthIn(min = 70.dp),
            ) {
                OutlinedTextField(
                    value = config.lcdCols.toString(),
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = colsExpanded) },
                    modifier = Modifier.menuAnchor(),
                    textStyle = MaterialTheme.typography.bodySmall,
                )
                ExposedDropdownMenu(expanded = colsExpanded, onDismissRequest = { colsExpanded = false }) {
                    listOf(8, 16, 20, 40).forEach { v ->
                        DropdownMenuItem(text = { Text(v.toString()) }, onClick = {
                            onUpdateComponent(config.id) { it.copy(lcdCols = v) }
                            colsExpanded = false
                        })
                    }
                }
            }
        }
        var linesExpanded by remember { mutableStateOf(false) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Lines", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(40.dp))
            ExposedDropdownMenuBox(
                expanded = linesExpanded,
                onExpandedChange = { linesExpanded = it },
                modifier = Modifier.widthIn(min = 70.dp),
            ) {
                OutlinedTextField(
                    value = config.lcdLines.toString(),
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = linesExpanded) },
                    modifier = Modifier.menuAnchor(),
                    textStyle = MaterialTheme.typography.bodySmall,
                )
                ExposedDropdownMenu(expanded = linesExpanded, onDismissRequest = { linesExpanded = false }) {
                    listOf(1, 2, 4).forEach { v ->
                        DropdownMenuItem(text = { Text(v.toString()) }, onClick = {
                            onUpdateComponent(config.id) { it.copy(lcdLines = v) }
                            linesExpanded = false
                        })
                    }
                }
            }
        }
    }
}
