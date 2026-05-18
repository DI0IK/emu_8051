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
import dev.dominikstahl.emu_8051.ui.components.hardware.PortFields

class LedMatrixComponent(config: HwComponentConfig) : HwComponent(config) {
    companion object {
        private const val MAX_BRIGHTNESS = 8192
        private const val DECAY = 1
        private const val THRESHOLD = 1
    }

    // 8x8 display buffer with brightness values (0-255)
    private val displayBuffer = Array(8) { IntArray(8) }
    private var lastRowPattern = 0xFF

    override fun needsTick() = true

    override fun tick(portValues: List<Int>) {
        val rowVal = portValues.getOrNull(0) ?: return
        val colVal = portValues.getOrNull(1) ?: return

        for (r in 0 until 8) {
            for (c in 0 until 8) {
                displayBuffer[r][c] = maxOf(0, displayBuffer[r][c] - DECAY)
            }
        }

        val activeRow = findActiveRow(rowVal)

        if (activeRow >= 0 && activeRow < 8) {
            for (col in 0 until 8) {
                if ((colVal and (1 shl col)) != 0) {
                    displayBuffer[activeRow][col] = MAX_BRIGHTNESS
                }
            }
        }

        lastRowPattern = rowVal
    }

    /**
     * Find which row is active by detecting a single high bit.
     * Returns -1 if no single row is detected.
     */
    private fun findActiveRow(rowVal: Int): Int {
        var highBitCount = 0
        var activeBit = -1
        for (i in 0 until 8) {
            if ((rowVal and (1 shl i)) != 0) {
                highBitCount++
                activeBit = i
            }
        }
        return if (highBitCount == 1) activeBit else -1
    }

    override fun reset() {
        for (r in 0 until 8) {
            for (c in 0 until 8) {
                displayBuffer[r][c] = 0
            }
        }
        lastRowPattern = 0x00
    }

    override fun snapshot(): ComponentSnapshot {
        val data = (0 until 8).map { r ->
            (0 until 8).map { c ->
                displayBuffer[r][c] >= THRESHOLD
            }
        }
        return LedMatrixSnapshot(data)
    }

    fun getDisplayBuffer(): Array<IntArray> = displayBuffer
    fun getThreshold(): Int = THRESHOLD
}

data class LedMatrixSnapshot(val data: List<List<Boolean>>) : ComponentSnapshot()

object LedMatrixFactory : HwComponentFactory {
    override val typeId = "LED_MATRIX"
    override fun displayName() = "LED Matrix"

    override fun createComponent(config: HwComponentConfig): HwComponent = LedMatrixComponent(config)

    override fun defaultConfig(id: String) = HwComponentConfig(
        id = id,
        label = "P1/P2 LED Matrix",
        type = typeId,
        ports = listOf(Port.P1, Port.P2)
    )

    override fun labelFor(config: HwComponentConfig): String {
        val rowPort = config.ports.getOrNull(0)?.name ?: "P1"
        val colPort = config.ports.getOrNull(1)?.name ?: "P2"
        return "$rowPort/$colPort Matrix"
    }

    override fun needsTick(config: HwComponentConfig) = true

    override fun portCount(config: HwComponentConfig) = 2

    @Composable
    override fun Render(
        config: HwComponentConfig,
        snapshot: ComponentSnapshot?,
        portValues: List<Int>,
        onUserInput: (HwUserInput) -> Unit,
    ) {
        val s = snapshot as? LedMatrixSnapshot
        val rowPort = config.ports.getOrNull(0)?.name ?: "P1"
        val colPort = config.ports.getOrNull(1)?.name ?: "P2"

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "$rowPort/$colPort Matrix",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            LedMatrixDisplay(
                data = s?.data ?: List(8) { List(8) { false } },
                color = Color(0xFFFF4444)
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
            onUpdateComponent = onUpdateComponent
        )
    }
}
