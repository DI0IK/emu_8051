package dev.dominikstahl.emu_8051.ui

import dev.dominikstahl.emu_8051.asm.AssemblyError
import dev.dominikstahl.emu_8051.ui.components.hardware.ComponentSnapshot

enum class SpeedMode { MANUAL, CUSTOM, UNLIMITED }

enum class Port(val addr: Int) { P0(0x80), P1(0x90), P2(0xA0), P3(0xB0) }

data class HwComponentConfig(
    val id: String,
    val label: String,
    val type: String,
    val port: Port,
    val pin: Int = -1,
    val enabled: Boolean = true,
    val rows: Int = 4,
    val cols: Int = 4,
    val lcdCols: Int = 16,
    val lcdLines: Int = 2,
    val props: Map<String, String> = emptyMap(),
)

fun defaultHwConfig(): List<HwComponentConfig> = emptyList()

data class EmulatorUiState(
    val pc: Int = 0,
    val acc: Int = 0,
    val b: Int = 0,
    val psw: Int = 0,
    val sp: Int = 0x07,
    val dpl: Int = 0,
    val dph: Int = 0,
    val p0: Int = 0xFF,
    val p1: Int = 0xFF,
    val p2: Int = 0xFF,
    val p3: Int = 0xFF,
    val totalCycles: Long = 0,
    val actualIps: Long = 0,
    val isRunning: Boolean = false,
    val speedMode: SpeedMode = SpeedMode.MANUAL,
    val isSlow: Boolean = false,
    val targetIps: Int = 10,
    val sourceCode: String = "",
    val assemblyErrors: List<AssemblyError> = emptyList(),
    val isProgramLoaded: Boolean = false,
    val hwConfig: List<HwComponentConfig> = defaultHwConfig(),
    val breakpoints: Set<Int> = emptySet(),
    val currentLine: Int? = null,
    val savedFiles: List<String> = emptyList(),
    val componentSnapshots: Map<String, ComponentSnapshot> = emptyMap(),
    val tmod: Int = 0,
    val tcon: Int = 0,
    val tl0: Int = 0,
    val th0: Int = 0,
    val tl1: Int = 0,
    val th1: Int = 0,
    val t2con: Int = 0,
    val tl2: Int = 0,
    val th2: Int = 0,
    val rcap2l: Int = 0,
    val rcap2h: Int = 0,
    val ie: Int = 0,
    val ip: Int = 0,
    val scon: Int = 0,
    val sbuf: Int = 0,
    val pcon: Int = 0,
)
