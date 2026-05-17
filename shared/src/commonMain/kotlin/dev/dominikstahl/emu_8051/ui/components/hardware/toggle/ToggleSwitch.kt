package dev.dominikstahl.emu_8051.ui.components.hardware.toggle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun ToggleSwitch(
    on: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val baseColor = MaterialTheme.colorScheme.surfaceVariant
    val borderColor = MaterialTheme.colorScheme.outline
    val activeColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(width = 20.dp, height = 34.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(baseColor)
                .border(1.dp, borderColor.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                .clickable(onClick = onClick),
        ) {
            Box(
                modifier = Modifier
                    .size(width = 16.dp, height = 14.dp)
                    .align(if (on) Alignment.TopCenter else Alignment.BottomCenter)
                    .padding(vertical = 2.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (on) activeColor else borderColor),
            )
        }
    }
}
