package dev.dominikstahl.emu_8051.platform

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent

actual fun copyToClipboard(text: String) {
    if (!AndroidContext.isInitialized()) return
    val manager = AndroidContext.appContext.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    manager.setPrimaryClip(ClipData.newPlainText("emu_8051", text))
}

actual suspend fun pasteFromClipboard(): String? {
    if (!AndroidContext.isInitialized()) return null
    val manager = AndroidContext.appContext.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
    val clip = manager.primaryClip ?: return null
    if (clip.itemCount == 0) return null
    return clip.getItemAt(0)?.text?.toString()
}

actual fun platformShare(text: String, title: String) {
    if (!AndroidContext.isInitialized()) return
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        putExtra(Intent.EXTRA_SUBJECT, title)
    }
    val chooser = Intent.createChooser(intent, title).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    AndroidContext.appContext.startActivity(chooser)
}

actual val hasPlatformShare: Boolean = true
