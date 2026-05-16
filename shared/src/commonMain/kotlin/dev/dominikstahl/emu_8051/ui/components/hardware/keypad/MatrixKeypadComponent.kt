package dev.dominikstahl.emu_8051.ui.components.hardware.keypad

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
import dev.dominikstahl.emu_8051.ui.components.hardware.PortController
import dev.dominikstahl.emu_8051.ui.components.hardware.PortField

class MatrixKeypadComponent(config: HwComponentConfig) : HwComponent(config) {
    private var pressedKeys: Set<String> = emptySet()
    private val columnRefCount = mutableMapOf<Int, Int>()

    fun setKeyState(row: Int, col: Int, pressed: Boolean) {
        val key = "$row,$col"
        pressedKeys = if (pressed) pressedKeys + key else pressedKeys - key
    }

    override fun onUserInput(input: HwUserInput, portCtrl: PortController) {
        if (input is HwUserInput.KeyInput && input.compId == config.id) {
            val col = input.col
            val count = columnRefCount.getOrDefault(col, 0)
            if (input.pressed) {
                columnRefCount[col] = count + 1
                if (count == 0) {
                    portCtrl.drive(config.port, 1 shl col, 0)
                }
            } else {
                val newCount = count - 1
                if (newCount <= 0) {
                    columnRefCount.remove(col)
                    portCtrl.release(config.port, 1 shl col)
                } else {
                    columnRefCount[col] = newCount
                }
            }
            setKeyState(input.row, input.col, input.pressed)
        }
    }

    override fun reset() {
        pressedKeys = emptySet()
        columnRefCount.clear()
    }

    override fun snapshot(): ComponentSnapshot = KeypadSnapshot(pressedKeys)
}

data class KeypadSnapshot(val pressedKeys: Set<String>) : ComponentSnapshot()

object MatrixKeypadFactory : HwComponentFactory {
    override val typeId = "MATRIX_KEYPAD"
    override fun displayName() = "Matrix Keypad"

    override fun createComponent(config: HwComponentConfig): HwComponent = MatrixKeypadComponent(config)

    override fun defaultConfig(id: String) = HwComponentConfig(id, "P1 Keypad (4x4)", typeId, Port.P1, rows = 4, cols = 4)

    override fun labelFor(config: HwComponentConfig) = "${config.port.name} Keypad (${config.rows}x${config.cols})"

    @Composable
    override fun Render(
        config: HwComponentConfig,
        snapshot: ComponentSnapshot?,
        portValue: Int,
        onUserInput: (HwUserInput) -> Unit,
    ) {
        val s = snapshot as? KeypadSnapshot
        Text(
            "${config.port.name} Keypad (${config.rows}x${config.cols})",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        MatrixKeypad(
            rows = config.rows,
            cols = config.cols,
            pressedKeys = s?.pressedKeys ?: emptySet(),
            onKey = { r, c, pressed -> onUserInput(HwUserInput.KeyInput(config.id, r, c, pressed)) },
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun ConfigFields(
        config: HwComponentConfig,
        onUpdateComponent: (String, (HwComponentConfig) -> HwComponentConfig) -> Unit,
    ) {
        PortField(config, onUpdateComponent)

        var sizeExpanded by remember { mutableStateOf(false) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Size", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(40.dp))
            ExposedDropdownMenuBox(
                expanded = sizeExpanded,
                onExpandedChange = { sizeExpanded = it },
                modifier = Modifier.widthIn(min = 70.dp),
            ) {
                OutlinedTextField(
                    value = "${config.rows}\u00D7${config.cols}",
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sizeExpanded) },
                    modifier = Modifier.menuAnchor(),
                    textStyle = MaterialTheme.typography.bodySmall,
                )
                ExposedDropdownMenu(expanded = sizeExpanded, onDismissRequest = { sizeExpanded = false }) {
                    listOf(2, 4, 8).forEach { v ->
                        DropdownMenuItem(text = { Text("${v}\u00D7${v}") }, onClick = {
                            onUpdateComponent(config.id) { it.copy(rows = v, cols = v) }
                            sizeExpanded = false
                        })
                    }
                }
            }
        }
    }
}
