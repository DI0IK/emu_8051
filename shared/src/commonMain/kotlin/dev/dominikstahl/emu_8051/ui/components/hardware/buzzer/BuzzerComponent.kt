package dev.dominikstahl.emu_8051.ui.components.hardware.buzzer

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
import androidx.compose.ui.graphics.Path
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

data class BuzzerSnapshot(val on: Boolean) : ComponentSnapshot()

class BuzzerComponent(config: HwComponentConfig) : HwComponent(config) {
    private var on = false

    override fun needsTick() = true

    override fun tick(portValues: List<Int>) {
        val portVal = portValues.firstOrNull() ?: return
        on = (portVal and (1 shl config.pin)) != 0
    }

    override fun snapshot() = BuzzerSnapshot(on)
}

object BuzzerFactory : HwComponentFactory {
    override val typeId = "BUZZER"
    override fun displayName() = "Buzzer"

    override fun createComponent(config: HwComponentConfig) = BuzzerComponent(config)

    override fun defaultConfig(id: String) = HwComponentConfig(id, "Buzzer", typeId, ports = listOf(Port.P1), pin = 0)

    override fun labelFor(config: HwComponentConfig) = "${config.port.name}.${config.pin} Buzzer"

    @Composable
    override fun Render(
        config: HwComponentConfig,
        snapshot: ComponentSnapshot?,
        portValues: List<Int>,
        onUserInput: (HwUserInput) -> Unit,
    ) {
        val on = (snapshot as? BuzzerSnapshot)?.on ?: false
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BuzzerIcon(on, Modifier.size(36.dp))
            Text(
                config.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
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
fun BuzzerIcon(on: Boolean, modifier: Modifier = Modifier) {
    val active = Color(0xFFFFAA00)
    val inactive = Color(0xFF555555)
    val color = if (on) active else inactive
    Canvas(modifier = modifier) {
        val cx = size.width / 2
        val cy = size.height / 2
        val r = cx.coerceAtMost(cy) * 0.6f

        val speaker = Path().apply {
            moveTo(cx + r * 0.3f, cy - r * 0.6f)
            lineTo(cx - r * 0.3f, cy - r * 0.25f)
            lineTo(cx - r * 0.3f, cy + r * 0.25f)
            lineTo(cx + r * 0.3f, cy + r * 0.6f)
            close()
        }
        drawPath(speaker, color)

        if (on) {
            drawArc(color, -40f, 80f, false,
                Offset(cx + r * 0.4f, cy - r * 0.5f),
                Size(r * 0.5f, r * 0.5f),
                style = Stroke(2f),
            )
            drawArc(color, -40f, 80f, false,
                Offset(cx + r * 0.75f, cy - r * 0.75f),
                Size(r * 0.75f, r * 0.75f),
                style = Stroke(2f),
            )
        } else {
            drawLine(color, Offset(cx + r * 0.85f, cy - r * 0.4f), Offset(cx + r * 1.1f, cy + r * 0.4f), 2f)
        }
    }
}
