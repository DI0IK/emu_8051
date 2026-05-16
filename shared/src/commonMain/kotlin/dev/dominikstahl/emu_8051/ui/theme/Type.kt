package dev.dominikstahl.emu_8051.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

val MonospaceStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    letterSpacing = 0.sp
)

val EmulatorTypography = Typography(
    bodyLarge = TextStyle(fontSize = 16.sp, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, letterSpacing = 0.25.sp),
    bodySmall = TextStyle(fontSize = 12.sp, letterSpacing = 0.4.sp),
    labelLarge = MonospaceStyle.copy(fontSize = 15.sp),
    labelMedium = MonospaceStyle.copy(fontSize = 13.sp),
    labelSmall = MonospaceStyle.copy(fontSize = 11.sp),
    titleSmall = TextStyle(fontSize = 13.sp, letterSpacing = 0.1.sp),
)
