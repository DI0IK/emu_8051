@file:OptIn(ExperimentalUnsignedTypes::class, ExperimentalMaterial3Api::class)

package dev.dominikstahl.emu_8051.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.dominikstahl.emu_8051.ui.MemorySnapshots

private enum class MemTab { ROM, RAM, XRAM, SFR }

@Composable
fun MemoryViewer(
    snapshots: MemorySnapshots,
    pc: Int,
    sp: Int,
    modifier: Modifier = Modifier,
    overrideBytesPerLine: Int? = null,
    onWidthCalculated: (Dp) -> Unit = {},
) {
    var tab by remember { mutableStateOf(MemTab.ROM) }
    var addrText by remember { mutableStateOf("") }
    var scrollReqId by remember { mutableStateOf(0) }
    var scrollReq by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    fun jumpToAddr() {
        val parsed = addrText.trim().removePrefix("0x").removePrefix("0X").removeSuffix("h").removeSuffix("H").toIntOrNull(16)
        if (parsed != null) {
            scrollReq = parsed to (++scrollReqId)
        }
    }

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

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        "Addr 0x",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(4.dp))
                    BasicTextField(
                        value = addrText,
                        onValueChange = { s ->
                            addrText = s.filter { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }.take(4)
                        },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .width(56.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.extraSmall)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), MaterialTheme.shapes.extraSmall)
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                            .onPreviewKeyEvent { e ->
                                if (e.type == KeyEventType.KeyDown && e.key == Key.Enter) {
                                    jumpToAddr()
                                    true
                                } else false
                            },
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Go",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable(onClick = ::jumpToAddr)
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }

                when (tab) {
                    MemTab.ROM -> Text(
                        "PC",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { scrollReq = pc to (++scrollReqId) }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                    MemTab.RAM -> Text(
                        "SP",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { scrollReq = sp to (++scrollReqId) }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                    else -> {}
                }
            }

            val data = when (tab) {
                MemTab.ROM -> snapshots.rom
                MemTab.RAM -> snapshots.ram
                MemTab.XRAM -> snapshots.xram
                MemTab.SFR -> snapshots.sfr
            }
            val highlight = when (tab) {
                MemTab.ROM -> pc
                MemTab.RAM -> sp
                else -> null
            }

            MemoryHexView(
                data = data,
                stateKey = Pair(snapshots.version, tab),
                baseAddress = if (tab == MemTab.SFR) 0x80 else 0,
                highlightAddress = highlight,
                scrollToAddressRequest = scrollReq,
                modifier = Modifier.fillMaxSize().padding(4.dp),
                onWidthCalculated = onWidthCalculated,
                overrideBytesPerLine = overrideBytesPerLine,
            )
        }
    }
}
