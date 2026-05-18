package dev.dominikstahl.emu_8051.ui.components.hardware.ledmatrix

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import dev.dominikstahl.emu_8051.ui.HwComponentConfig
import dev.dominikstahl.emu_8051.ui.Port
import dev.dominikstahl.emu_8051.ui.components.hardware.ComponentSnapshot
import dev.dominikstahl.emu_8051.ui.components.hardware.HwComponent
import dev.dominikstahl.emu_8051.ui.components.hardware.HwComponentFactory
import dev.dominikstahl.emu_8051.ui.components.hardware.HwUserInput
import dev.dominikstahl.emu_8051.ui.components.hardware.PortFields
import kotlin.math.max

/** Snapshot of LED Matrix state - captures which LEDs are on/off with persistence */
data class LedMatrixSnapshot(
    val ledStates: IntArray,  // 64 bits packed into 2 ints: [rows 0-3][rows 4-7]
) : ComponentSnapshot() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LedMatrixSnapshot) return false
        return ledStates.contentEquals(other.ledStates)
    }

    override fun hashCode(): Int {
        return ledStates.contentHashCode()
    }
}

class LedMatrixComponent(config: HwComponentConfig) : HwComponent(config) {
    private var lastRowValue = 0
    private var lastDataValue = 0
    private val ledStates = IntArray(2)  // 64 bits: 8 bits per row
    
    // LED persistence tracking - track how many frames since each LED was lit
    private val ledFramesSinceLit = IntArray(64)  // Increment each frame, reset when LED is active
    private val PERSISTENCE_FRAMES = 3  // Keep LED visible for 3 frames (~50ms at 60Hz)

    override fun needsTick() = true

    override fun tick(portVal: Int) {
        // This receives P1 (row selector)
        // We need P2 (data) too - but tick only gets one value
        // So we'll capture state differently
    }

    fun setMatrixState(rowValue: Int, dataValue: Int) {
        lastRowValue = rowValue
        lastDataValue = dataValue
        updateLedStates(rowValue, dataValue)
    }
    
    private fun updateLedStates(rowValue: Int, dataValue: Int) {
        // Increment frame counter for all LEDs
        for (i in ledFramesSinceLit.indices) {
            ledFramesSinceLit[i]++
        }
        
        // For active LEDs (currently selected row with data on), reset their frame counter
        for (row in 0..7) {
            val rowOn = (rowValue and (1 shl row)) == 0  // Active LOW
            if (rowOn) {
                for (col in 0..7) {
                    val colOn = (dataValue and (1 shl col)) != 0  // Active HIGH
                    if (colOn) {
                        val ledIndex = row * 8 + col
                        ledFramesSinceLit[ledIndex] = 0  // Just lit, reset counter
                    }
                }
            }
        }
        
        // Update LED state array based on persistence
        for (row in 0..7) {
            var rowBits = 0
            for (col in 0..7) {
                val ledIndex = row * 8 + col
                if (ledFramesSinceLit[ledIndex] < PERSISTENCE_FRAMES) {
                    rowBits = rowBits or (1 shl col)
                }
            }
            val wordIdx = row / 4
            val bitOffset = (row % 4) * 8
            ledStates[wordIdx] = (ledStates[wordIdx] and (0xFF shl bitOffset).inv()) or (rowBits shl bitOffset)
        }
    }

    override fun snapshot(): ComponentSnapshot = LedMatrixSnapshot(ledStates.copyOf())

    override fun reset() {
        lastRowValue = 0xFF  // All rows inactive
        lastDataValue = 0x00
        ledStates.fill(0)
        ledFramesSinceLit.fill(0)
    }
}

object LedMatrixFactory : HwComponentFactory {
    override val typeId = "LED_MATRIX"
    override fun displayName() = "LED Matrix"

    override fun createComponent(config: HwComponentConfig): HwComponent = LedMatrixComponent(config)

    override fun defaultConfig(id: String) = HwComponentConfig(
        id = id,
        label = "P1/P2 LED Matrix",
        type = typeId,
        ports = listOf(Port.P1, Port.P2),
    )

    override fun labelFor(config: HwComponentConfig) = buildString {
        if (config.ports.size >= 2) {
            append(config.ports[0].name)
            append("/")
            append(config.ports[1].name)
        } else {
            append(config.port.name)
        }
        append(" LED Matrix")
    }

    override fun portCount(config: HwComponentConfig) = 2

    override fun needsTick(config: HwComponentConfig) = true

    @Composable
    override fun Render(
        config: HwComponentConfig,
        snapshot: ComponentSnapshot?,
        portValues: List<Int>,
        onUserInput: (HwUserInput) -> Unit,
    ) {
        val rowPortValue = portValues.getOrNull(0) ?: 0xFF
        val dataPortValue = portValues.getOrNull(1) ?: 0x00

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (config.ports.size >= 2) "${config.ports[0].name}/${config.ports[1].name}"
                else config.port.name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LedMatrix(
                rowPortValue = rowPortValue,
                dataPortValue = dataPortValue,
                snapshot = snapshot as? LedMatrixSnapshot,
                color = Color(0xFFFF4444)
            )
        }
    }

    @Composable
    override fun ConfigFields(
        config: HwComponentConfig,
        onUpdateComponent: (String, (HwComponentConfig) -> HwComponentConfig) -> Unit,
    ) {
        PortFields(portCount = 2, config = config, onUpdateComponent = onUpdateComponent)
    }
}


