package dev.dominikstahl.emu_8051.platform

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

actual fun copyToClipboard(text: String) {
    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
    clipboard.setContents(StringSelection(text), null)
}

actual fun platformShare(text: String, title: String) {}

actual val hasPlatformShare: Boolean = false
