package dev.dominikstahl.emu_8051.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.dominikstahl.emu_8051.ui.EmulatorUiState

private enum class CpuTab { REGS, TIMERS, INT, SERIAL }

@Composable
fun CpuInfoPanel(
    state: EmulatorUiState,
    modifier: Modifier = Modifier,
) {
    var tab by remember { mutableStateOf(CpuTab.REGS) }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column {
            TabRow(selectedTabIndex = tab.ordinal) {
                CpuTab.entries.forEach { t ->
                    Tab(
                        selected = tab == t,
                        onClick = { tab = t },
                        text = {
                            Text(
                                when (t) {
                                    CpuTab.REGS -> "Regs"
                                    CpuTab.TIMERS -> "Timers"
                                    CpuTab.INT -> "Int"
                                    CpuTab.SERIAL -> "Serial"
                                },
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                    )
                }
            }

            Column(
                modifier = Modifier.padding(6.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                when (tab) {
                    CpuTab.REGS -> RegsContent(state)
                    CpuTab.TIMERS -> TimersContent(state)
                    CpuTab.INT -> IntContent(state)
                    CpuTab.SERIAL -> SerialContent(state)
                }
            }
        }
    }
}

@Composable
private fun RegsContent(state: EmulatorUiState) {
    RegRow("PC", hex(state.pc, 4))
    RegRow("ACC", hex(state.acc))
    RegRow("B", hex(state.b))
    RegRow("SP", hex(state.sp))
    RegRow("DPL", hex(state.dpl))
    RegRow("DPH", hex(state.dph))
    RegRow("DPTR", hex((state.dph shl 8) or state.dpl, 4))
    HorizontalDivider()
    RegRow("P0", hex(state.p0))
    RegRow("P1", hex(state.p1))
    RegRow("P2", hex(state.p2))
    RegRow("P3", hex(state.p3))
    HorizontalDivider()
    RegRow("Cycles", state.totalCycles.toString())
    RegRow("Real time", formatRealTime(state.totalCycles))
    RegRow("Speed", formatMhz(state.actualIps))
    HorizontalDivider()
    PswView(state.psw)
}

@Composable
private fun TimersContent(state: EmulatorUiState) {
    SectionHeader("Timer 0")
    RegRow("TMOD", hex(state.tmod))
    RegRow("TCON", hex(state.tcon))
    RegRow("TH0", hex(state.th0))
    RegRow("TL0", hex(state.tl0))
    Reg16Row("T0 count", (state.th0 shl 8) or state.tl0)
    HorizontalDivider()
    val tmod = state.tmod
    val gate0 = (tmod and 0x08) != 0
    val ct0 = (tmod and 0x04) != 0
    val m10 = (tmod and 0x02) != 0
    val m00 = (tmod and 0x01) != 0
    val mode0 = (if (m10) 2 else 0) or (if (m00) 1 else 0)
    RegRow("Mode", "$mode0 (${timerModeName(mode0)})")
    RegRow("Gate0", if (gate0) "INT1" else "TR0")
    RegRow("C/T0", if (ct0) "Counter" else "Timer")
    val tcon = state.tcon
    RegRow("TR0", if ((tcon and 0x10) != 0) "1" else "0")
    RegRow("TF0", if ((tcon and 0x20) != 0) "1" else "0")

    HorizontalDivider()
    SectionHeader("Timer 1")
    RegRow("TH1", hex(state.th1))
    RegRow("TL1", hex(state.tl1))
    Reg16Row("T1 count", (state.th1 shl 8) or state.tl1)
    val gate1 = (tmod and 0x80) != 0
    val ct1 = (tmod and 0x40) != 0
    val m11 = (tmod and 0x20) != 0
    val m01 = (tmod and 0x10) != 0
    val mode1 = (if (m11) 2 else 0) or (if (m01) 1 else 0)
    RegRow("Mode", "$mode1 (${timerModeName(mode1)})")
    RegRow("Gate1", if (gate1) "INT1" else "TR1")
    RegRow("C/T1", if (ct1) "Counter" else "Timer")
    RegRow("TR1", if ((tcon and 0x40) != 0) "1" else "0")
    RegRow("TF1", if ((tcon and 0x80) != 0) "1" else "0")

    HorizontalDivider()
    SectionHeader("Timer 2")
    RegRow("T2CON", hex(state.t2con))
    RegRow("TH2", hex(state.th2))
    RegRow("TL2", hex(state.tl2))
    RegRow("RCAP2H", hex(state.rcap2h))
    RegRow("RCAP2L", hex(state.rcap2l))
    val t2con = state.t2con
    RegRow("TR2", if ((t2con and 0x04) != 0) "1" else "0")
    RegRow("EXEN2", if ((t2con and 0x08) != 0) "1" else "0")
    RegRow("CP/RL2", if ((t2con and 0x01) != 0) "Capture" else "Reload")
    RegRow("C/T2", if ((t2con and 0x02) != 0) "Counter" else "Timer")
}

@Composable
private fun IntContent(state: EmulatorUiState) {
    SectionHeader("IE (Interrupt Enable)")
    val ie = state.ie
    RegRow("EA", if ((ie and 0x80) != 0) "1 (enabled)" else "0 (disabled)")
    RegRow("ET2", if ((ie and 0x20) != 0) "1" else "0")
    RegRow("ES", if ((ie and 0x10) != 0) "1" else "0")
    RegRow("ET1", if ((ie and 0x08) != 0) "1" else "0")
    RegRow("EX1", if ((ie and 0x04) != 0) "1" else "0")
    RegRow("ET0", if ((ie and 0x02) != 0) "1" else "0")
    RegRow("EX0", if ((ie and 0x01) != 0) "1" else "0")

    HorizontalDivider()
    SectionHeader("IP (Priority)")
    val ip = state.ip
    RegRow("PT2", if ((ip and 0x20) != 0) "High" else "Low")
    RegRow("PS", if ((ip and 0x10) != 0) "High" else "Low")
    RegRow("PT1", if ((ip and 0x08) != 0) "High" else "Low")
    RegRow("PX1", if ((ip and 0x04) != 0) "High" else "Low")
    RegRow("PT0", if ((ip and 0x02) != 0) "High" else "Low")
    RegRow("PX0", if ((ip and 0x01) != 0) "High" else "Low")

    HorizontalDivider()
    SectionHeader("TCON flags")
    val tcon = state.tcon
    RegRow("IE0 (Ext 0)", if ((tcon and 0x01) != 0) "1" else "0")
    RegRow("IE1 (Ext 1)", if ((tcon and 0x04) != 0) "1" else "0")
    RegRow("IT0", if ((tcon and 0x02) != 0) "Edge" else "Level")
    RegRow("IT1", if ((tcon and 0x08) != 0) "Edge" else "Level")
}

@Composable
private fun SerialContent(state: EmulatorUiState) {
    SectionHeader("SCON")
    val scon = state.scon
    val sm0 = (scon and 0x80) != 0
    val sm1 = (scon and 0x40) != 0
    val mode = (if (sm0) 2 else 0) or (if (sm1) 1 else 0)
    RegRow("Mode", "$mode (${serialModeName(mode)})")
    RegRow("SM2", if ((scon and 0x20) != 0) "1" else "0")
    RegRow("REN", if ((scon and 0x10) != 0) "1 (recv)" else "0 (no recv)")
    RegRow("TB8", if ((scon and 0x08) != 0) "1" else "0")
    RegRow("RB8", if ((scon and 0x04) != 0) "1" else "0")
    RegRow("TI", if ((scon and 0x02) != 0) "1" else "0")
    RegRow("RI", if ((scon and 0x01) != 0) "1" else "0")

    HorizontalDivider()
    RegRow("SBUF", hex(state.sbuf))

    HorizontalDivider()
    SectionHeader("Power")
    RegRow("PCON", hex(state.pcon))
    RegRow("SMOD", if ((state.pcon and 0x80) != 0) "1 (2x baud)" else "0")
}

@Composable
private fun RegRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelSmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
    }
}

@Composable
private fun Reg16Row(label: String, value: Int) {
    RegRow(label, "${hex(value, 4)} ($value)")
}

@Composable
private fun SectionHeader(title: String) {
    Spacer(Modifier.height(2.dp))
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

private fun hex(v: Int, digits: Int = 2): String = "0x${v.toString(16).padStart(digits, '0').uppercase()}"

private fun formatRealTime(totalCycles: Long): String {
    val us = totalCycles
    val mins = us / 60_000_000
    val rem1 = us % 60_000_000
    val secs = rem1 / 1_000_000
    val rem2 = rem1 % 1_000_000
    val ms = rem2 / 1000
    val micros = rem2 % 1000
    return buildString {
        append(mins.toString().padStart(2, '0'))
        append(" min ")
        append(secs.toString().padStart(2, '0'))
        append(" s ")
        append(ms.toString().padStart(3, '0'))
        append(" ms ")
        append(micros.toString().padStart(3, '0'))
        append(" µs")
    }
}

private fun formatMhz(actualIps: Long): String {
    if (actualIps == 0L) return "0 MHz"
    val mhz = actualIps.toDouble() * 12.0 / 1_000_000.0
    return when {
        mhz < 1.0 -> {
            val tenThousandths = (mhz * 10000).toInt()
            val whole = tenThousandths / 10000
            val frac = (tenThousandths % 10000).toString().padStart(4, '0')
            "$whole.$frac MHz"
        }
        else -> {
            val hundredths = (mhz * 100).toInt()
            "${hundredths / 100}.${(hundredths % 100).toString().padStart(2, '0')} MHz"
        }
    }
}

private fun timerModeName(mode: Int): String = when (mode) {
    0 -> "13-bit"
    1 -> "16-bit"
    2 -> "8-bit auto-reload"
    3 -> "split (T0 only)"
    else -> "?"
}

private fun serialModeName(mode: Int): String = when (mode) {
    0 -> "8-bit shift"
    1 -> "8-bit UART"
    2 -> "9-bit UART"
    3 -> "9-bit UART (var baud)"
    else -> "?"
}
