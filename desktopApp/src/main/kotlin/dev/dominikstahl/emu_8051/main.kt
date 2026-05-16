package dev.dominikstahl.emu_8051

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "emu_8051"
    ) {
        App()
    }
}