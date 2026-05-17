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
    private var tickCount = 0L
    private var pulseStart = 0L
    private var angle = 90

    override fun needsTick() = true

    override fun tick(portVal: Int) {
        tickCount++
        val pinState = (portVal shr config.pin) and 1
        if (pinState == 1 && pulseStart == 0L) {
            pulseStart = tickCount
        } else if (pinState == 0 && pulseStart != 0L) {
            val pulseWidth = (tickCount - pulseStart).toInt()
            angle = ((pulseWidth - 3) * 180 / 100).coerceIn(0, 180)
            pulseStart = 0
        }
    }

    override fun snapshot() = ServoSnapshot(angle)
}

object ServoFactory : HwComponentFactory {
    override val typeId = "SERVO"
    override fun displayName() = "Servo Motor"

    override fun createComponent(config: HwComponentConfig) = ServoComponent(config)

    override fun defaultConfig(id: String) = HwComponentConfig(id, "Servo", typeId, Port.P1, pin = 1)

    override fun labelFor(config: HwComponentConfig) = "${config.port.name}.${config.pin} Servo"

    @Composable
    override fun Render(
        config: HwComponentConfig,
        snapshot: ComponentSnapshot?,
        portValue: Int,
        onUserInput: (HwUserInput) -> Unit,
    ) {
        val angle = (snapshot as? ServoSnapshot)?.angle ?: 90
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ServoArm(angle, Modifier.size(48.dp))
            Text(
                "${angle}°",
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
        val cy = size.height / 2
        val bodyR = cx.coerceAtMost(cy) * 0.4f
        val armLen = bodyR * 1.8f

        drawCircle(color = color.copy(alpha = 0.1f), radius = bodyR)
        drawArc(
            color = color.copy(alpha = 0.25f), startAngle = 0f, sweepAngle = 180f, useCenter = false,
            topLeft = Offset(cx - bodyR, cy - bodyR),
            size = Size(bodyR * 2, bodyR * 2),
            style = Stroke(1f),
        )

        for (tickAngle in listOf(0f, 90f, 180f)) {
            val tickRad = (tickAngle * PI / 180f).toFloat()
            val inner = Offset(cx + sin(tickRad) * bodyR * 0.85f, cy - cos(tickRad) * bodyR * 0.85f)
            val outer = Offset(cx + sin(tickRad) * bodyR * 1.05f, cy - cos(tickRad) * bodyR * 1.05f)
            drawLine(color.copy(alpha = 0.4f), inner, outer, 1f)
        }

        drawCircle(color = color, radius = 4f)
        val angleRad = (angle * PI / 180f).toFloat()
        val end = Offset(cx + sin(angleRad) * armLen, cy - cos(angleRad) * armLen)
        drawLine(color = color, start = Offset(cx, cy), end = end, strokeWidth = 2f)
        drawCircle(color, 3f, end)
    }
}
