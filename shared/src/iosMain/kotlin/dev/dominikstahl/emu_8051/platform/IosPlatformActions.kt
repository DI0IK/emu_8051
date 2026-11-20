package dev.dominikstahl.emu_8051.platform

import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIPasteboard
import platform.UIKit.popoverPresentationController

actual fun copyToClipboard(text: String) {
    UIPasteboard.generalPasteboard.string = text
}

actual suspend fun pasteFromClipboard(): String? {
    return UIPasteboard.generalPasteboard.string
}

actual fun platformShare(text: String, title: String) {
    val activityVC = UIActivityViewController(
        activityItems = listOf(text),
        applicationActivities = null,
    )
    val rootVC = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
    activityVC.popoverPresentationController?.apply {
        sourceView = rootVC.view
        sourceRect = rootVC.view.bounds
        permittedArrowDirections = 0u
    }
    rootVC.presentViewController(activityVC, animated = true, completion = null)
}

actual val hasPlatformShare: Boolean = true
