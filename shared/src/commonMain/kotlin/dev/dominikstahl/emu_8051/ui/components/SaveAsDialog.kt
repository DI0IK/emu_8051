package dev.dominikstahl.emu_8051.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveAsDialog(
    savedFiles: List<String>,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var fileName by remember { mutableStateOf("program.asm") }
    var overwriteConfirmed by remember { mutableStateOf(false) }

    val fullName = if (fileName.endsWith(".asm")) fileName else "$fileName.asm"
    val exists = fullName in savedFiles
    val hasInvalidChars = fileName.contains("/") || fileName.contains("\\")
    val isValid = fileName.isNotBlank() && !hasInvalidChars

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp),
        ) {
            Text(
                "Save ASM Source",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = fileName,
                onValueChange = { fileName = it; overwriteConfirmed = false },
                label = { Text("File name") },
                singleLine = true,
                isError = hasInvalidChars,
                supportingText = if (hasInvalidChars) {
                    { Text("Invalid character: / or \\") }
                } else if (fileName.isBlank()) {
                    { Text("Name cannot be empty") }
                } else null,
                modifier = Modifier.fillMaxWidth(),
            )

            if (exists) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (overwriteConfirmed)
                        MaterialTheme.colorScheme.errorContainer
                    else
                        MaterialTheme.colorScheme.tertiaryContainer,
                ) {
                    Text(
                        if (overwriteConfirmed)
                            "Press Save again to overwrite \"$fullName\"."
                        else
                            "\"$fullName\" already exists.",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (overwriteConfirmed)
                            MaterialTheme.colorScheme.onErrorContainer
                        else
                            MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }

            if (savedFiles.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Existing files:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                ) {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        savedFiles.forEach { file ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        fileName = file
                                        overwriteConfirmed = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    file,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }

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
                    onClick = {
                        if (exists && !overwriteConfirmed) {
                            overwriteConfirmed = true
                        } else {
                            onSave(fullName)
                        }
                    },
                    enabled = isValid,
                ) {
                    Text(
                        if (exists && overwriteConfirmed) "Overwrite"
                        else "Save"
                    )
                }
            }
        }
    }
}
