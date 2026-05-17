package dev.dominikstahl.emu_8051.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

object CustomIcons {
    val Save: ImageVector by lazy { buildIcon("Save", savePath) }
    val FolderOpen: ImageVector by lazy { buildIcon("FolderOpen", folderOpenPath) }
    val ContentCopy: ImageVector by lazy { buildIcon("ContentCopy", contentCopyPath) }
    val SkipNext: ImageVector by lazy { buildIcon("SkipNext", skipNextPath) }
    val Pause: ImageVector by lazy { buildIcon("Pause", pausePath) }

    private fun buildIcon(name: String, pathData: String) = ImageVector.Builder(
        name = "Custom.$name",
        defaultWidth = 24.0.dp,
        defaultHeight = 24.0.dp,
        viewportWidth = 24.0f,
        viewportHeight = 24.0f
    ).apply {
        addPath(
            addPathNodes(pathData),
            pathFillType = PathFillType.NonZero,
            fill = SolidColor(Color.Black),
            strokeLineWidth = 0.0f
        )
    }.build()

    private const val savePath =
        "M17 3H5c-1.11 0-2 .9-2 2v14c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V7l-4-4zm-5 16c-1.66 0-3-1.34-3-3s1.34-3 3-3 3 1.34 3 3-1.34 3-3 3zm3-10H5V5h10v4z"
    private const val folderOpenPath =
        "M20 6h-8l-2-2H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zm0 12H4V8h16v10z"
    private const val contentCopyPath =
        "M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z"
    private const val skipNextPath =
        "M6 18l8.5-6L6 6v12zM16 6v12h2V6h-2z"
    private const val pausePath =
        "M6 19h4V5H6v14zm8-14v14h4V5h-4z"
}
