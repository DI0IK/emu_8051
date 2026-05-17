package dev.dominikstahl.emu_8051.ui.components.hardware

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
import dev.dominikstahl.emu_8051.ui.components.hardware.hd44780.Hd44780Factory
import dev.dominikstahl.emu_8051.ui.components.hardware.keypad.MatrixKeypadFactory
import dev.dominikstahl.emu_8051.ui.components.hardware.led.LedFactory
import dev.dominikstahl.emu_8051.ui.components.hardware.ledbar.LedBarFactory
import dev.dominikstahl.emu_8051.ui.components.hardware.sevenseg.SevenSegFactory
import dev.dominikstahl.emu_8051.ui.components.hardware.toggle.ToggleFactory
import dev.dominikstahl.emu_8051.ui.components.hardware.uart.UartFactory
import dev.dominikstahl.emu_8051.ui.components.hardware.stepper.StepperFactory
import dev.dominikstahl.emu_8051.ui.components.hardware.buzzer.BuzzerFactory
import dev.dominikstahl.emu_8051.ui.components.hardware.servo.ServoFactory

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

    @Composable
    fun Render(
        config: HwComponentConfig,
        snapshot: ComponentSnapshot?,
        portValue: Int,
        onUserInput: (HwUserInput) -> Unit,
    )

    @Composable
    fun ConfigFields(
        config: HwComponentConfig,
        onUpdateComponent: (String, (HwComponentConfig) -> HwComponentConfig) -> Unit,
    )
}

object HwRegistry {
    private val factories = mutableMapOf<String, HwComponentFactory>()

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
    HwRegistry.register(ToggleFactory)
    HwRegistry.register(MatrixKeypadFactory)
    HwRegistry.register(Hd44780Factory)
    HwRegistry.register(UartFactory)
    HwRegistry.register(StepperFactory)
    HwRegistry.register(BuzzerFactory)
    HwRegistry.register(ServoFactory)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortField(
    config: HwComponentConfig,
    onUpdateComponent: (String, (HwComponentConfig) -> HwComponentConfig) -> Unit,
) {
    var portExpanded by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Port", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(40.dp))
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
                            onUpdateComponent(config.id) { it.copy(port = p) }
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
fun PinField(
    config: HwComponentConfig,
    onPinChange: (Int) -> Unit,
) {
    var pinExpanded by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Pin", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(40.dp))
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
