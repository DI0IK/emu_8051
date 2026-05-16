package dev.dominikstahl.emu_8051

import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.dominikstahl.emu_8051.ui.components.EmulatorScreen
import dev.dominikstahl.emu_8051.ui.theme.EmulatorTheme

@Composable
fun App() {
    EmulatorTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Surface(
                modifier = Modifier.safeDrawingPadding()
            ) {
                EmulatorScreen()
            }
        }
    }
}