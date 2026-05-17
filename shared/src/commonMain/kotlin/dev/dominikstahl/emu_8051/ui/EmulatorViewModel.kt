package dev.dominikstahl.emu_8051.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.dominikstahl.emu_8051.asm.assemble
import dev.dominikstahl.emu_8051.engine.CpuState
import dev.dominikstahl.emu_8051.engine.Interpreter
import dev.dominikstahl.emu_8051.platform.FileStorage
import dev.dominikstahl.emu_8051.platform.createFileStorage
import dev.dominikstahl.emu_8051.ui.components.hardware.ComponentSnapshot
import dev.dominikstahl.emu_8051.ui.components.hardware.HwComponent
import dev.dominikstahl.emu_8051.ui.components.hardware.HwRegistry
import dev.dominikstahl.emu_8051.ui.components.hardware.HwUserInput
import dev.dominikstahl.emu_8051.ui.components.hardware.PortController
import dev.dominikstahl.emu_8051.ui.components.hardware.registerBuiltinHardwareComponents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield
import kotlin.time.TimeSource

@OptIn(ExperimentalUnsignedTypes::class)
class EmulatorViewModel(
    private val storage: FileStorage = createFileStorage()
) : ViewModel() {
    private val cpuState = CpuState()
    private val interpreter = Interpreter(cpuState)
    private val mutex = Mutex()

    private val _uiState = MutableStateFlow(EmulatorUiState())
    val uiState: StateFlow<EmulatorUiState> = _uiState.asStateFlow()
    private var pcToLineMap: Map<Int, Int> = emptyMap()

    private var runJob: Job? = null
    private var autoAssembleJob: Job? = null

    private val portController = object : PortController {
        override fun drive(port: Port, mask: Int, value: Int) {
            val idx = port.ordinal
            cpuState.externalDriven[idx] = cpuState.externalDriven[idx] or mask
            cpuState.externalValue[idx] = (cpuState.externalValue[idx] and mask.inv()) or (value and mask)
        }

        override fun release(port: Port, mask: Int) {
            val idx = port.ordinal
            cpuState.externalDriven[idx] = cpuState.externalDriven[idx] and mask.inv()
            cpuState.externalValue[idx] = cpuState.externalValue[idx] or mask
        }
    }

    init {
        registerBuiltinHardwareComponents()
    }

    private fun snapshotState(isSlow: Boolean = false, actualIps: Long = 0): EmulatorUiState {
        val prev = _uiState.value
        val pc = cpuState.pc
        val currentLine = pcToLineMap[pc]
        return prev.copy(
            pc = pc,
            acc = cpuState.ACC.toInt(),
            b = cpuState.B.toInt(),
            psw = cpuState.PSW.toInt(),
            sp = cpuState.SP.toInt(),
            dpl = cpuState.DPL.toInt(),
            dph = cpuState.DPH.toInt(),
            p0 = cpuState.getEffectivePort(0),
            p1 = cpuState.getEffectivePort(1),
            p2 = cpuState.getEffectivePort(2),
            p3 = cpuState.getEffectivePort(3),
            totalCycles = cpuState.totalCycles,
            actualIps = actualIps,
            tmod = cpuState.TMOD.toInt(),
            tcon = cpuState.TCON.toInt(),
            tl0 = cpuState.TL0.toInt(),
            th0 = cpuState.TH0.toInt(),
            tl1 = cpuState.TL1.toInt(),
            th1 = cpuState.TH1.toInt(),
            t2con = cpuState.T2CON.toInt(),
            tl2 = cpuState.TL2.toInt(),
            th2 = cpuState.TH2.toInt(),
            rcap2l = cpuState.RCAP2L.toInt(),
            rcap2h = cpuState.RCAP2H.toInt(),
            ie = cpuState.IE.toInt(),
            ip = cpuState.IP.toInt(),
            scon = cpuState.SCON.toInt(),
            sbuf = cpuState.SBUF.toInt(),
            pcon = cpuState.PCON.toInt(),
            isSlow = isSlow,
            currentLine = currentLine,
        )
    }

    fun step() {
        viewModelScope.launch(Dispatchers.Default) {
            mutex.withLock {
                interpreter.step()
                tickComponents()
                captureSnapshots()
                _uiState.value = snapshotState()
            }
        }
    }

// TODO: Format
fun run(mode: SpeedMode) {
    runJob?.cancel()
    if (mode == SpeedMode.MANUAL) {
        _uiState.value = _uiState.value.copy(isRunning = false, speedMode = mode)
        return
    }

    _uiState.value = _uiState.value.copy(isRunning = true, speedMode = mode)

    runJob = viewModelScope.launch(Dispatchers.Default) {
        val timeSource = TimeSource.Monotonic
        var skipFirstBreakpointCheck = true
        var ipsWindowStart = timeSource.markNow()
        var ipsWindowCycles = 0L
        var smoothedActualIps = 0L
        
        val frameDurationMs = 20L
        var lastFrameMark = timeSource.markNow()

        while (isActive) {
            val currentMode = _uiState.value.speedMode
            val currentTargetIps = _uiState.value.targetIps.toDouble()

            // --- 1. UNLIMITED SPEED MODE ---
            if (currentMode == SpeedMode.UNLIMITED) {
                var cyclesThisFrame = 0
                var breakpointHit = false
                
                mutex.withLock {
                    val breakpoints = _uiState.value.breakpoints
                    val hasBreakpoints = breakpoints.isNotEmpty()
                    
                    // Keep chunk sizes sane (50k) so the UI thread can still acquire the mutex lock
                    while (cyclesThisFrame < 50_000 && !breakpointHit) {
                        if (hasBreakpoints && !skipFirstBreakpointCheck) {
                            val currentLine = pcToLineMap[cpuState.pc]
                            if (currentLine != null && currentLine in breakpoints) {
                                breakpointHit = true
                                break
                            }
                        }
                        skipFirstBreakpointCheck = false

                        cyclesThisFrame += interpreter.step()
                        tickComponents()
                    }
                    captureSnapshots()
                    _uiState.value = if (breakpointHit) {
                        snapshotState().copy(isRunning = false)
                    } else {
                        snapshotState(isSlow = false, actualIps = smoothedActualIps)
                    }
                }

                ipsWindowCycles += cyclesThisFrame
                if (breakpointHit) break

                val windowElapsed = ipsWindowStart.elapsedNow().inWholeMicroseconds
                if (windowElapsed >= 500_000) {
                    smoothedActualIps = (ipsWindowCycles * 1_000_000L) / windowElapsed
                    ipsWindowCycles = 0L
                    ipsWindowStart = timeSource.markNow()
                }

                yield()
                continue
            }

            // --- 2. THROTTLED MODE (SpeedMode.CUSTOM) ---
            val elapsedMs = lastFrameMark.elapsedNow().inWholeMilliseconds
            
            if (elapsedMs < frameDurationMs) {
                delay(frameDurationMs - elapsedMs)
                continue
            }
            
            // Limit the maximum elapsed time used for cycle calculations.
            // Even if the system stuttered for 500ms, we only calculate cycles for a max of 2 frames (40ms).
            val effectiveElapsedMs = elapsedMs.coerceAtMost(frameDurationMs * 2)
            lastFrameMark = timeSource.markNow()

            // Lower upper-bound cap to 100k cycles. At 1M IPS, a 20ms frame is 20k cycles.
            val cyclesToRun = ((currentTargetIps * effectiveElapsedMs) / 1000.0).toInt().coerceIn(1, 100_000)

            var cyclesThisFrame = 0
            var breakpointHit = false
            val executionStart = timeSource.markNow()

            mutex.withLock {
                if (cyclesToRun > 0) {
                    val breakpoints = _uiState.value.breakpoints
                    val hasBreakpoints = breakpoints.isNotEmpty()
                    while (cyclesThisFrame < cyclesToRun && !breakpointHit) {
                        if (hasBreakpoints && !skipFirstBreakpointCheck) {
                            val currentLine = pcToLineMap[cpuState.pc]
                            if (currentLine != null && currentLine in breakpoints) {
                                breakpointHit = true
                                break
                            }
                        }
                        skipFirstBreakpointCheck = false

                        cyclesThisFrame += interpreter.step()
                        tickComponents()
                    }
                }
                captureSnapshots()
                
                val executionTimeMs = executionStart.elapsedNow().inWholeMilliseconds
                val isSlow = executionTimeMs > frameDurationMs

                _uiState.value = if (breakpointHit) {
                    snapshotState().copy(isRunning = false)
                } else {
                    snapshotState(isSlow = isSlow, actualIps = smoothedActualIps)
                }
            }

            ipsWindowCycles += cyclesThisFrame
            if (breakpointHit) break

            // Update IPS metrics window
            val windowElapsed = ipsWindowStart.elapsedNow().inWholeMicroseconds
            if (windowElapsed >= 500_000) {
                smoothedActualIps = (ipsWindowCycles * 1_000_000L) / windowElapsed
                ipsWindowCycles = 0L
                ipsWindowStart = timeSource.markNow()
            }

            val totalFrameTimeMs = executionStart.elapsedNow().inWholeMilliseconds
            if (totalFrameTimeMs < frameDurationMs) {
                // Host is running fast enough. Sleep away the leftover frame time budget.
                delay(frameDurationMs - totalFrameTimeMs)
            } else {
                // CRITICAL SAFETY VALVE: The host is too slow.
                // We wipe out the time debt by resetting the timeline mark to right NOW.
                lastFrameMark = timeSource.markNow()
                yield()
            }
        }
    }
}
    
    fun pause() {
        runJob?.cancel()
        runJob = null
        _uiState.value = _uiState.value.copy(isRunning = false, actualIps = 0)
    }

    fun reset() {
        pause()
        viewModelScope.launch(Dispatchers.Default) {
            mutex.withLock {
                cpuState.reset()
                components.values.toList().forEach { it.reset() }
                _uiState.value = snapshotState()
            }
        }
    }

    fun setSpeed(mode: SpeedMode) {
        _uiState.value = _uiState.value.copy(speedMode = mode)
        when {
            mode == SpeedMode.MANUAL -> pause()
            _uiState.value.isRunning -> run(mode)
        }
    }

    fun setTargetIps(ips: Int) {
        _uiState.value = _uiState.value.copy(targetIps = ips)
    }

    fun updateSource(text: String) {
        _uiState.value = _uiState.value.copy(sourceCode = text, assemblyErrors = emptyList())
        autoAssembleJob?.cancel()
        autoAssembleJob = viewModelScope.launch {
            delay(500)
            if (text.isNotBlank()) {
                assemble()
            }
        }
    }

    fun assemble() {
        viewModelScope.launch(Dispatchers.Default) {
            val source = _uiState.value.sourceCode
            val result = assemble(source)
            if (result.isSuccess) {
                mutex.withLock {
                    val rom = result.rom!!
                    for (i in rom.indices) {
                        cpuState.rom[i] = rom[i]
                    }
                    cpuState.reset()
                    components.values.toList().forEach { it.reset() }
                    _uiState.value = snapshotState().copy(
                        assemblyErrors = emptyList(),
                        isProgramLoaded = true,
                        assemblyVersion = _uiState.value.assemblyVersion + 1,
                    )
                }
                pcToLineMap = result.sourceMap.entries.associate { it.value to it.key }
            } else {
                _uiState.value = _uiState.value.copy(
                    assemblyErrors = result.errors,
                    isProgramLoaded = false,
                )
            }
        }
    }

    fun toggleBreakpoint(line: Int) {
        val bp = _uiState.value.breakpoints
        _uiState.value = _uiState.value.copy(
            breakpoints = if (line in bp) bp - line else bp + line
        )
    }

    private val components = mutableMapOf<String, HwComponent>()

    private fun getOrCreateComponent(comp: HwComponentConfig): HwComponent {
        return components.getOrPut(comp.id) {
            val factory = HwRegistry.get(comp.type)
            requireNotNull(factory) { "Unknown hardware component type: ${comp.type}" }
            factory.createComponent(comp)
        }
    }

    private fun captureSnapshots() {
        val snapshots = mutableMapOf<String, ComponentSnapshot>()
        for (comp in _uiState.value.hwConfig.filter { it.enabled }) {
            snapshots[comp.id] = getOrCreateComponent(comp).snapshot()
        }
        _uiState.value = _uiState.value.copy(componentSnapshots = snapshots)
    }

    private var needsTickComponents: List<HwComponent>? = null

    private fun tickComponents() {
        var list = needsTickComponents
        if (list == null) {
            list = buildList {
                for (comp in _uiState.value.hwConfig) {
                    if (!comp.enabled) continue
                    val factory = HwRegistry.get(comp.type) ?: continue
                    if (factory.needsTick(comp)) {
                        add(getOrCreateComponent(comp))
                    }
                }
            }
            needsTickComponents = list
        }
        for (c in list) {
            c.tick(cpuState.getEffectivePort(c.config.port.ordinal))
        }
    }

    fun handleUserInput(input: HwUserInput) {
        viewModelScope.launch(Dispatchers.Default) {
            mutex.withLock {
                when (input) {
                    is HwUserInput.ToggleInput -> {
                        val idx = input.port.ordinal
                        val mask = 1 shl input.pin
                        if (input.on) {
                            cpuState.externalDriven[idx] = cpuState.externalDriven[idx] or mask
                            cpuState.externalValue[idx] = cpuState.externalValue[idx] or mask
                        } else {
                            cpuState.externalDriven[idx] = cpuState.externalDriven[idx] or mask
                            cpuState.externalValue[idx] = cpuState.externalValue[idx] and mask.inv()
                        }
                    }
                    is HwUserInput.KeyInput -> {
                        val comp = _uiState.value.hwConfig.find { it.id == input.compId } ?: return@withLock
                        val idx = comp.port.ordinal
                        val mask = 1 shl input.col
                        if (input.pressed) {
                            cpuState.externalDriven[idx] = cpuState.externalDriven[idx] or mask
                            cpuState.externalValue[idx] = cpuState.externalValue[idx] and mask.inv()
                        } else {
                            cpuState.externalDriven[idx] = cpuState.externalDriven[idx] and mask.inv()
                            cpuState.externalValue[idx] = cpuState.externalValue[idx] or mask
                        }
                        components[input.compId]?.onUserInput(input, portController)
                    }
                }
                captureSnapshots()
                _uiState.value = snapshotState()
            }
        }
    }

    fun getRomForDisplay(): UByteArray = cpuState.rom
    fun getRamForDisplay(): UByteArray = cpuState.ram
    fun getSfrForDisplay(): UByteArray = cpuState.sfr

    fun updateHwConfig(config: List<HwComponentConfig>) {
        needsTickComponents = null
        _uiState.value = _uiState.value.copy(hwConfig = config)
    }

    private var _nextHwId = 1
    private fun nextHwId(): String = "hw_${_nextHwId++}"

    fun defaultForType(typeId: String): HwComponentConfig? {
        val factory = HwRegistry.get(typeId) ?: return null
        return factory.defaultConfig(nextHwId())
    }

    fun addHwComponent(typeId: String) {
        val comp = defaultForType(typeId) ?: return
        needsTickComponents = null
        _uiState.value = _uiState.value.copy(
            hwConfig = _uiState.value.hwConfig + comp
        )
    }

    fun removeHwComponent(id: String) {
        needsTickComponents = null
        viewModelScope.launch(Dispatchers.Default) {
            mutex.withLock {
                components.remove(id)
            }
        }
        _uiState.value = _uiState.value.copy(
            hwConfig = _uiState.value.hwConfig.filter { it.id != id }
        )
    }

    fun updateHwComponent(id: String, transform: (HwComponentConfig) -> HwComponentConfig) {
        needsTickComponents = null
        viewModelScope.launch(Dispatchers.Default) {
            mutex.withLock {
                components.remove(id)
                _uiState.value = _uiState.value.copy(
                    hwConfig = _uiState.value.hwConfig.map {
                        if (it.id == id) {
                            val updated = transform(it)
                            val factory = HwRegistry.get(updated.type)
                            if (factory != null) updated.copy(label = factory.labelFor(updated)) else updated
                        } else it
                    }
                )
            }
        }
    }

    fun saveSource(name: String) {
        viewModelScope.launch(Dispatchers.Default) {
            storage.save(name, _uiState.value.sourceCode)
            _uiState.value = _uiState.value.copy(savedFiles = storage.list().sorted())
        }
    }

    fun loadSource(name: String) {
        viewModelScope.launch(Dispatchers.Default) {
            val content = storage.load(name)
            if (content != null) {
                updateSource(content)
            }
        }
    }

    fun refreshSavedFiles() {
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.value = _uiState.value.copy(savedFiles = storage.list().sorted())
        }
    }

    fun deleteSource(name: String) {
        viewModelScope.launch(Dispatchers.Default) {
            storage.delete(name)
            _uiState.value = _uiState.value.copy(savedFiles = storage.list().sorted())
        }
    }

    fun renameSource(oldName: String, newName: String) {
        viewModelScope.launch(Dispatchers.Default) {
            storage.rename(oldName, newName)
            _uiState.value = _uiState.value.copy(savedFiles = storage.list().sorted())
        }
    }
}
