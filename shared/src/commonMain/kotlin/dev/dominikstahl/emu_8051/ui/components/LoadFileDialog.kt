package dev.dominikstahl.emu_8051.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadFileDialog(
    savedFiles: List<String>,
    onLoad: (String) -> Unit,
    onDelete: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedFile by remember { mutableStateOf<String?>(null) }
    var renameTarget by remember { mutableStateOf<String?>(null) }
    var renameText by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp),
        ) {
            Text(
                "Load ASM Source",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(12.dp))

            when {
                deleteTarget != null -> {
                    Text(
                        "Delete \"${deleteTarget}\"?",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "This cannot be undone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { deleteTarget = null }) {
                            Text("Back")
                        }
                        TextButton(onClick = {
                            onDelete(deleteTarget!!)
                            selectedFile = if (selectedFile == deleteTarget) null else selectedFile
                            deleteTarget = null
                        }) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                renameTarget != null -> {
                    Text(
                        "Rename \"${renameTarget}\" to:",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(8.dp))
                    val fullNewName = if (renameText.endsWith(".asm")) renameText else "$renameText.asm"
                    val hasInvalidChars = renameText.contains("/") || renameText.contains("\\")
                    val isValid = renameText.isNotBlank() && !hasInvalidChars && fullNewName != renameTarget
                    OutlinedTextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        label = { Text("New filename") },
                        singleLine = true,
                        isError = hasInvalidChars,
                        supportingText = if (hasInvalidChars) {
                            { Text("Invalid character") }
                        } else null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { renameTarget = null }) {
                            Text("Back")
                        }
                        TextButton(
                            onClick = {
                                onRename(renameTarget!!, fullNewName)
                                selectedFile = fullNewName
                                renameTarget = null
                            },
                            enabled = isValid,
                        ) {
                            Text("Rename")
                        }
                    }
                }
                else -> {
                    Text(
                        "Saved files:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    if (savedFiles.isEmpty()) {
                        Text(
                            "No saved source files.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 24.dp),
                        )
                    } else {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth().height(220.dp),
                        ) {
                            Column(Modifier.verticalScroll(rememberScrollState())) {
                                savedFiles.forEachIndexed { index, file ->
                                    val isSelected = file == selectedFile
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedFile = file }
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                                else Color.Transparent,
                                                shape = RoundedCornerShape(4.dp),
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            file,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isSelected)
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            else
                                                MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f),
                                        )
                                        IconButton(
                                            onClick = {
                                                renameTarget = file
                                                renameText = file.removeSuffix(".asm")
                                            },
                                            modifier = Modifier.size(32.dp),
                                        ) {
                                            Text(
                                                "\u270E",
                                                style = MaterialTheme.typography.bodySmall,
                                            )
                                        }
                                        IconButton(
                                            onClick = { deleteTarget = file },
                                            modifier = Modifier.size(32.dp),
                                        ) {
                                            Text(
                                                "\u2715",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error,
                                            )
                                        }
                                    }
                                    if (index < savedFiles.lastIndex) {
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant,
                                            modifier = Modifier.padding(start = 8.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (selectedFile != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Selected: $selectedFile",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (deleteTarget == null && renameTarget == null) {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = { selectedFile?.let { onLoad(it) } },
                        enabled = selectedFile != null,
                    ) {
                        Text("Load")
                    }
                }
            }
        }
    }
}
