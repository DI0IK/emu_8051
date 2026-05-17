package dev.dominikstahl.emu_8051.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import dev.dominikstahl.emu_8051.ui.CustomIcons
import dev.dominikstahl.emu_8051.ui.SpeedMode

private fun ipsLabel(ips: Int): String = when (ips) {
    10 -> "10"
    100 -> "100"
    1_000 -> "1K"
    100_000 -> "100K"
    1_000_000 -> "1M"
    12_000_000 -> "12M"
    else -> ips.toString()
}

private fun speedLabel(speedMode: SpeedMode, targetIps: Int): String = when (speedMode) {
    SpeedMode.MANUAL -> "Manual"
    SpeedMode.CUSTOM -> "${ipsLabel(targetIps)} IPS"
    SpeedMode.UNLIMITED -> "Unlimited"
}

@Composable
fun ControlBar(
    isRunning: Boolean,
    speedMode: SpeedMode,
    actualIps: Long,
    targetIps: Int,
    onStep: () -> Unit,
    onRun: (SpeedMode) -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
    onLoad: () -> Unit,
    onPaste: () -> Unit,
    onShare: () -> Unit,
    onSetTargetIps: (Int) -> Unit,
    onSetSpeed: (SpeedMode) -> Unit,
    isSlow: Boolean,
    isMobile: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showIpsDialog by remember { mutableStateOf(false) }
    var dialogIpsText by remember(targetIps) { mutableStateOf(targetIps.toString()) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        if (isMobile) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ActionButtons(
                        isRunning = isRunning,
                        speedMode = speedMode,
                        onReset = onReset,
                        onStep = onStep,
                        onRun = onRun,
                        onPause = onPause,
                        onSetSpeed = onSetSpeed,
                    )
                    SpeedSection(
                        speedMode = speedMode,
                        targetIps = targetIps,

                        isRunning = isRunning,
                        isSlow = isSlow,
                        showSpeedMenu = showSpeedMenu,
                        onToggleSpeedMenu = { showSpeedMenu = it },
                        onSetSpeed = onSetSpeed,
                        onSetTargetIps = onSetTargetIps,
                        onShowIpsDialog = { dialogIpsText = targetIps.toString(); showIpsDialog = true },
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    IconButton(onClick = onSave) {
                        Icon(CustomIcons.Save, contentDescription = "Save")
                    }
                    IconButton(onClick = onLoad) {
                        Icon(CustomIcons.FolderOpen, contentDescription = "Load")
                    }
                    IconButton(onClick = onPaste) {
                        Icon(CustomIcons.ContentPaste, contentDescription = "Paste")
                    }
                    IconButton(onClick = onShare) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ActionButtons(
                    isRunning = isRunning,
                    speedMode = speedMode,
                    onReset = onReset,
                    onStep = onStep,
                    onRun = onRun,
                    onPause = onPause,
                    onSetSpeed = onSetSpeed,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    IconButton(onClick = onSave) {
                        Icon(CustomIcons.Save, contentDescription = "Save")
                    }
                    IconButton(onClick = onLoad) {
                        Icon(CustomIcons.FolderOpen, contentDescription = "Load")
                    }
                    IconButton(onClick = onPaste) {
                        Icon(CustomIcons.ContentPaste, contentDescription = "Paste")
                    }
                    IconButton(onClick = onShare) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    SpeedSection(
                        speedMode = speedMode,
                        targetIps = targetIps,

                        isRunning = isRunning,
                        isSlow = isSlow,
                        showSpeedMenu = showSpeedMenu,
                        onToggleSpeedMenu = { showSpeedMenu = it },
                        onSetSpeed = onSetSpeed,
                        onSetTargetIps = onSetTargetIps,
                        onShowIpsDialog = { dialogIpsText = targetIps.toString(); showIpsDialog = true },
                    )
                }
            }
        }
    }

    if (showIpsDialog) {
        AlertDialog(
            onDismissRequest = { showIpsDialog = false },
            title = { Text("Custom Speed") },
            text = {
                OutlinedTextField(
                    value = dialogIpsText,
                    onValueChange = { dialogIpsText = it.filter { c -> c.isDigit() } },
                    label = { Text("Instructions per second") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val value = dialogIpsText.toIntOrNull()
                    if (value != null && value > 0) {
                        onSetTargetIps(value)
                        onSetSpeed(SpeedMode.CUSTOM)
                    }
                    showIpsDialog = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showIpsDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun ActionButtons(
    isRunning: Boolean,
    speedMode: SpeedMode,
    onReset: () -> Unit,
    onStep: () -> Unit,
    onRun: (SpeedMode) -> Unit,
    onPause: () -> Unit,
    onSetSpeed: (SpeedMode) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        IconButton(onClick = onReset) {
            Icon(Icons.Default.Refresh, contentDescription = "Reset")
        }
        IconButton(onClick = onStep, enabled = !isRunning) {
            Icon(CustomIcons.SkipNext, contentDescription = "Step")
        }
        if (isRunning) {
            IconButton(onClick = onPause) {
                Icon(CustomIcons.Pause, contentDescription = "Pause")
            }
        } else {
            IconButton(
                onClick = { onRun(speedMode) },
                enabled = speedMode != SpeedMode.MANUAL,
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Run")
            }
        }
    }
}

private data class SpeedPreset(val label: String, val ips: Int)

private val speedPresets = listOf(
    SpeedPreset("10", 10),
    SpeedPreset("100", 100),
    SpeedPreset("1K", 1_000),
    SpeedPreset("100K", 100_000),
    SpeedPreset("1M", 1_000_000),
    SpeedPreset("12M", 12_000_000),
)

@Composable
private fun SpeedSection(
    speedMode: SpeedMode,
    targetIps: Int,
    isRunning: Boolean,
    isSlow: Boolean,
    showSpeedMenu: Boolean,
    onToggleSpeedMenu: (Boolean) -> Unit,
    onSetSpeed: (SpeedMode) -> Unit,
    onSetTargetIps: (Int) -> Unit,
    onShowIpsDialog: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        OutlinedButton(onClick = { onToggleSpeedMenu(true) }) {
            Text(
                speedLabel(speedMode, targetIps),
                style = MaterialTheme.typography.labelSmall,
            )
        }
        DropdownMenu(
            expanded = showSpeedMenu,
            onDismissRequest = { onToggleSpeedMenu(false) },
        ) {
            DropdownMenuItem(
                text = { Text("Manual", style = MaterialTheme.typography.bodySmall) },
                onClick = { onToggleSpeedMenu(false); onSetSpeed(SpeedMode.MANUAL) },
            )
            HorizontalDivider()
            speedPresets.forEach { preset ->
                DropdownMenuItem(
                    text = { Text("${preset.label} IPS", style = MaterialTheme.typography.bodySmall) },
                    onClick = {
                        onToggleSpeedMenu(false)
                        onSetTargetIps(preset.ips)
                        onSetSpeed(SpeedMode.CUSTOM)
                    },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Unlimited", style = MaterialTheme.typography.bodySmall) },
                onClick = { onToggleSpeedMenu(false); onSetSpeed(SpeedMode.UNLIMITED) },
            )
            DropdownMenuItem(
                text = { Text("Custom\u2026", style = MaterialTheme.typography.bodySmall) },
                onClick = { onToggleSpeedMenu(false); onShowIpsDialog() },
            )
        }

        if (isRunning && isSlow) {
            Spacer(Modifier.width(4.dp))
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.extraSmall,
            ) {
                Text(
                    "SLOW",
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}
