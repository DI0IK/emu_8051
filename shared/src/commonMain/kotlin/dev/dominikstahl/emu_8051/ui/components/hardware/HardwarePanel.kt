@file:OptIn(ExperimentalMaterial3Api::class)

package dev.dominikstahl.emu_8051.ui.components.hardware

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.dominikstahl.emu_8051.ui.HwComponentConfig

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HardwarePanel(
    p0: Int,
    p1: Int,
    p2: Int,
    p3: Int,
    hwConfig: List<HwComponentConfig>,
    snapshots: Map<String, ComponentSnapshot> = emptyMap(),
    onUserInput: (HwUserInput) -> Unit = {},
    onAddComponent: (String) -> Unit = {},
    onRemoveComponent: (String) -> Unit = {},
    onUpdateComponent: (String, (HwComponentConfig) -> HwComponentConfig) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableStateOf(0) }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(8.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Display") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Configure") })
            }

            Spacer(Modifier.height(4.dp))

            if (selectedTab == 0) {
                DisplayTab(p0, p1, p2, p3, hwConfig, snapshots, onUserInput)
            } else {
                ConfigureTab(hwConfig, onAddComponent, onRemoveComponent, onUpdateComponent)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DisplayTab(
    p0: Int,
    p1: Int,
    p2: Int,
    p3: Int,
    hwConfig: List<HwComponentConfig>,
    snapshots: Map<String, ComponentSnapshot>,
    onUserInput: (HwUserInput) -> Unit,
) {
    val enabled = hwConfig.filter { it.enabled }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (comp in enabled) {
            val factory = HwRegistry.get(comp.type)
            if (factory != null) {
                val portValue = portValue(p0, p1, p2, p3, comp.port)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    tonalElevation = 0.5.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        factory.Render(comp, snapshots[comp.id], portValue, onUserInput)
                    }
                }
            }
        }
    }

    if (enabled.isEmpty()) {
        Text(
            "No components enabled.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun portValue(p0: Int, p1: Int, p2: Int, p3: Int, port: dev.dominikstahl.emu_8051.ui.Port): Int = when (port) {
    dev.dominikstahl.emu_8051.ui.Port.P0 -> p0
    dev.dominikstahl.emu_8051.ui.Port.P1 -> p1
    dev.dominikstahl.emu_8051.ui.Port.P2 -> p2
    dev.dominikstahl.emu_8051.ui.Port.P3 -> p3
}

private val typeColors = listOf(
    0xFF66D9EF, 0xFFA6E22E, 0xFFFD971F, 0xFFAE81FF, 0xFFF92672, 0xFFE6DB74
)

@Composable
private fun ConfigureTab(
    hwConfig: List<HwComponentConfig>,
    onAddComponent: (String) -> Unit,
    onRemoveComponent: (String) -> Unit,
    onUpdateComponent: (String, (HwComponentConfig) -> HwComponentConfig) -> Unit,
) {
    var addTypeExpanded by remember { mutableStateOf(false) }
    val factoryList = remember { HwRegistry.allFactories().toList() }
    var selectedAddType by remember { mutableStateOf(factoryList.firstOrNull()?.typeId ?: "") }
    val expandedItems = remember { mutableStateMapOf<String, Boolean>() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        ExposedDropdownMenuBox(
            expanded = addTypeExpanded,
            onExpandedChange = { addTypeExpanded = it },
            modifier = Modifier.weight(1f),
        ) {
            val selectedFactory = HwRegistry.get(selectedAddType)
            OutlinedTextField(
                value = selectedFactory?.displayName() ?: selectedAddType,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = addTypeExpanded) },
                modifier = Modifier.menuAnchor().widthIn(min = 50.dp),
                textStyle = MaterialTheme.typography.bodySmall,
            )
            ExposedDropdownMenu(
                expanded = addTypeExpanded,
                onDismissRequest = { addTypeExpanded = false },
            ) {
                factoryList.forEach { factory ->
                    DropdownMenuItem(
                        text = { Text(factory.displayName()) },
                        onClick = {
                            selectedAddType = factory.typeId
                            addTypeExpanded = false
                        },
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        TextButton(onClick = { onAddComponent(selectedAddType) }) {
            Text("Add", style = MaterialTheme.typography.labelSmall)
        }
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

    if (hwConfig.isEmpty()) {
        Text(
            "No components. Add one above.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        return
    }

    for ((index, comp) in hwConfig.withIndex()) {
        val expanded = expandedItems[comp.id] ?: false
        val typeColor = Color(typeColors[index % typeColors.size])

        Surface(
            shape = RoundedCornerShape(4.dp),
            color = if (expanded) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    else MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().clickable { expandedItems[comp.id] = !expanded },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (expanded) "\u25BE" else "\u25B8",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp),
                )
                Text(
                    comp.label,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                )
                val factory = HwRegistry.get(comp.type)
                Text(
                    factory?.displayName() ?: comp.type,
                    style = MaterialTheme.typography.labelSmall,
                    color = typeColor,
                )
                IconButton(
                    onClick = { onRemoveComponent(comp.id) },
                    modifier = Modifier.size(28.dp),
                ) {
                    Text("\u2715", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val factory = HwRegistry.get(comp.type)
                if (factory != null) {
                    factory.ConfigFields(comp, onUpdateComponent)
                } else {
                    Text(
                        "Unknown component type: ${comp.type}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
