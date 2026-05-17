package dev.dominikstahl.emu_8051.platform

import kotlinx.browser.window
import kotlinx.coroutines.await

actual fun copyToClipboard(text: String) {
    js("navigator.clipboard.writeText(text)")
}

actual suspend fun pasteFromClipboard(): String? {
    return try {
        window.navigator.clipboard.readText().await()
    } catch (_: Exception) {
        null
    }
}

actual fun platformShare(text: String, title: String) {}

actual val hasPlatformShare: Boolean = false
