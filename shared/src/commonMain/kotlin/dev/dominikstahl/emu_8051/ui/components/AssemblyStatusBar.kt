package dev.dominikstahl.emu_8051.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.dominikstahl.emu_8051.asm.AssemblyError
import dev.dominikstahl.emu_8051.asm.AssemblyWarning

@Composable
fun AssemblyStatusBar(
    errors: List<AssemblyError>,
    warnings: List<AssemblyWarning> = emptyList(),
    isProgramLoaded: Boolean,
    currentFileName: String? = null,
    isDirty: Boolean = false,
    onJumpToLine: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var expanded by remember(errors.isNotEmpty()) { mutableStateOf(errors.isNotEmpty()) }
    val hasMessages = errors.isNotEmpty() || warnings.isNotEmpty()

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = hasMessages) { expanded = !expanded }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when {
                        errors.isNotEmpty() -> {
                            Text(
                                "${errors.size} error${if (errors.size != 1) "s" else ""}",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                        isProgramLoaded -> {
                            Text(
                                "Assembled OK",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                        else -> {
                            Text(
                                "Ready",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }

                    if (warnings.isNotEmpty()) {
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "${warnings.size} warning${if (warnings.size != 1) "s" else ""}",
                            color = Color(0xFFFFB74D),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }

                    if (hasMessages) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (expanded) "\u25BE" else "\u25B8",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }

                if (currentFileName != null || isDirty) {
                    val fileLabel = (currentFileName ?: "untitled.asm") + if (isDirty) " *" else ""
                    Text(
                        fileLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded && hasMessages,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 110.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    for (err in errors) {
                        Text(
                            if (err.line > 0) "Line ${err.line}: ${err.message}" else err.message,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = err.line > 0) { onJumpToLine(err.line) },
                        )
                    }
                    for (warn in warnings) {
                        Text(
                            if (warn.line > 0) "Warning line ${warn.line}: ${warn.message}" else "Warning: ${warn.message}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFFB74D),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = warn.line > 0) { onJumpToLine(warn.line) },
                        )
                    }
                }
            }
        }
    }
}
