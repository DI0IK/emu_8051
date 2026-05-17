package dev.dominikstahl.emu_8051.platform

expect fun copyToClipboard(text: String)

expect suspend fun pasteFromClipboard(): String?

expect fun platformShare(text: String, title: String)

expect val hasPlatformShare: Boolean
