package dev.dominikstahl.emu_8051.ui.components.hardware.sevenseg

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.dominikstahl.emu_8051.ui.HwComponentConfig
import dev.dominikstahl.emu_8051.ui.Port
import dev.dominikstahl.emu_8051.ui.components.hardware.ComponentSnapshot
import dev.dominikstahl.emu_8051.ui.components.hardware.HwComponent
import dev.dominikstahl.emu_8051.ui.components.hardware.HwComponentFactory
import dev.dominikstahl.emu_8051.ui.components.hardware.HwUserInput
import dev.dominikstahl.emu_8051.ui.components.hardware.PolarityField
import dev.dominikstahl.emu_8051.ui.components.hardware.PortFields

data class MultiSevenSegSnapshot(val digits: List<List<Float>>) : ComponentSnapshot()

class MultiSevenSegComponent(config: HwComponentConfig) : HwComponent(config) {
    private val digitCount = config.cols.coerceIn(2, 4)
    private val brightness = Array(4) { FloatArray(8) }
    private var lastTickCycles = 0L

    override fun needsTick() = true

    override fun tick(portValues: List<Int>, totalCycles: Long) {
        val rawSeg = portValues.getOrNull(0) ?: return
        val rawDig = portValues.getOrNull(1) ?: return

        val segVal = if (config.activeLow) (rawSeg xor 0xFF) and 0xFF else rawSeg and 0xFF
        val digVal = if (config.activeLow) (rawDig xor 0xFF) and 0xFF else rawDig and 0xFF

        val decay = if (totalCycles > 0L) {
            val delta = (totalCycles - lastTickCycles).coerceAtLeast(0L)
            lastTickCycles = totalCycles
            delta / 40_000f
        } else {
            0.002f
        }

        if (decay > 0f) {
            for (d in 0 until digitCount) {
                for (s in 0..7) {
                    if (brightness[d][s] > 0f) {
                        brightness[d][s] = (brightness[d][s] - decay).coerceAtLeast(0f)
                    }
                }
            }
        }

        for (d in 0 until digitCount) {
            if ((digVal and (1 shl d)) != 0) {
                for (s in 0..7) {
                    if ((segVal and (1 shl s)) != 0) {
                        brightness[d][s] = 1f
                    }
                }
            }
        }
    }

    override fun reset() {
        for (d in 0..3) brightness[d].fill(0f)
        lastTickCycles = 0L
    }

    override fun snapshot(): ComponentSnapshot = MultiSevenSegSnapshot(
        (0 until digitCount).map { d -> brightness[d].toList() }
    )
}

private val MultiSegDefs = listOf(
    listOf(8, 6, 44, 8),
    listOf(52, 10, 8, 36),
    listOf(52, 54, 8, 36),
    listOf(8, 86, 44, 8),
    listOf(0, 54, 8, 36),
    listOf(0, 10, 8, 36),
    listOf(8, 48, 44, 8),
)

@Composable
private fun MultiSevenSegDigit(segments: List<Float>, color: Color = Color(0xFFFF4444)) {
    val offColor = color.copy(alpha = 0.16f)
    Canvas(modifier = Modifier.size(width = 28.dp, height = 42.dp)) {
        val sx = size.width / 66f
        val sy = size.height / 100f
        for (i in 0..6) {
            val b = segments.getOrElse(i) { 0f }.coerceIn(0f, 1f)
            val c = if (b > 0.05f) color.copy(alpha = 0.16f + 0.84f * b) else offColor
            val s = MultiSegDefs[i]
            drawRoundRect(
                color = c,
                topLeft = Offset(s[0] * sx, s[1] * sy),
                size = Size(s[2] * sx, s[3] * sy),
                cornerRadius = CornerRadius(2f * sx, 2f * sy),
            )
        }
        val dpB = segments.getOrElse(7) { 0f }.coerceIn(0f, 1f)
        val dpColor = if (dpB > 0.05f) color.copy(alpha = 0.16f + 0.84f * dpB) else offColor
        drawCircle(
            color = dpColor,
            radius = 3f * sx.coerceAtMost(sy),
            center = Offset(65f * sx, 90f * sy),
        )
    }
}

object MultiSevenSegFactory : HwComponentFactory {
    override val typeId = "MULTI_SEVEN_SEG"
    override fun displayName() = "Multi 7-Segment"

    override fun createComponent(config: HwComponentConfig): HwComponent = MultiSevenSegComponent(config)

    override fun defaultConfig(id: String) = HwComponentConfig(
        id = id,
        label = "P2/P0 4×7-Seg",
        type = typeId,
        ports = listOf(Port.P2, Port.P0),
        cols = 4,
    )

    override fun labelFor(config: HwComponentConfig): String {
        val segPort = config.ports.getOrNull(0)?.name ?: "P2"
        val digPort = config.ports.getOrNull(1)?.name ?: "P0"
        val count = config.cols.coerceIn(2, 4)
        return "$segPort/$digPort ${count}×7-Seg"
    }

    override fun needsTick(config: HwComponentConfig) = true

    override fun portCount(config: HwComponentConfig) = 2

    override fun portLabels(config: HwComponentConfig) = listOf("Seg Port", "Digit Port")

    override fun usedPins(config: HwComponentConfig): List<Pair<Port, Int>> {
        val segPort = config.ports.getOrNull(0) ?: Port.P2
        val digPort = config.ports.getOrNull(1) ?: Port.P0
        val count = config.cols.coerceIn(2, 4)
        return (0..7).map { segPort to it } + (0 until count).map { digPort to it }
    }

    @Composable
    override fun Render(
        config: HwComponentConfig,
        snapshot: ComponentSnapshot?,
        portValues: List<Int>,
        onUserInput: (HwUserInput) -> Unit,
    ) {
        val count = config.cols.coerceIn(2, 4)
        val s = snapshot as? MultiSevenSegSnapshot
        val digits = s?.digits ?: List(count) { List(8) { 0f } }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                labelFor(config),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (d in (count - 1) downTo 0) {
                    MultiSevenSegDigit(digits.getOrElse(d) { List(8) { 0f } })
                }
            }
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
        var digitsExpanded by remember { mutableStateOf(false) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Digits", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(64.dp))
            ExposedDropdownMenuBox(
                expanded = digitsExpanded,
                onExpandedChange = { digitsExpanded = it },
                modifier = Modifier.widthIn(min = 70.dp),
            ) {
                OutlinedTextField(
                    value = config.cols.coerceIn(2, 4).toString(),
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = digitsExpanded) },
                    modifier = Modifier.menuAnchor(),
                    textStyle = MaterialTheme.typography.bodySmall,
                )
                ExposedDropdownMenu(expanded = digitsExpanded, onDismissRequest = { digitsExpanded = false }) {
                    listOf(2, 4).forEach { count ->
                        DropdownMenuItem(text = { Text(count.toString()) }, onClick = {
                            onUpdateComponent(config.id) { it.copy(cols = count) }
                            digitsExpanded = false
                        })
                    }
                }
            }
        }
        PolarityField(config, onUpdateComponent)
    }
}
