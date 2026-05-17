package dev.dominikstahl.emu_8051.platform

import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection

actual fun copyToClipboard(text: String) {
    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
    clipboard.setContents(StringSelection(text), null)
}

actual suspend fun pasteFromClipboard(): String? {
    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
    return try {
        clipboard.getContents(null)?.getTransferData(DataFlavor.stringFlavor) as? String
    } catch (_: Exception) {
        null
    }
}

actual fun platformShare(text: String, title: String) {}

actual val hasPlatformShare: Boolean = false
