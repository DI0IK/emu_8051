package dev.dominikstahl.emu_8051.ui.components.hardware

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import dev.dominikstahl.emu_8051.ui.components.hardware.button.PushButtonFactory
import dev.dominikstahl.emu_8051.ui.components.hardware.buzzer.BuzzerFactory
import dev.dominikstahl.emu_8051.ui.components.hardware.dipswitch.DipSwitchFactory
import dev.dominikstahl.emu_8051.ui.components.hardware.hd44780.Hd44780Factory
import dev.dominikstahl.emu_8051.ui.components.hardware.keypad.MatrixKeypadFactory
import dev.dominikstahl.emu_8051.ui.components.hardware.led.LedFactory
import dev.dominikstahl.emu_8051.ui.components.hardware.ledbar.LedBarFactory
import dev.dominikstahl.emu_8051.ui.components.hardware.ledmatrix.LedMatrixFactory
import dev.dominikstahl.emu_8051.ui.components.hardware.servo.ServoFactory
import dev.dominikstahl.emu_8051.ui.components.hardware.sevenseg.MultiSevenSegFactory
import dev.dominikstahl.emu_8051.ui.components.hardware.sevenseg.SevenSegFactory
import dev.dominikstahl.emu_8051.ui.components.hardware.stepper.StepperFactory
import dev.dominikstahl.emu_8051.ui.components.hardware.toggle.ToggleFactory
import dev.dominikstahl.emu_8051.ui.components.hardware.uart.UartFactory

interface PortController {
    fun drive(port: Port, mask: Int, value: Int)
    fun release(port: Port, mask: Int)
}

interface HwComponentFactory {
    val typeId: String
    fun displayName(): String
    fun createComponent(config: HwComponentConfig): HwComponent
    fun defaultConfig(id: String): HwComponentConfig
    fun labelFor(config: HwComponentConfig): String
    fun needsTick(config: HwComponentConfig): Boolean = false
    fun portCount(config: HwComponentConfig): Int = 1

    fun portLabels(config: HwComponentConfig): List<String> =
        List(portCount(config)) { i -> if (portCount(config) == 1) "Port" else "Port ${i + 1}" }

    fun usedPins(config: HwComponentConfig): List<Pair<Port, Int>> =
        if (config.pin in 0..7) listOf(config.port to config.pin)
        else (0..7).map { config.port to it }

    @Composable
    fun Render(
        config: HwComponentConfig,
        snapshot: ComponentSnapshot?,
        portValues: List<Int>,
        onUserInput: (HwUserInput) -> Unit,
    )

    @Composable
    fun ConfigFields(
        config: HwComponentConfig,
        onUpdateComponent: (String, (HwComponentConfig) -> HwComponentConfig) -> Unit,
    )
}

object HwRegistry {
    private val factories = linkedMapOf<String, HwComponentFactory>()

    fun register(factory: HwComponentFactory) {
        factories[factory.typeId] = factory
    }

    fun get(typeId: String): HwComponentFactory? = factories[typeId]

    fun allFactories(): List<HwComponentFactory> = factories.values.toList()

    fun allTypeIds(): List<String> = factories.keys.toList()
}

fun registerBuiltinHardwareComponents() {
    HwRegistry.register(LedFactory)
    HwRegistry.register(LedBarFactory)
    HwRegistry.register(SevenSegFactory)
    HwRegistry.register(MultiSevenSegFactory)
    HwRegistry.register(ToggleFactory)
    HwRegistry.register(PushButtonFactory)
    HwRegistry.register(DipSwitchFactory)
    HwRegistry.register(MatrixKeypadFactory)
    HwRegistry.register(Hd44780Factory)
    HwRegistry.register(UartFactory)
    HwRegistry.register(StepperFactory)
    HwRegistry.register(BuzzerFactory)
    HwRegistry.register(ServoFactory)
    HwRegistry.register(LedMatrixFactory)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortField(
    config: HwComponentConfig,
    onUpdateComponent: (String, (HwComponentConfig) -> HwComponentConfig) -> Unit,
    label: String = "Port",
) {
    var portExpanded by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(64.dp))
        ExposedDropdownMenuBox(
            expanded = portExpanded,
            onExpandedChange = { portExpanded = it },
            modifier = Modifier.widthIn(min = 70.dp),
        ) {
            OutlinedTextField(
                value = config.port.name,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = portExpanded) },
                modifier = Modifier.menuAnchor(),
                textStyle = MaterialTheme.typography.bodySmall,
            )
            ExposedDropdownMenu(expanded = portExpanded, onDismissRequest = { portExpanded = false }) {
                Port.entries.forEach { p ->
                    DropdownMenuItem(
                        text = { Text(p.name) },
                        onClick = {
                            onUpdateComponent(config.id) {
                                it.copy(ports = it.ports.toMutableList().apply { if (isNotEmpty()) set(0, p) else add(p) })
                            }
                            portExpanded = false
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortFields(
    portCount: Int,
    config: HwComponentConfig,
    onUpdateComponent: (String, (HwComponentConfig) -> HwComponentConfig) -> Unit,
    labels: List<String> = emptyList(),
) {
    for (i in 0 until portCount) {
        val port = config.ports.getOrNull(i) ?: Port.entries.getOrElse(i + 1) { Port.P1 }
        val fieldLabel = labels.getOrNull(i) ?: "Port ${i + 1}"
        var portExpanded by remember { mutableStateOf(false) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(fieldLabel, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(64.dp))
            ExposedDropdownMenuBox(
                expanded = portExpanded,
                onExpandedChange = { portExpanded = it },
                modifier = Modifier.widthIn(min = 70.dp),
            ) {
                OutlinedTextField(
                    value = port.name,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = portExpanded) },
                    modifier = Modifier.menuAnchor(),
                    textStyle = MaterialTheme.typography.bodySmall,
                )
                ExposedDropdownMenu(expanded = portExpanded, onDismissRequest = { portExpanded = false }) {
                    Port.entries.forEach { p ->
                        DropdownMenuItem(
                            text = { Text(p.name) },
                            onClick = {
                                onUpdateComponent(config.id) { c ->
                                    val newPorts = c.ports.toMutableList()
                                    while (newPorts.size <= i) newPorts.add(Port.entries.getOrElse(newPorts.size + 1) { Port.P1 })
                                    newPorts[i] = p
                                    c.copy(ports = newPorts)
                                }
                                portExpanded = false
                            },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinField(
    config: HwComponentConfig,
    onPinChange: (Int) -> Unit,
) {
    var pinExpanded by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Pin", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(64.dp))
        ExposedDropdownMenuBox(
            expanded = pinExpanded,
            onExpandedChange = { pinExpanded = it },
            modifier = Modifier.widthIn(min = 70.dp),
        ) {
            OutlinedTextField(
                value = config.pin.toString(),
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = pinExpanded) },
                modifier = Modifier.menuAnchor(),
                textStyle = MaterialTheme.typography.bodySmall,
            )
            ExposedDropdownMenu(expanded = pinExpanded, onDismissRequest = { pinExpanded = false }) {
                (0..7).forEach { v ->
                    DropdownMenuItem(text = { Text(v.toString()) }, onClick = {
                        onPinChange(v)
                        pinExpanded = false
                    })
                }
            }
        }
    }
}

@Composable
fun PolarityField(
    config: HwComponentConfig,
    onUpdateComponent: (String, (HwComponentConfig) -> HwComponentConfig) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Active LOW",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(64.dp),
        )
        Switch(
            checked = config.activeLow,
            onCheckedChange = { checked ->
                onUpdateComponent(config.id) { it.copy(activeLow = checked) }
            },
        )
    }
}
