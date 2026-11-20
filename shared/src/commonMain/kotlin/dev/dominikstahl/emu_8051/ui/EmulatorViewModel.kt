package dev.dominikstahl.emu_8051.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.dominikstahl.emu_8051.asm.assemble
import dev.dominikstahl.emu_8051.engine.CpuState
import dev.dominikstahl.emu_8051.engine.Interpreter
import dev.dominikstahl.emu_8051.engine.REN_BIT
import dev.dominikstahl.emu_8051.engine.RI_BIT
import dev.dominikstahl.emu_8051.engine.TI_BIT
import dev.dominikstahl.emu_8051.examples.BuiltinExample
import dev.dominikstahl.emu_8051.platform.FileFormat
import dev.dominikstahl.emu_8051.platform.FileStorage
import dev.dominikstahl.emu_8051.platform.copyToClipboard
import dev.dominikstahl.emu_8051.platform.createBuzzerAudioPlayer
import dev.dominikstahl.emu_8051.platform.createFileStorage
import dev.dominikstahl.emu_8051.platform.hasPlatformShare
import dev.dominikstahl.emu_8051.platform.pasteFromClipboard
import dev.dominikstahl.emu_8051.platform.platformShare
import dev.dominikstahl.emu_8051.ui.components.hardware.ComponentSnapshot
import dev.dominikstahl.emu_8051.ui.components.hardware.HwComponent
import dev.dominikstahl.emu_8051.ui.components.hardware.HwRegistry
import dev.dominikstahl.emu_8051.ui.components.hardware.HwUserInput
import dev.dominikstahl.emu_8051.ui.components.hardware.PortController
import dev.dominikstahl.emu_8051.ui.components.hardware.buzzer.BuzzerSnapshot
import dev.dominikstahl.emu_8051.ui.components.hardware.registerBuiltinHardwareComponents
import dev.dominikstahl.emu_8051.ui.components.hardware.uart.UartTerminalComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.TimeSource

@OptIn(ExperimentalUnsignedTypes::class)
class EmulatorViewModel(
    private val storage: FileStorage = createFileStorage()
) : ViewModel() {
    private val cpuState = CpuState()
    private val interpreter = Interpreter(cpuState)
    private val mutex = Mutex()
    private val buzzerAudio = createBuzzerAudioPlayer()

    private val _uiState = MutableStateFlow(EmulatorUiState())
    val uiState: StateFlow<EmulatorUiState> = _uiState.asStateFlow()

    // Protected by mutex
    private var pcToLineMap: Map<Int, Int> = emptyMap()
    private var lastBreakpointPausedPc: Int? = null

    private var cachedRomSnapshot = UByteArray(65536)
    private var cachedXramSnapshot = UByteArray(65536)
    private var memorySnapshotVersion = 0L

    private var runJob: Job? = null
    private var autoAssembleJob: Job? = null

    private class TickEntry(
        val component: HwComponent,
        val portOrdinals: IntArray,
        val buf: ArrayList<Int>,
    )

    private val components = mutableMapOf<String, HwComponent>()
    private var tickEntries: List<TickEntry>? = null
    private var uartComponents: List<UartTerminalComponent>? = null

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
        cpuState.onSbufTx = { byte ->
            val uarts = ensureUartComponentsLocked()
            for (u in uarts) {
                u.appendTx(byte)
            }
            cpuState.SCON = (cpuState.SCON.toInt() or TI_BIT).toUByte()
        }
        val initialMem = buildMemorySnapshotsLocked()
        _uiState.update { it.copy(memorySnapshots = initialMem) }
    }

    override fun onCleared() {
        runJob?.cancel()
        autoAssembleJob?.cancel()
        buzzerAudio.release()
        super.onCleared()
    }

    private fun getOrCreateComponentLocked(comp: HwComponentConfig): HwComponent {
        return components.getOrPut(comp.id) {
            val factory = HwRegistry.get(comp.type)
            requireNotNull(factory) { "Unknown hardware component type: ${comp.type}" }
            factory.createComponent(comp).also { it.onAttach(portController) }
        }
    }

    private fun invalidateHardwareCachesLocked() {
        tickEntries = null
        uartComponents = null
    }

    private fun ensureTickEntriesLocked(): List<TickEntry> {
        tickEntries?.let { return it }
        val built = buildList {
            for (comp in _uiState.value.hwConfig) {
                if (!comp.enabled) continue
                val factory = HwRegistry.get(comp.type) ?: continue
                val instance = getOrCreateComponentLocked(comp)
                if (factory.needsTick(comp)) {
                    val count = factory.portCount(comp)
                    val ordinals = IntArray(count) { i -> comp.ports.getOrNull(i)?.ordinal ?: i }
                    add(TickEntry(instance, ordinals, ArrayList<Int>(count).apply { repeat(count) { add(0) } }))
                }
            }
        }
        tickEntries = built
        return built
    }

    private fun ensureUartComponentsLocked(): List<UartTerminalComponent> {
        uartComponents?.let { return it }
        val built = buildList {
            for (comp in _uiState.value.hwConfig) {
                if (comp.enabled && comp.type == "UART_TERMINAL") {
                    val c = getOrCreateComponentLocked(comp)
                    if (c is UartTerminalComponent) add(c)
                }
            }
        }
        uartComponents = built
        return built
    }

    private fun tickComponentsLocked() {
        val entries = ensureTickEntriesLocked()
        val cycles = cpuState.totalCycles
        for (i in entries.indices) {
            val entry = entries[i]
            val ordinals = entry.portOrdinals
            val buf = entry.buf
            for (p in ordinals.indices) {
                buf[p] = cpuState.getEffectivePort(ordinals[p])
            }
            entry.component.tick(buf, cycles)
        }
    }

    private fun drainUartRxLocked() {
        val uarts = ensureUartComponentsLocked()
        if (uarts.isEmpty()) return
        for (u in uarts) {
            val scon = cpuState.SCON.toInt()
            if ((scon and REN_BIT) != 0 && (scon and RI_BIT) == 0) {
                val nextByte = u.dequeueRxIfReady(scon)
                if (nextByte != null) {
                    cpuState.receiveSerialByte(nextByte)
                    cpuState.SCON = (scon or RI_BIT).toUByte()
                    break
                }
            }
        }
    }

    private fun stepOnceLocked(): Int {
        val cycles = interpreter.step()
        tickComponentsLocked()
        drainUartRxLocked()
        return cycles
    }

    private fun collectSnapshotsLocked(hwConfig: List<HwComponentConfig>): Map<String, ComponentSnapshot> {
        val snapshots = LinkedHashMap<String, ComponentSnapshot>(hwConfig.size)
        for (comp in hwConfig) {
            if (!comp.enabled) continue
            if (HwRegistry.get(comp.type) == null) continue
            snapshots[comp.id] = getOrCreateComponentLocked(comp).snapshot()
        }
        return snapshots
    }

    private fun syncBuzzerAudio(snapshots: Map<String, ComponentSnapshot>, isEmulating: Boolean) {
        if (!isEmulating) {
            buzzerAudio.setTone(0.0, false)
            return
        }
        var activeFreq = 0.0
        var anyActive = false
        for (snap in snapshots.values) {
            if (snap is BuzzerSnapshot && snap.on && !snap.muted) {
                anyActive = true
                activeFreq = snap.frequencyHz.toDouble()
                break
            }
        }
        if (anyActive) {
            buzzerAudio.setMuted(false)
            buzzerAudio.setTone(activeFreq, true)
        } else {
            buzzerAudio.setTone(0.0, false)
        }
    }

    private fun buildMemorySnapshotsLocked(): MemorySnapshots {
        if (cpuState.xramDirty) {
            cachedXramSnapshot = cpuState.xram.copyOf()
            cpuState.xramDirty = false
        }
        return MemorySnapshots(
            rom = cachedRomSnapshot,
            ram = cpuState.ram.copyOf(),
            xram = cachedXramSnapshot,
            sfr = cpuState.sfr.copyOf(),
            version = ++memorySnapshotVersion,
        )
    }

    private fun commitStateLocked(
        isSlow: Boolean = false,
        actualIps: Long = 0,
        isRunningOverride: Boolean? = null,
    ) {
        val currentState = _uiState.value
        val pc = cpuState.pc
        val currentLine = pcToLineMap[pc]
        val snapshots = collectSnapshotsLocked(currentState.hwConfig)
        val effectiveRunning = isRunningOverride ?: currentState.isRunning
        syncBuzzerAudio(snapshots, effectiveRunning)
        val memSnaps = buildMemorySnapshotsLocked()
        val bankBase = cpuState.registerBankBase
        val regs = List(8) { i -> cpuState.ram[bankBase + i].toInt() }
        val acc = cpuState.ACC.toInt()
        val b = cpuState.B.toInt()
        val psw = cpuState.PSW.toInt()
        val sp = cpuState.SP.toInt()
        val dpl = cpuState.DPL.toInt()
        val dph = cpuState.DPH.toInt()
        val p0 = cpuState.getEffectivePort(0)
        val p1 = cpuState.getEffectivePort(1)
        val p2 = cpuState.getEffectivePort(2)
        val p3 = cpuState.getEffectivePort(3)
        val totalCycles = cpuState.totalCycles
        val tmod = cpuState.TMOD.toInt()
        val tcon = cpuState.TCON.toInt()
        val tl0 = cpuState.TL0.toInt()
        val th0 = cpuState.TH0.toInt()
        val tl1 = cpuState.TL1.toInt()
        val th1 = cpuState.TH1.toInt()
        val t2con = cpuState.T2CON.toInt()
        val tl2 = cpuState.TL2.toInt()
        val th2 = cpuState.TH2.toInt()
        val rcap2l = cpuState.RCAP2L.toInt()
        val rcap2h = cpuState.RCAP2H.toInt()
        val ie = cpuState.IE.toInt()
        val ip = cpuState.IP.toInt()
        val scon = cpuState.SCON.toInt()
        val sbuf = cpuState.sbufRx.toInt()
        val sbufTx = cpuState.sbufTx.toInt()
        val pcon = cpuState.PCON.toInt()

        _uiState.update { prev ->
            prev.copy(
                pc = pc,
                acc = acc,
                b = b,
                psw = psw,
                sp = sp,
                dpl = dpl,
                dph = dph,
                p0 = p0,
                p1 = p1,
                p2 = p2,
                p3 = p3,
                activeRegisters = regs,
                totalCycles = totalCycles,
                actualIps = actualIps,
                isRunning = effectiveRunning,
                tmod = tmod,
                tcon = tcon,
                tl0 = tl0,
                th0 = th0,
                tl1 = tl1,
                th1 = th1,
                t2con = t2con,
                tl2 = tl2,
                th2 = th2,
                rcap2l = rcap2l,
                rcap2h = rcap2h,
                ie = ie,
                ip = ip,
                scon = scon,
                sbuf = sbuf,
                sbufTx = sbufTx,
                pcon = pcon,
                isSlow = isSlow,
                currentLine = currentLine,
                componentSnapshots = snapshots,
                memorySnapshots = memSnaps,
            )
        }
    }

    private suspend fun cancelRunJobAndWait() {
        val oldJob = runJob
        runJob = null
        oldJob?.cancelAndJoin()
        buzzerAudio.setTone(0.0, false)
    }

    fun step() {
        viewModelScope.launch(Dispatchers.Default) {
            cancelRunJobAndWait()
            mutex.withLock {
                lastBreakpointPausedPc = null
                stepOnceLocked()
                commitStateLocked(isRunningOverride = false)
            }
        }
    }

    fun stepOver() {
        viewModelScope.launch(Dispatchers.Default) {
            cancelRunJobAndWait()
            var targetPc: Int? = null
            var entrySp = 0
            mutex.withLock {
                lastBreakpointPausedPc = null
                val pc = cpuState.pc
                val op = cpuState.rom[pc].toInt()
                val callLen = when {
                    op == 0x12 -> 3 // LCALL
                    (op and 0x1F) == 0x11 -> 2 // ACALL
                    else -> 0
                }
                if (callLen == 0) {
                    stepOnceLocked()
                    commitStateLocked(isRunningOverride = false)
                    return@launch
                }
                targetPc = (pc + callLen) and 0xFFFF
                entrySp = cpuState.SP.toInt()
            }
            val stopPc = targetPc ?: return@launch
            launchStepUntil(stopCondition = { cpuState.pc == stopPc && cpuState.SP.toInt() <= entrySp })
        }
    }

    fun stepOut() {
        viewModelScope.launch(Dispatchers.Default) {
            cancelRunJobAndWait()
            var entrySp = 0
            mutex.withLock {
                lastBreakpointPausedPc = null
                entrySp = cpuState.SP.toInt()
            }
            if (entrySp < 2) {
                step()
                return@launch
            }
            val targetSp = (entrySp - 2) and 0xFF
            launchStepUntil(stopCondition = { cpuState.SP.toInt() <= targetSp })
        }
    }

    private fun launchStepUntil(stopCondition: () -> Boolean) {
        _uiState.update { it.copy(isRunning = true) }
        runJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                var reached = false
                var skipCurrent = true
                while (isActive && !reached) {
                    mutex.withLock {
                        if (!isActive) return@withLock
                        val breakpoints = _uiState.value.breakpoints
                        val hasBp = breakpoints.isNotEmpty()
                        var cycles = 0
                        while (cycles < 25_000) {
                            if (hasBp && !skipCurrent) {
                                val line = pcToLineMap[cpuState.pc]
                                if (line != null && line in breakpoints) {
                                    lastBreakpointPausedPc = cpuState.pc
                                    reached = true
                                    break
                                }
                            }
                            skipCurrent = false
                            cycles += stepOnceLocked()
                            if (stopCondition()) {
                                reached = true
                                break
                            }
                        }
                        commitStateLocked(isRunningOverride = !reached)
                    }
                    if (!reached) delay(1)
                }
            } finally {
                buzzerAudio.setTone(0.0, false)
                _uiState.update { it.copy(isRunning = false, actualIps = 0) }
            }
        }
    }

    fun run(mode: SpeedMode) {
        val oldJob = runJob
        runJob = null
        if (mode == SpeedMode.MANUAL) {
            oldJob?.cancel()
            buzzerAudio.setTone(0.0, false)
            _uiState.update { it.copy(isRunning = false, speedMode = mode, actualIps = 0) }
            return
        }

        _uiState.update { it.copy(isRunning = true, speedMode = mode) }

        runJob = viewModelScope.launch(Dispatchers.Default) {
            oldJob?.cancelAndJoin()
            try {
                val timeSource = TimeSource.Monotonic
                var skipBreakpointOnce = mutex.withLock {
                    val shouldSkip = lastBreakpointPausedPc != null && lastBreakpointPausedPc == cpuState.pc
                    lastBreakpointPausedPc = null
                    shouldSkip
                }

                var ipsWindowStart = timeSource.markNow()
                var ipsWindowCycles = 0L
                var smoothedActualIps = 0L

                val frameDurationMs = 20.0
                var lastFrameMark = timeSource.markNow()
                var cycleBudget = 0.0

                while (isActive) {
                    val stateSnap = _uiState.value
                    val currentMode = stateSnap.speedMode
                    val currentTargetIps = stateSnap.targetIps.toDouble()

                    // --- 1. UNLIMITED SPEED MODE ---
                    if (currentMode == SpeedMode.UNLIMITED) {
                        var cyclesThisFrame = 0
                        var breakpointHit = false

                        mutex.withLock {
                            if (!isActive) return@withLock
                            val breakpoints = _uiState.value.breakpoints
                            val hasBreakpoints = breakpoints.isNotEmpty()

                            while (cyclesThisFrame < 25_000 && !breakpointHit) {
                                if (hasBreakpoints && !skipBreakpointOnce) {
                                    val currentLine = pcToLineMap[cpuState.pc]
                                    if (currentLine != null && currentLine in breakpoints) {
                                        lastBreakpointPausedPc = cpuState.pc
                                        breakpointHit = true
                                        break
                                    }
                                }
                                skipBreakpointOnce = false

                                cyclesThisFrame += stepOnceLocked()
                            }
                            commitStateLocked(
                                isSlow = false,
                                actualIps = if (breakpointHit) 0 else smoothedActualIps,
                                isRunningOverride = !breakpointHit,
                            )
                        }

                        ipsWindowCycles += cyclesThisFrame
                        if (breakpointHit) break

                        val windowElapsed = ipsWindowStart.elapsedNow().inWholeMicroseconds
                        if (windowElapsed >= 500_000) {
                            smoothedActualIps = (ipsWindowCycles * 1_000_000L) / windowElapsed
                            ipsWindowCycles = 0L
                            ipsWindowStart = timeSource.markNow()
                        }

                        // Yield to host/browser event loop so UI stays responsive on single-threaded JS/Wasm
                        delay(1)
                        continue
                    }

                    // --- 2. THROTTLED MODE (SpeedMode.CUSTOM) ---
                    val elapsedMs = lastFrameMark.elapsedNow().inWholeMicroseconds / 1000.0

                    if (elapsedMs < frameDurationMs) {
                        val remainingMs = (frameDurationMs - elapsedMs).toLong().coerceAtLeast(1L)
                        delay(remainingMs)
                        continue
                    }

                    val effectiveElapsedMs = elapsedMs.coerceAtMost(frameDurationMs * 2)
                    lastFrameMark = timeSource.markNow()

                    cycleBudget += (currentTargetIps * effectiveElapsedMs) / 1000.0
                    val cyclesToRun = cycleBudget.toInt()

                    var cyclesThisFrame = 0
                    var breakpointHit = false

                    mutex.withLock {
                        if (!isActive) return@withLock
                        if (cyclesToRun > 0) {
                            val breakpoints = _uiState.value.breakpoints
                            val hasBreakpoints = breakpoints.isNotEmpty()
                            while (cyclesThisFrame < cyclesToRun && !breakpointHit) {
                                if (hasBreakpoints && !skipBreakpointOnce) {
                                    val currentLine = pcToLineMap[cpuState.pc]
                                    if (currentLine != null && currentLine in breakpoints) {
                                        lastBreakpointPausedPc = cpuState.pc
                                        breakpointHit = true
                                        break
                                    }
                                }
                                skipBreakpointOnce = false

                                cyclesThisFrame += stepOnceLocked()
                            }
                            cycleBudget = (cycleBudget - cyclesThisFrame).coerceAtLeast(-4.0)
                        }

                        val frameElapsedMs = lastFrameMark.elapsedNow().inWholeMicroseconds / 1000.0
                        commitStateLocked(
                            isSlow = !breakpointHit && frameElapsedMs > frameDurationMs,
                            actualIps = if (breakpointHit) 0 else smoothedActualIps,
                            isRunningOverride = !breakpointHit,
                        )
                    }

                    ipsWindowCycles += cyclesThisFrame
                    if (breakpointHit) break

                    val windowElapsed = ipsWindowStart.elapsedNow().inWholeMicroseconds
                    if (windowElapsed >= 500_000) {
                        smoothedActualIps = (ipsWindowCycles * 1_000_000L) / windowElapsed
                        ipsWindowCycles = 0L
                        ipsWindowStart = timeSource.markNow()
                    }

                    val totalFrameTimeMs = lastFrameMark.elapsedNow().inWholeMicroseconds / 1000.0
                    if (totalFrameTimeMs < frameDurationMs) {
                        delay((frameDurationMs - totalFrameTimeMs).toLong().coerceAtLeast(1L))
                    } else {
                        lastFrameMark = timeSource.markNow()
                        cycleBudget = cycleBudget.coerceAtMost(cyclesToRun.toDouble())
                        delay(1)
                    }
                }
            } finally {
                buzzerAudio.setTone(0.0, false)
                _uiState.update { it.copy(isRunning = false, actualIps = 0) }
            }
        }
    }

    fun pause() {
        val job = runJob
        runJob = null
        job?.cancel()
        buzzerAudio.setTone(0.0, false)
        _uiState.update { it.copy(isRunning = false, actualIps = 0) }
    }

    fun reset() {
        viewModelScope.launch(Dispatchers.Default) {
            cancelRunJobAndWait()
            mutex.withLock {
                lastBreakpointPausedPc = null
                cpuState.reset()
                components.values.toList().forEach { it.reset() }
                commitStateLocked(isRunningOverride = false)
            }
        }
    }

    fun setSpeed(mode: SpeedMode) {
        _uiState.update { it.copy(speedMode = mode) }
        when {
            mode == SpeedMode.MANUAL -> pause()
            _uiState.value.isRunning -> run(mode)
        }
    }

    fun setTargetIps(ips: Int) {
        _uiState.update { it.copy(targetIps = ips) }
    }

    fun updateSource(text: String) {
        if (_uiState.value.isRunning) {
            pause()
        }
        _uiState.update {
            it.copy(
                sourceCode = text,
                isDirty = true,
                assemblyErrors = emptyList(),
                assemblyWarnings = emptyList(),
            )
        }
        autoAssembleJob?.cancel()
        if (text.isBlank()) {
            viewModelScope.launch(Dispatchers.Default) {
                cancelRunJobAndWait()
                mutex.withLock {
                    lastBreakpointPausedPc = null
                    cpuState.rom.fill(0u)
                    cachedRomSnapshot = UByteArray(65536)
                    pcToLineMap = emptyMap()
                    cpuState.reset()
                    components.values.toList().forEach { it.reset() }
                    _uiState.update { prev ->
                        prev.copy(
                            isProgramLoaded = false,
                            assemblyErrors = emptyList(),
                            assemblyWarnings = emptyList(),
                            currentLine = null,
                            assemblyVersion = prev.assemblyVersion + 1,
                        )
                    }
                    commitStateLocked(isRunningOverride = false)
                }
            }
            return
        }
        autoAssembleJob = viewModelScope.launch {
            delay(400)
            assemble()
        }
    }

    fun assemble() {
        viewModelScope.launch(Dispatchers.Default) {
            assembleNowLocked()
        }
    }

    private suspend fun assembleNowLocked() {
        cancelRunJobAndWait()
        val source = _uiState.value.sourceCode
        if (source.isBlank()) return
        val result = assemble(source)
        mutex.withLock {
            lastBreakpointPausedPc = null
            if (result.isSuccess) {
                val rom = result.rom!!
                rom.copyInto(cpuState.rom)
                cachedRomSnapshot = rom.copyOf()
                pcToLineMap = result.sourceMap.entries.associate { it.value to it.key }
                cpuState.reset()
                components.values.toList().forEach { it.reset() }
                _uiState.update { prev ->
                    prev.copy(
                        assemblyErrors = emptyList(),
                        assemblyWarnings = result.warnings,
                        isProgramLoaded = true,
                        assemblyVersion = prev.assemblyVersion + 1,
                    )
                }
                commitStateLocked(isRunningOverride = false)
            } else {
                _uiState.update { prev ->
                    prev.copy(
                        assemblyErrors = result.errors,
                        assemblyWarnings = result.warnings,
                        isProgramLoaded = false,
                    )
                }
            }
        }
    }

    fun toggleBreakpoint(line: Int) {
        _uiState.update { prev ->
            val bp = prev.breakpoints
            prev.copy(breakpoints = if (line in bp) bp - line else bp + line)
        }
    }

    fun handleUserInput(input: HwUserInput) {
        viewModelScope.launch(Dispatchers.Default) {
            mutex.withLock {
                when (input) {
                    is HwUserInput.SerialInput -> {
                        val comp = components[input.compId] as? UartTerminalComponent
                        if (comp != null) {
                            comp.enqueueRx(input.char)
                            drainUartRxLocked()
                        }
                    }
                    is HwUserInput.SerialStringInput -> {
                        val comp = components[input.compId] as? UartTerminalComponent
                        if (comp != null) {
                            comp.enqueueRxString(input.text)
                            drainUartRxLocked()
                        }
                    }
                    is HwUserInput.ClearUartInput -> {
                        (components[input.compId] as? UartTerminalComponent)?.clearTx()
                    }
                    is HwUserInput.ToggleInput -> {
                        components[input.compId]?.onUserInput(input, portController)
                    }
                    is HwUserInput.ButtonInput -> {
                        components[input.compId]?.onUserInput(input, portController)
                    }
                    is HwUserInput.DipSwitchInput -> {
                        components[input.compId]?.onUserInput(input, portController)
                    }
                    is HwUserInput.KeyInput -> {
                        components[input.compId]?.onUserInput(input, portController)
                    }
                    is HwUserInput.BuzzerMuteInput -> {
                        components[input.compId]?.onUserInput(input, portController)
                    }
                }
                commitStateLocked()
            }
        }
    }

    private var _nextHwId = 1
    private fun nextHwId(): String = "hw_${_nextHwId++}"

    private fun advanceNextHwIdPast(config: List<HwComponentConfig>) {
        var maxId = _nextHwId - 1
        for (c in config) {
            val num = c.id.removePrefix("hw_").toIntOrNull()
            if (num != null && num > maxId) maxId = num
        }
        _nextHwId = maxId + 1
    }

    private fun replaceHardwareConfigLocked(newConfig: List<HwComponentConfig>) {
        components.values.toList().forEach { it.onRemove(portController) }
        components.clear()
        cpuState.externalDriven.fill(0)
        cpuState.externalValue.fill(0)
        invalidateHardwareCachesLocked()
        advanceNextHwIdPast(newConfig)
        _uiState.update { it.copy(hwConfig = newConfig) }
        for (comp in newConfig) {
            if (comp.enabled && HwRegistry.get(comp.type) != null) {
                getOrCreateComponentLocked(comp)
            }
        }
    }

    fun addHwComponent(typeId: String) {
        val factory = HwRegistry.get(typeId) ?: return
        val comp = factory.defaultConfig(nextHwId())
        viewModelScope.launch(Dispatchers.Default) {
            mutex.withLock {
                invalidateHardwareCachesLocked()
                _uiState.update { it.copy(hwConfig = it.hwConfig + comp) }
                getOrCreateComponentLocked(comp)
                commitStateLocked()
            }
        }
    }

    fun removeHwComponent(id: String) {
        viewModelScope.launch(Dispatchers.Default) {
            mutex.withLock {
                invalidateHardwareCachesLocked()
                components.remove(id)?.onRemove(portController)
                _uiState.update { prev ->
                    prev.copy(hwConfig = prev.hwConfig.filter { it.id != id })
                }
                commitStateLocked()
            }
        }
    }

    fun updateHwComponent(id: String, transform: (HwComponentConfig) -> HwComponentConfig) {
        viewModelScope.launch(Dispatchers.Default) {
            mutex.withLock {
                invalidateHardwareCachesLocked()
                components.remove(id)?.onRemove(portController)
                var updatedComp: HwComponentConfig? = null
                _uiState.update { prev ->
                    prev.copy(
                        hwConfig = prev.hwConfig.map {
                            if (it.id == id) {
                                val updated = transform(it)
                                val factory = HwRegistry.get(updated.type)
                                val labeled = if (factory != null) updated.copy(label = factory.labelFor(updated)) else updated
                                updatedComp = labeled
                                labeled
                            } else it
                        }
                    )
                }
                updatedComp?.let {
                    if (it.enabled && HwRegistry.get(it.type) != null) {
                        getOrCreateComponentLocked(it)
                    }
                }
                commitStateLocked()
            }
        }
    }

    fun saveSource(name: String) {
        viewModelScope.launch(Dispatchers.Default) {
            val snap = _uiState.value
            val content = FileFormat.encode(snap.sourceCode, snap.hwConfig)
            storage.save(name, content)
            val files = storage.list().sorted()
            _uiState.update {
                it.copy(
                    savedFiles = files,
                    currentFileName = name,
                    isDirty = false,
                )
            }
        }
    }

    fun loadSource(name: String) {
        viewModelScope.launch(Dispatchers.Default) {
            val content = storage.load(name) ?: return@launch
            cancelRunJobAndWait()
            autoAssembleJob?.cancel()
            val (sourceCode, config) = FileFormat.decode(content)
            mutex.withLock {
                replaceHardwareConfigLocked(config)
                _uiState.update {
                    it.copy(
                        sourceCode = sourceCode,
                        currentFileName = name,
                        isDirty = false,
                        breakpoints = emptySet(),
                    )
                }
            }
            assembleNowLocked()
        }
    }

    fun loadExample(example: BuiltinExample) {
        viewModelScope.launch(Dispatchers.Default) {
            cancelRunJobAndWait()
            autoAssembleJob?.cancel()
            val config = example.buildHwConfig()
            mutex.withLock {
                replaceHardwareConfigLocked(config)
                _uiState.update {
                    it.copy(
                        sourceCode = example.sourceCode,
                        currentFileName = example.fileName,
                        isDirty = false,
                        breakpoints = emptySet(),
                    )
                }
            }
            assembleNowLocked()
        }
    }

    fun refreshSavedFiles() {
        viewModelScope.launch(Dispatchers.Default) {
            val files = storage.list().sorted()
            _uiState.update { it.copy(savedFiles = files) }
        }
    }

    fun deleteSource(name: String) {
        viewModelScope.launch(Dispatchers.Default) {
            storage.delete(name)
            val files = storage.list().sorted()
            _uiState.update { prev ->
                prev.copy(
                    savedFiles = files,
                    currentFileName = if (prev.currentFileName == name) null else prev.currentFileName,
                )
            }
        }
    }

    fun renameSource(oldName: String, newName: String) {
        viewModelScope.launch(Dispatchers.Default) {
            storage.rename(oldName, newName)
            val files = storage.list().sorted()
            _uiState.update { prev ->
                prev.copy(
                    savedFiles = files,
                    currentFileName = if (prev.currentFileName == oldName) newName else prev.currentFileName,
                )
            }
        }
    }

    suspend fun pasteSource(): Boolean {
        val text = pasteFromClipboard() ?: return false
        cancelRunJobAndWait()
        autoAssembleJob?.cancel()
        val (sourceCode, config) = FileFormat.decode(text)
        mutex.withLock {
            if (config.isNotEmpty()) {
                replaceHardwareConfigLocked(config)
            }
            _uiState.update {
                it.copy(
                    sourceCode = sourceCode,
                    currentFileName = null,
                    isDirty = true,
                    breakpoints = emptySet(),
                )
            }
        }
        assembleNowLocked()
        return true
    }

    fun shareSource() {
        val snap = _uiState.value
        val content = FileFormat.encode(snap.sourceCode, snap.hwConfig)
        if (hasPlatformShare) {
            platformShare(content, "emu_8051 Program")
        } else {
            copyToClipboard(content)
        }
    }
}
