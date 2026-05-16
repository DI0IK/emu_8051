package dev.dominikstahl.emu_8051.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PswView(
    psw: Int,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.padding(horizontal = 8.dp, vertical = 2.dp)) {
        Text("PSW", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(
            modifier = Modifier.padding(start = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            FlagIndicator("CY", (psw and 0x80) != 0)
            FlagIndicator("AC", (psw and 0x40) != 0)
            FlagIndicator("F0", (psw and 0x20) != 0)
            FlagIndicator("RS1", (psw and 0x10) != 0)
            FlagIndicator("RS0", (psw and 0x08) != 0)
            FlagIndicator("OV", (psw and 0x04) != 0)
            FlagIndicator("P", (psw and 0x01) != 0)
        }
    }
}
