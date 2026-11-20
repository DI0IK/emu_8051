package dev.dominikstahl.emu_8051.ui.components.hardware.ledmatrix

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import dev.dominikstahl.emu_8051.ui.HwComponentConfig
import dev.dominikstahl.emu_8051.ui.Port
import dev.dominikstahl.emu_8051.ui.components.hardware.ComponentSnapshot
import dev.dominikstahl.emu_8051.ui.components.hardware.HwComponent
import dev.dominikstahl.emu_8051.ui.components.hardware.HwComponentFactory
import dev.dominikstahl.emu_8051.ui.components.hardware.HwUserInput
import dev.dominikstahl.emu_8051.ui.components.hardware.PolarityField
import dev.dominikstahl.emu_8051.ui.components.hardware.PortFields

data class LedMatrixSnapshot(val brightness: List<List<Float>>) : ComponentSnapshot()

class LedMatrixComponent(config: HwComponentConfig) : HwComponent(config) {
    private val displayBuffer = Array(8) { FloatArray(8) }
    private var lastTickCycles = 0L

    override fun needsTick() = true

    override fun tick(portValues: List<Int>, totalCycles: Long) {
        val rawRow = portValues.getOrNull(0) ?: return
        val rawCol = portValues.getOrNull(1) ?: return

        val rowVal = if (config.activeLow) (rawRow xor 0xFF) and 0xFF else rawRow and 0xFF
        val colVal = if (config.activeLow) (rawCol xor 0xFF) and 0xFF else rawCol and 0xFF

        val decay = if (totalCycles > 0L) {
            val delta = (totalCycles - lastTickCycles).coerceAtLeast(0L)
            lastTickCycles = totalCycles
            delta / 40_000f
        } else {
            0.002f
        }

        if (decay > 0f) {
            for (r in 0 until 8) {
                for (c in 0 until 8) {
                    if (displayBuffer[r][c] > 0f) {
                        displayBuffer[r][c] = (displayBuffer[r][c] - decay).coerceAtLeast(0f)
                    }
                }
            }
        }

        if (rowVal != 0 && colVal != 0) {
            for (r in 0 until 8) {
                if ((rowVal and (1 shl r)) != 0) {
                    for (c in 0 until 8) {
                        if ((colVal and (1 shl c)) != 0) {
                            displayBuffer[r][c] = 1f
                        }
                    }
                }
            }
        }
    }

    override fun reset() {
        for (r in 0 until 8) displayBuffer[r].fill(0f)
        lastTickCycles = 0L
    }

    override fun snapshot(): ComponentSnapshot =
        LedMatrixSnapshot((0 until 8).map { r -> displayBuffer[r].toList() })
}

object LedMatrixFactory : HwComponentFactory {
    override val typeId = "LED_MATRIX"
    override fun displayName() = "LED Matrix"

    override fun createComponent(config: HwComponentConfig): HwComponent = LedMatrixComponent(config)

    override fun defaultConfig(id: String) = HwComponentConfig(
        id = id,
        label = "P1/P2 Matrix",
        type = typeId,
        ports = listOf(Port.P1, Port.P2),
    )

    override fun labelFor(config: HwComponentConfig): String {
        val rowPort = config.ports.getOrNull(0)?.name ?: "P1"
        val colPort = config.ports.getOrNull(1)?.name ?: "P2"
        return "$rowPort/$colPort Matrix"
    }

    override fun needsTick(config: HwComponentConfig) = true

    override fun portCount(config: HwComponentConfig) = 2

    override fun portLabels(config: HwComponentConfig) = listOf("Row Port", "Col Port")

    override fun usedPins(config: HwComponentConfig): List<Pair<Port, Int>> {
        val rowPort = config.ports.getOrNull(0) ?: Port.P1
        val colPort = config.ports.getOrNull(1) ?: Port.P2
        return (0..7).map { rowPort to it } + (0..7).map { colPort to it }
    }

    @Composable
    override fun Render(
        config: HwComponentConfig,
        snapshot: ComponentSnapshot?,
        portValues: List<Int>,
        onUserInput: (HwUserInput) -> Unit,
    ) {
        val s = snapshot as? LedMatrixSnapshot

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                labelFor(config),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            LedMatrixDisplay(
                brightness = s?.brightness ?: List(8) { List(8) { 0f } },
                color = Color(0xFFFF4444),
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun ConfigFields(
        config: HwComponentConfig,
        onUpdateComponent: (String, (HwComponentConfig) -> HwComponentConfig) -> Unit,
    ) {
        PortFields(
            portCount = 2,
            config = config,
            onUpdateComponent = onUpdateComponent,
            labels = portLabels(config),
        )
        PolarityField(config, onUpdateComponent)
    }
}
