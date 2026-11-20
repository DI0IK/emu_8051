package dev.dominikstahl.emu_8051.ui.components.hardware.stepper

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.dominikstahl.emu_8051.ui.HwComponentConfig
import dev.dominikstahl.emu_8051.ui.Port
import dev.dominikstahl.emu_8051.ui.components.hardware.ComponentSnapshot
import dev.dominikstahl.emu_8051.ui.components.hardware.HwComponent
import dev.dominikstahl.emu_8051.ui.components.hardware.HwComponentFactory
import dev.dominikstahl.emu_8051.ui.components.hardware.HwUserInput
import dev.dominikstahl.emu_8051.ui.components.hardware.PortField
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class StepperSnapshot(
    val positionDegrees: Float,
    val coilBits: Int = 0,
) : ComponentSnapshot()

class StepperComponent(config: HwComponentConfig) : HwComponent(config) {
    private var lastPattern = -1
    private var position = 0f
    private var coilBits = 0

    // Half-step sequence on P.0..P.3 (1 = A, 2 = B, 4 = C, 8 = D)
    private val patterns = listOf(
        0b0001, 0b0011, 0b0010, 0b0110,
        0b0100, 0b1100, 0b1000, 0b1001,
    )

    override fun needsTick() = true

    override fun tick(portValues: List<Int>, totalCycles: Long) {
        val portVal = portValues.firstOrNull() ?: return
        val pattern = portVal and 0x0F
        coilBits = pattern
        if (pattern == lastPattern || pattern == 0 || pattern == 0x0F) return

        val idx = patterns.indexOf(pattern)
        if (idx >= 0 && lastPattern >= 0) {
            val lastIdx = patterns.indexOf(lastPattern)
            if (lastIdx >= 0) {
                var diff = idx - lastIdx
                if (diff > 4) diff -= 8
                else if (diff < -4) diff += 8
                // 64 half-steps / 32 full steps per visual revolution (5.625 deg/half-step)
                position += diff * 5.625f
            }
        }
        if (idx >= 0) lastPattern = pattern
    }

    override fun reset() {
        lastPattern = -1
        position = 0f
        coilBits = 0
    }

    override fun snapshot() = StepperSnapshot(position, coilBits)
}

object StepperFactory : HwComponentFactory {
    override val typeId = "STEPPER"
    override fun displayName() = "Stepper Motor"

    override fun createComponent(config: HwComponentConfig) = StepperComponent(config)

    override fun defaultConfig(id: String) = HwComponentConfig(id, "P1 Stepper", typeId, ports = listOf(Port.P1))

    override fun labelFor(config: HwComponentConfig) = "${config.port.name} Stepper"

    override fun needsTick(config: HwComponentConfig) = true

    override fun usedPins(config: HwComponentConfig) = (0..3).map { config.port to it }

    @Composable
    override fun Render(
        config: HwComponentConfig,
        snapshot: ComponentSnapshot?,
        portValues: List<Int>,
        onUserInput: (HwUserInput) -> Unit,
    ) {
        val snap = snapshot as? StepperSnapshot
        val angle = snap?.positionDegrees ?: 0f
        val coils = snap?.coilBits ?: 0
        val normAngle = ((angle % 360f) + 360f) % 360f

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${config.label} (${normAngle.toInt()}°)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
            StepperRotor(angle, coils, Modifier.size(52.dp))
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun ConfigFields(
        config: HwComponentConfig,
        onUpdateComponent: (String, (HwComponentConfig) -> HwComponentConfig) -> Unit,
    ) {
        PortField(config, onUpdateComponent)
    }
}

@Composable
fun StepperRotor(angleDegrees: Float, coilBits: Int = 0, modifier: Modifier = Modifier) {
    val color = Color(0xFF4488FF)
    Canvas(modifier = modifier) {
        val cx = size.width / 2
        val cy = size.height / 2
        val r = cx.coerceAtMost(cy) * 0.78f

        drawCircle(color = color.copy(alpha = 0.1f), radius = r)

        val coilOff = color.copy(alpha = 0.2f)
        val coilOn = Color(0xFFFFAA00)
        // Coils A, B, C, D at top (0°), right (90°), bottom (180°), left (270°)
        val coilPos = listOf(
            Offset(cx, cy - r * 0.85f),
            Offset(cx + r * 0.85f, cy),
            Offset(cx, cy + r * 0.85f),
            Offset(cx - r * 0.85f, cy),
        )
        for ((i, pos) in coilPos.withIndex()) {
            val active = (coilBits and (1 shl i)) != 0
            drawCircle(if (active) coilOn else coilOff, r * 0.12f, pos)
        }

        drawCircle(color = color, radius = 4f)
        // 0° points straight up (12 o'clock) matching coil A
        val angleRad = (angleDegrees * PI / 180f).toFloat()
        val end = Offset(cx + sin(angleRad) * r * 0.75f, cy - cos(angleRad) * r * 0.75f)
        drawLine(color = color, start = Offset(cx, cy), end = end, strokeWidth = 2.5f)
    }
}
