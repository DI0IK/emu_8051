package dev.dominikstahl.emu_8051.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.dominikstahl.emu_8051.platform.hasPlatformShare
import dev.dominikstahl.emu_8051.ui.EmulatorUiState
import dev.dominikstahl.emu_8051.ui.EmulatorViewModel
import dev.dominikstahl.emu_8051.ui.SpeedMode
import dev.dominikstahl.emu_8051.ui.components.hardware.HardwarePanel
import kotlinx.coroutines.launch

@Composable
fun EmulatorScreen(
    viewModel: EmulatorViewModel = viewModel { EmulatorViewModel() },
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSaveDialog by remember { mutableStateOf(false) }
    var showLoadDialog by remember { mutableStateOf(false) }
    var showExamplesDialog by remember { mutableStateOf(false) }
    var jumpToLineRequest by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var jumpSeq by remember { mutableStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { viewModel.refreshSavedFiles() }

    val anyDialogOpen = showSaveDialog || showLoadDialog || showExamplesDialog

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .onPreviewKeyEvent { event ->
                if (anyDialogOpen || event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when {
                    event.key == Key.F5 -> {
                        if (uiState.isRunning) {
                            viewModel.pause()
                        } else {
                            val mode = if (uiState.speedMode == SpeedMode.MANUAL) SpeedMode.CUSTOM else uiState.speedMode
                            if (uiState.speedMode == SpeedMode.MANUAL) viewModel.setSpeed(mode)
                            viewModel.run(mode)
                        }
                        true
                    }
                    event.key == Key.F10 && !uiState.isRunning -> {
                        viewModel.step()
                        true
                    }
                    event.key == Key.F11 && !uiState.isRunning -> {
                        viewModel.stepOver()
                        true
                    }
                    event.isCtrlPressed && event.key == Key.R -> {
                        viewModel.reset()
                        true
                    }
                    event.isCtrlPressed && event.key == Key.S -> {
                        viewModel.refreshSavedFiles()
                        showSaveDialog = true
                        true
                    }
                    event.isCtrlPressed && event.key == Key.O -> {
                        viewModel.refreshSavedFiles()
                        showLoadDialog = true
                        true
                    }
                    else -> false
                }
            },
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isMobile = maxWidth < 980.dp

            Column(modifier = Modifier.fillMaxSize()) {
                ControlBar(
                    isRunning = uiState.isRunning,
                    speedMode = uiState.speedMode,
                    actualIps = uiState.actualIps,
                    targetIps = uiState.targetIps,
                    onStep = { viewModel.step() },
                    onStepOver = { viewModel.stepOver() },
                    onStepOut = { viewModel.stepOut() },
                    onRun = { viewModel.run(it) },
                    onPause = { viewModel.pause() },
                    onReset = { viewModel.reset() },
                    onExamples = { showExamplesDialog = true },
                    onSave = {
                        viewModel.refreshSavedFiles()
                        showSaveDialog = true
                    },
                    onLoad = {
                        viewModel.refreshSavedFiles()
                        showLoadDialog = true
                    },
                    onPaste = {
                        scope.launch {
                            if (viewModel.pasteSource()) {
                                snackbarHostState.showSnackbar("Pasted!")
                            }
                        }
                    },
                    onShare = {
                        viewModel.shareSource()
                        if (!hasPlatformShare) {
                            scope.launch { snackbarHostState.showSnackbar("Copied!") }
                        }
                    },
                    onSetTargetIps = { viewModel.setTargetIps(it) },
                    onSetSpeed = { viewModel.setSpeed(it) },
                    isSlow = uiState.isSlow,
                    isMobile = isMobile,
                )
                Spacer(Modifier.height(6.dp))

                if (isMobile) {
                    MobileLayout(
                        uiState = uiState,
                        viewModel = viewModel,
                        jumpToLineRequest = jumpToLineRequest,
                        onJumpToLine = { line -> jumpToLineRequest = line to (++jumpSeq) },
                    )
                } else {
                    DesktopLayout(
                        uiState = uiState,
                        viewModel = viewModel,
                        jumpToLineRequest = jumpToLineRequest,
                        onJumpToLine = { line -> jumpToLineRequest = line to (++jumpSeq) },
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    if (showSaveDialog) {
        SaveAsDialog(
            savedFiles = uiState.savedFiles,
            initialName = uiState.currentFileName,
            onSave = { name ->
                viewModel.saveSource(name)
                showSaveDialog = false
            },
            onDismiss = { showSaveDialog = false },
        )
    }

    if (showLoadDialog) {
        LoadFileDialog(
            savedFiles = uiState.savedFiles,
            onLoad = { name ->
                viewModel.loadSource(name)
                showLoadDialog = false
            },
            onDelete = { name -> viewModel.deleteSource(name) },
            onRename = { oldName, newName -> viewModel.renameSource(oldName, newName) },
            onDismiss = { showLoadDialog = false },
        )
    }

    if (showExamplesDialog) {
        ExamplesDialog(
            onSelectExample = { example ->
                viewModel.loadExample(example)
                showExamplesDialog = false
            },
            onDismiss = { showExamplesDialog = false },
        )
    }
}

@Composable
private fun ColumnScope.DesktopLayout(
    uiState: EmulatorUiState,
    viewModel: EmulatorViewModel,
    jumpToLineRequest: Pair<Int, Int>?,
    onJumpToLine: (Int) -> Unit,
) {
    var optimalWidth by remember { mutableStateOf(420.dp) }
    val errorLines = remember(uiState.assemblyErrors) {
        uiState.assemblyErrors.map { it.line }.filter { it > 0 }.toSet()
    }

    Row(
        modifier = Modifier.fillMaxWidth().weight(1f),
    ) {
        Column(
            modifier = Modifier.width(300.dp).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            CpuInfoPanel(state = uiState)
            HardwarePanel(
                p0 = uiState.p0,
                p1 = uiState.p1,
                p2 = uiState.p2,
                p3 = uiState.p3,
                hwConfig = uiState.hwConfig,
                snapshots = uiState.componentSnapshots,
                onUserInput = { viewModel.handleUserInput(it) },
                onAddComponent = { viewModel.addHwComponent(it) },
                onRemoveComponent = { viewModel.removeHwComponent(it) },
                onUpdateComponent = { id, transform -> viewModel.updateHwComponent(id, transform) },
                modifier = Modifier.weight(1f),
            )
        }

        Column(
            modifier = Modifier.weight(1f).fillMaxHeight().padding(horizontal = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SourceEditor(
                text = uiState.sourceCode,
                onTextChange = { viewModel.updateSource(it) },
                currentLine = uiState.currentLine,
                errorLines = errorLines,
                breakpoints = uiState.breakpoints,
                jumpToLineRequest = jumpToLineRequest,
                onToggleBreakpoint = { viewModel.toggleBreakpoint(it) },
                modifier = Modifier.weight(1f),
            )
            AssemblyStatusBar(
                errors = uiState.assemblyErrors,
                warnings = uiState.assemblyWarnings,
                isProgramLoaded = uiState.isProgramLoaded,
                currentFileName = uiState.currentFileName,
                isDirty = uiState.isDirty,
                onJumpToLine = onJumpToLine,
            )
        }

        MemoryViewer(
            snapshots = uiState.memorySnapshots,
            pc = uiState.pc,
            sp = uiState.sp,
            onWidthCalculated = { optimalWidth = it },
            overrideBytesPerLine = 8,
            modifier = Modifier.width(optimalWidth).fillMaxHeight(),
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ColumnScope.MobileLayout(
    uiState: EmulatorUiState,
    viewModel: EmulatorViewModel,
    jumpToLineRequest: Pair<Int, Int>?,
    onJumpToLine: (Int) -> Unit,
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Editor", "CPU", "Hardware", "Memory")
    val stateHolder = rememberSaveableStateHolder()
    val errorLines = remember(uiState.assemblyErrors) {
        uiState.assemblyErrors.map { it.line }.filter { it > 0 }.toSet()
    }

    Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) },
                )
            }
        }
        Spacer(Modifier.height(6.dp))

        BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f)) {
            stateHolder.SaveableStateProvider(selectedTab) {
                when (selectedTab) {
                    0 -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            SourceEditor(
                                text = uiState.sourceCode,
                                onTextChange = { viewModel.updateSource(it) },
                                currentLine = uiState.currentLine,
                                errorLines = errorLines,
                                breakpoints = uiState.breakpoints,
                                jumpToLineRequest = jumpToLineRequest,
                                onToggleBreakpoint = { viewModel.toggleBreakpoint(it) },
                                modifier = Modifier.weight(1f),
                            )
                            AssemblyStatusBar(
                                errors = uiState.assemblyErrors,
                                warnings = uiState.assemblyWarnings,
                                isProgramLoaded = uiState.isProgramLoaded,
                                currentFileName = uiState.currentFileName,
                                isDirty = uiState.isDirty,
                                onJumpToLine = onJumpToLine,
                            )
                        }
                    }
                    1 -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            CpuInfoPanel(
                                state = uiState,
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                            )
                        }
                    }
                    2 -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            HardwarePanel(
                                p0 = uiState.p0,
                                p1 = uiState.p1,
                                p2 = uiState.p2,
                                p3 = uiState.p3,
                                hwConfig = uiState.hwConfig,
                                snapshots = uiState.componentSnapshots,
                                onUserInput = { viewModel.handleUserInput(it) },
                                onAddComponent = { viewModel.addHwComponent(it) },
                                onRemoveComponent = { viewModel.removeHwComponent(it) },
                                onUpdateComponent = { id, transform -> viewModel.updateHwComponent(id, transform) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    3 -> {
                        MemoryViewer(
                            snapshots = uiState.memorySnapshots,
                            pc = uiState.pc,
                            sp = uiState.sp,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}
