package dev.dominikstahl.emu_8051.platform

import platform.UIKit.UIApplication
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIPasteboard

actual fun copyToClipboard(text: String) {
    UIPasteboard.generalPasteboard.string = text
}

actual fun platformShare(text: String, title: String) {
    val activityVC = UIActivityViewController(
        activityItems = listOf(text),
        applicationActivities = null
    )
    val rootVC = UIApplication.sharedApplication.keyWindow?.rootViewController
    rootVC?.presentViewController(activityVC, animated = true, completion = null)
}

actual val hasPlatformShare: Boolean = true
