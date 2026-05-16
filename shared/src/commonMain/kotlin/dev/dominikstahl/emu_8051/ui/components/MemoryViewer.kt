package dev.dominikstahl.emu_8051.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private enum class MemTab { ROM, RAM, SFR }

@OptIn(ExperimentalUnsignedTypes::class)
@Composable
fun MemoryViewer(
    getRom: () -> UByteArray,
    getRam: () -> UByteArray,
    getSfr: () -> UByteArray,
    stateKey: Any,
    modifier: Modifier = Modifier,
    overrideBytesPerLine: Int? = null,
    onWidthCalculated: (androidx.compose.ui.unit.Dp) -> Unit = {},
) {
    var tab by remember { mutableStateOf(MemTab.ROM) }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PrimaryTabRow(selectedTabIndex = tab.ordinal) {
                MemTab.entries.forEach { t ->
                    Tab(
                        selected = tab == t,
                        onClick = { tab = t },
                        text = { Text(t.name, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }
            val data = when (tab) {
                MemTab.ROM -> getRom()
                MemTab.RAM -> getRam()
                MemTab.SFR -> getSfr()
            }
            MemoryHexView(
                data = data,
                baseAddress = if (tab == MemTab.SFR) 0x80 else 0,
                modifier = Modifier.fillMaxSize().padding(4.dp),
                stateKey = Pair(stateKey, tab),
                onWidthCalculated = onWidthCalculated,
                overrideBytesPerLine = overrideBytesPerLine,
            )
        }
    }
}
