@file:OptIn(ExperimentalWasmJsInterop::class)

package dev.dominikstahl.emu_8051.platform

import kotlinx.browser.window
import kotlinx.coroutines.await

actual fun copyToClipboard(text: String) {
    jsCopyToClipboard(text)
}

private fun jsCopyToClipboard(text: String): Unit = js(
    "(function() { try { if (navigator && navigator.clipboard && navigator.clipboard.writeText) { navigator.clipboard.writeText(text).catch(function(){}); } } catch (e) {} })()"
)

actual suspend fun pasteFromClipboard(): String? {
    return try {
        window.navigator.clipboard.readText().await()
    } catch (_: Exception) {
        null
    }
}

actual fun platformShare(text: String, title: String) {}

actual val hasPlatformShare: Boolean = false
