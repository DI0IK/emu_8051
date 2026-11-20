package dev.dominikstahl.emu_8051.ui.components.hardware.servo

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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.dominikstahl.emu_8051.ui.HwComponentConfig
import dev.dominikstahl.emu_8051.ui.Port
import dev.dominikstahl.emu_8051.ui.components.hardware.ComponentSnapshot
import dev.dominikstahl.emu_8051.ui.components.hardware.HwComponent
import dev.dominikstahl.emu_8051.ui.components.hardware.HwComponentFactory
import dev.dominikstahl.emu_8051.ui.components.hardware.HwUserInput
import dev.dominikstahl.emu_8051.ui.components.hardware.PinField
import dev.dominikstahl.emu_8051.ui.components.hardware.PortField
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class ServoSnapshot(val angle: Int) : ComponentSnapshot()

class ServoComponent(config: HwComponentConfig) : HwComponent(config) {
    private var prevPin = -1
    private var pulseStartCycle = -1L
    private var angle = 90

    override fun needsTick() = true

    override fun tick(portValues: List<Int>, totalCycles: Long) {
        val portVal = portValues.firstOrNull() ?: return
        val pinState = (portVal shr config.pin) and 1
        if (prevPin == -1) {
            prevPin = pinState
            return
        }
        if (prevPin == 0 && pinState == 1) {
            pulseStartCycle = totalCycles
        } else if (prevPin == 1 && pinState == 0 && pulseStartCycle >= 0L) {
            val pulseWidthUs = (totalCycles - pulseStartCycle).toInt()
            if (pulseWidthUs in 500..2500) {
                angle = ((pulseWidthUs - 1000) * 180 / 1000).coerceIn(0, 180)
            }
            pulseStartCycle = -1L
        }
        prevPin = pinState
    }

    override fun reset() {
        prevPin = -1
        pulseStartCycle = -1L
        angle = 90
    }

    override fun snapshot() = ServoSnapshot(angle)
}

object ServoFactory : HwComponentFactory {
    override val typeId = "SERVO"
    override fun displayName() = "Servo Motor"

    override fun createComponent(config: HwComponentConfig) = ServoComponent(config)

    override fun defaultConfig(id: String) = HwComponentConfig(id, "P1.1 Servo", typeId, ports = listOf(Port.P1), pin = 1)

    override fun labelFor(config: HwComponentConfig) = "${config.port.name}.${config.pin} Servo"

    override fun needsTick(config: HwComponentConfig) = true

    override fun usedPins(config: HwComponentConfig) = listOf(config.port to config.pin)

    @Composable
    override fun Render(
        config: HwComponentConfig,
        snapshot: ComponentSnapshot?,
        portValues: List<Int>,
        onUserInput: (HwUserInput) -> Unit,
    ) {
        val angle = (snapshot as? ServoSnapshot)?.angle ?: 90
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ServoArm(angle, Modifier.size(56.dp))
            Text(
                "${config.label}: ${angle}°",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun ConfigFields(
        config: HwComponentConfig,
        onUpdateComponent: (String, (HwComponentConfig) -> HwComponentConfig) -> Unit,
    ) {
        PortField(config, onUpdateComponent)
        PinField(config) { newPin ->
            onUpdateComponent(config.id) { it.copy(pin = newPin) }
        }
    }
}

@Composable
fun ServoArm(angle: Int, modifier: Modifier = Modifier) {
    val color = Color(0xFF44BB44)
    Canvas(modifier = modifier) {
        val cx = size.width / 2
        val cy = size.height * 0.65f
        val bodyR = (size.width / 2).coerceAtMost(size.height * 0.55f) * 0.55f
        val armLen = bodyR * 1.45f

        drawCircle(color = color.copy(alpha = 0.1f), radius = bodyR, center = Offset(cx, cy))
        // Upper semicircle arc: 180° (9 o'clock) sweeping 180° to 360° (3 o'clock) via top
        drawArc(
            color = color.copy(alpha = 0.3f),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(cx - bodyR, cy - bodyR),
            size = Size(bodyR * 2, bodyR * 2),
            style = Stroke(1.5f),
        )

        for (tickAngle in listOf(0f, 90f, 180f)) {
            val tickRad = (tickAngle * PI / 180f).toFloat()
            val inner = Offset(cx - cos(tickRad) * bodyR * 0.85f, cy - sin(tickRad) * bodyR * 0.85f)
            val outer = Offset(cx - cos(tickRad) * bodyR * 1.1f, cy - sin(tickRad) * bodyR * 1.1f)
            drawLine(color.copy(alpha = 0.5f), inner, outer, 1.5f)
        }

        drawCircle(color = color, radius = 4f, center = Offset(cx, cy))
        val angleRad = (angle.coerceIn(0, 180) * PI / 180f).toFloat()
        val end = Offset(cx - cos(angleRad) * armLen, cy - sin(angleRad) * armLen)
        drawLine(color = color, start = Offset(cx, cy), end = end, strokeWidth = 2.5f)
        drawCircle(color, 3.5f, end)
    }
}
