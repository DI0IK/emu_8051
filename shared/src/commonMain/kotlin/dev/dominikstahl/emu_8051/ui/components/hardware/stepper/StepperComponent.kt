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

data class StepperSnapshot(val positionDegrees: Float) : ComponentSnapshot()

class StepperComponent(config: HwComponentConfig) : HwComponent(config) {
    private var lastPattern = -1
    private var position = 0f

    private val patterns = listOf(
        0b1000, 0b1100, 0b0100, 0b0110,
        0b0010, 0b0011, 0b0001, 0b1001,
    )

    override fun needsTick() = true

    override fun tick(portVal: Int) {
        val pattern = portVal and 0x0F
        if (pattern == lastPattern || pattern == 0 || pattern == 0x0F) return

        val idx = patterns.indexOf(pattern)
        if (idx >= 0 && lastPattern >= 0) {
            val lastIdx = patterns.indexOf(lastPattern)
            if (lastIdx >= 0) {
                var diff = idx - lastIdx
                if (diff > 4) diff -= 8
                else if (diff < -4) diff += 8
                position += diff * 0.45f
            }
        }
        if (idx >= 0) lastPattern = pattern
    }

    override fun snapshot() = StepperSnapshot(position)
}

object StepperFactory : HwComponentFactory {
    override val typeId = "STEPPER"
    override fun displayName() = "Stepper Motor"

    override fun createComponent(config: HwComponentConfig) = StepperComponent(config)

    override fun defaultConfig(id: String) = HwComponentConfig(id, "Stepper", typeId, Port.P1)

    override fun labelFor(config: HwComponentConfig) = "${config.port.name} Stepper"

    @Composable
    override fun Render(
        config: HwComponentConfig,
        snapshot: ComponentSnapshot?,
        portValue: Int,
        onUserInput: (HwUserInput) -> Unit,
    ) {
        val angle = (snapshot as? StepperSnapshot)?.positionDegrees ?: 0f
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                config.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
            StepperRotor(angle, Modifier.size(48.dp))
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
fun StepperRotor(angleDegrees: Float, modifier: Modifier = Modifier) {
    val color = Color(0xFF4488FF)
    Canvas(modifier = modifier) {
        val cx = size.width / 2
        val cy = size.height / 2
        val r = cx.coerceAtMost(cy) * 0.75f

        drawCircle(color = color.copy(alpha = 0.1f), radius = r)

        val coilOff = color.copy(alpha = 0.2f)
        val coilOn = color.copy(alpha = 0.7f)
        val coilPos = listOf(
            Offset(cx, cy - r * 0.85f),
            Offset(cx + r * 0.85f, cy),
            Offset(cx, cy + r * 0.85f),
            Offset(cx - r * 0.85f, cy),
        )
        for ((i, pos) in coilPos.withIndex()) {
            val active = (angleDegrees.toInt() / 45) % 8
            val coilActive = when (i) {
                0 -> active in 0..1 || active == 7
                1 -> active in 1..3
                2 -> active in 3..5
                3 -> active in 5..7
                else -> false
            }
            drawCircle(if (coilActive) coilOn else coilOff, r * 0.08f, pos)
        }

        drawCircle(color = color, radius = 4f)
        val angleRad = (angleDegrees * PI / 180f).toFloat()
        val end = Offset(cx + cos(angleRad) * r, cy + sin(angleRad) * r)
        drawLine(color = color, start = Offset(cx, cy), end = end, strokeWidth = 2f)
    }
}
