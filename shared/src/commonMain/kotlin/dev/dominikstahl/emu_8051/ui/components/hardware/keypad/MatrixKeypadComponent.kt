package dev.dominikstahl.emu_8051.ui.components.hardware.keypad

import androidx.compose.foundation.layout.Row
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
import dev.dominikstahl.emu_8051.ui.components.hardware.PortFields

data class KeypadSnapshot(val pressedKeys: Set<Int>) : ComponentSnapshot()

class MatrixKeypadComponent(config: HwComponentConfig) : HwComponent(config) {
    // Key packed as (row shl 4) or col to avoid String allocations in tick()
    private val pressedKeys = mutableSetOf<Int>()
    private var drivenMask = 0
    private var portCtrl: PortController? = null

    private val isTwoPort: Boolean get() = config.rows > 4 || config.cols > 4
    private val colPort: Port
        get() = if (isTwoPort) (config.ports.getOrNull(1) ?: Port.P2) else config.port
    private val colBitShift: Int get() = if (isTwoPort) 0 else 4

    override fun needsTick() = true

    override fun onUserInput(input: HwUserInput, portCtrl: PortController) {
        this.portCtrl = portCtrl
        if (input is HwUserInput.KeyInput && input.compId == config.id) {
            val key = (input.row shl 4) or (input.col and 0x0F)
            if (input.pressed) {
                pressedKeys.add(key)
            } else {
                pressedKeys.remove(key)
            }
        }
    }

    override fun tick(portValues: List<Int>, totalCycles: Long) {
        val ctrl = portCtrl ?: return
        val rowPortVal = portValues.getOrNull(0) ?: return
        var desiredMask = 0

        for (key in pressedKeys) {
            val row = key ushr 4
            val col = key and 0x0F
            if ((rowPortVal and (1 shl row)) == 0) {
                desiredMask = desiredMask or (1 shl (col + colBitShift))
            }
        }

        val toRelease = drivenMask and desiredMask.inv()
        if (toRelease != 0) {
            ctrl.release(colPort, toRelease)
        }
        val toDrive = desiredMask and drivenMask.inv()
        if (toDrive != 0) {
            ctrl.drive(colPort, toDrive, 0)
        }
        drivenMask = desiredMask
    }

    override fun reset() {
        pressedKeys.clear()
        if (drivenMask != 0) {
            portCtrl?.release(colPort, drivenMask)
            drivenMask = 0
        }
    }

    override fun onRemove(portCtrl: PortController) {
        pressedKeys.clear()
        if (drivenMask != 0) {
            portCtrl.release(colPort, drivenMask)
            drivenMask = 0
        }
    }

    override fun snapshot(): ComponentSnapshot = KeypadSnapshot(pressedKeys.toSet())
}

object MatrixKeypadFactory : HwComponentFactory {
    override val typeId = "MATRIX_KEYPAD"
    override fun displayName() = "Matrix Keypad"

    override fun createComponent(config: HwComponentConfig): HwComponent = MatrixKeypadComponent(config)

    override fun defaultConfig(id: String) = HwComponentConfig(id, "P1 Keypad (4x4)", typeId, ports = listOf(Port.P1), rows = 4, cols = 4)

    override fun portCount(config: HwComponentConfig) = if (config.rows > 4 || config.cols > 4) 2 else 1

    override fun portLabels(config: HwComponentConfig) =
        if (portCount(config) == 2) listOf("Row Port", "Col Port") else listOf("Port")

    override fun labelFor(config: HwComponentConfig): String {
        return if (portCount(config) == 2) {
            val p1 = config.ports.getOrNull(0)?.name ?: "P1"
            val p2 = config.ports.getOrNull(1)?.name ?: "P2"
            "$p1/$p2 Keypad (${config.rows}x${config.cols})"
        } else {
            "${config.port.name} Keypad (${config.rows}x${config.cols})"
        }
    }

    override fun needsTick(config: HwComponentConfig) = true

    override fun usedPins(config: HwComponentConfig): List<Pair<Port, Int>> {
        return if (portCount(config) == 2) {
            val rowPort = config.ports.getOrNull(0) ?: Port.P1
            val colPort = config.ports.getOrNull(1) ?: Port.P2
            (0 until config.rows.coerceAtMost(8)).map { rowPort to it } +
                (0 until config.cols.coerceAtMost(8)).map { colPort to it }
        } else {
            (0 until config.rows.coerceAtMost(4)).map { config.port to it } +
                (4 until (4 + config.cols).coerceAtMost(8)).map { config.port to it }
        }
    }

    @Composable
    override fun Render(
        config: HwComponentConfig,
        snapshot: ComponentSnapshot?,
        portValues: List<Int>,
        onUserInput: (HwUserInput) -> Unit,
    ) {
        val s = snapshot as? KeypadSnapshot
        Text(
            labelFor(config),
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
        if (portCount(config) == 2) {
            PortFields(
                portCount = 2,
                config = config,
                onUpdateComponent = onUpdateComponent,
                labels = portLabels(config),
            )
        } else {
            PortField(config, onUpdateComponent)
        }

        var sizeExpanded by remember { mutableStateOf(false) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Size", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(64.dp))
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
                            onUpdateComponent(config.id) { c ->
                                val ports = if (v > 4 && c.ports.size < 2) {
                                    listOf(c.port, if (c.port == Port.P2) Port.P1 else Port.P2)
                                } else {
                                    c.ports
                                }
                                c.copy(rows = v, cols = v, ports = ports)
                            }
                            sizeExpanded = false
                        })
                    }
                }
            }
        }
    }
}
