package dev.dominikstahl.emu_8051.platform

actual fun copyToClipboard(text: String) {
    js("navigator.clipboard.writeText(text)")
}

actual fun platformShare(text: String, title: String) {}

actual val hasPlatformShare: Boolean = false
