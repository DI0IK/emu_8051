package dev.dominikstahl.emu_8051.ui.components.hardware.buzzer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
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
import dev.dominikstahl.emu_8051.ui.components.hardware.PolarityField
import dev.dominikstahl.emu_8051.ui.components.hardware.PortController
import dev.dominikstahl.emu_8051.ui.components.hardware.PortField

data class BuzzerSnapshot(
    val on: Boolean,
    val frequencyHz: Int = 0,
    val muted: Boolean = false,
) : ComponentSnapshot()

class BuzzerComponent(config: HwComponentConfig) : HwComponent(config) {
    private var lastPinState = -1
    private var lastTransitionCycle = 0L
    private var lastActiveCycle = 0L
    private var frequencyHz = 0
    private var on = false
    private var muted = false

    override fun needsTick() = true

    override fun onUserInput(input: HwUserInput, portCtrl: PortController) {
        if (input is HwUserInput.BuzzerMuteInput && input.compId == config.id) {
            muted = input.muted
        }
    }

    override fun tick(portValues: List<Int>, totalCycles: Long) {
        val portVal = portValues.firstOrNull() ?: return
        val rawBit = (portVal shr config.pin) and 1
        val activeBit = if (config.activeLow) (rawBit xor 1) else rawBit

        if (lastPinState == -1) {
            lastPinState = rawBit
            if (activeBit == 1) {
                on = true
                frequencyHz = 1000
                lastActiveCycle = totalCycles
            }
            return
        }

        if (rawBit != lastPinState) {
            if (totalCycles > 0L && lastTransitionCycle > 0L) {
                val delta = totalCycles - lastTransitionCycle
                if (delta in 40L..25_000L) {
                    frequencyHz = (500_000.0 / delta).toInt().coerceIn(40, 12_000)
                    on = true
                } else {
                    on = activeBit == 1
                    if (on && frequencyHz == 0) frequencyHz = 1000
                }
            } else {
                on = activeBit == 1
                if (on && frequencyHz == 0) frequencyHz = 1000
            }
            lastTransitionCycle = totalCycles
            lastActiveCycle = totalCycles
            lastPinState = rawBit
        } else if (totalCycles > 0L) {
            if (activeBit == 1) {
                if (totalCycles - lastTransitionCycle > 50_000L) {
                    on = true
                    frequencyHz = 1000
                }
                lastActiveCycle = totalCycles
            } else if (on && totalCycles - lastActiveCycle > 50_000L) {
                on = false
                frequencyHz = 0
            }
        }
    }

    override fun reset() {
        lastPinState = -1
        lastTransitionCycle = 0L
        lastActiveCycle = 0L
        frequencyHz = 0
        on = false
    }

    override fun snapshot() = BuzzerSnapshot(on = on, frequencyHz = frequencyHz, muted = muted)
}

object BuzzerFactory : HwComponentFactory {
    override val typeId = "BUZZER"
    override fun displayName() = "Buzzer"

    override fun createComponent(config: HwComponentConfig) = BuzzerComponent(config)

    override fun defaultConfig(id: String) = HwComponentConfig(id, "P1.0 Buzzer", typeId, ports = listOf(Port.P1), pin = 0)

    override fun labelFor(config: HwComponentConfig) = "${config.port.name}.${config.pin} Buzzer"

    override fun needsTick(config: HwComponentConfig) = true

    override fun usedPins(config: HwComponentConfig) = listOf(config.port to config.pin)

    @Composable
    override fun Render(
        config: HwComponentConfig,
        snapshot: ComponentSnapshot?,
        portValues: List<Int>,
        onUserInput: (HwUserInput) -> Unit,
    ) {
        val snap = snapshot as? BuzzerSnapshot
        val on = snap?.on ?: false
        val freq = snap?.frequencyHz ?: 0
        val muted = snap?.muted ?: false

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BuzzerIcon(on = on && !muted, muted = muted, modifier = Modifier.size(36.dp))
            Text(
                if (on && freq > 0) "${config.label} (${freq} Hz)" else config.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .clickable {
                        onUserInput(HwUserInput.BuzzerMuteInput(config.id, !muted))
                    },
            ) {
                Text(
                    if (muted) "Muted" else "Sound On",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (muted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
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
        PolarityField(config, onUpdateComponent)
    }
}

@Composable
fun BuzzerIcon(on: Boolean, muted: Boolean = false, modifier: Modifier = Modifier) {
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

        if (on && !muted) {
            drawArc(
                color, -40f, 80f, false,
                Offset(cx + r * 0.4f, cy - r * 0.5f),
                Size(r * 0.5f, r * 0.5f),
                style = Stroke(2f),
            )
            drawArc(
                color, -40f, 80f, false,
                Offset(cx + r * 0.75f, cy - r * 0.75f),
                Size(r * 0.75f, r * 0.75f),
                style = Stroke(2f),
            )
        } else if (muted) {
            drawLine(Color(0xFFEF9A9A), Offset(cx + r * 0.55f, cy - r * 0.35f), Offset(cx + r * 1.1f, cy + r * 0.35f), 2f)
            drawLine(Color(0xFFEF9A9A), Offset(cx + r * 1.1f, cy - r * 0.35f), Offset(cx + r * 0.55f, cy + r * 0.35f), 2f)
        } else {
            drawLine(color, Offset(cx + r * 0.85f, cy - r * 0.4f), Offset(cx + r * 1.1f, cy + r * 0.4f), 2f)
        }
    }
}
