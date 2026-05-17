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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.dominikstahl.emu_8051.platform.hasPlatformShare
import dev.dominikstahl.emu_8051.ui.EmulatorViewModel
import dev.dominikstahl.emu_8051.ui.SpeedMode
import dev.dominikstahl.emu_8051.ui.components.hardware.HardwarePanel
import kotlinx.coroutines.launch

@Composable
fun EmulatorScreen() {
    val viewModel = remember { EmulatorViewModel() }
    val uiState by viewModel.uiState.collectAsState()
    var showSaveDialog by remember { mutableStateOf(false) }
    var showLoadDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { viewModel.refreshSavedFiles() }

    Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        // Use BoxWithConstraints to read the available screen width dynamically
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            // Desktop needs at least 300dp (CPU) + 480dp (Memory) + room for Editor (~200dp+) = ~980dp
            val isMobile = maxWidth < 980.dp

            Column(modifier = Modifier.fillMaxSize()) {
                // ControlBar stays globally accessible at the top on both Desktop and Mobile
                ControlBar(
                    isRunning = uiState.isRunning,
                    speedMode = uiState.speedMode,
                    actualIps = uiState.actualIps,
                    targetIps = uiState.targetIps,
                    onStep = { viewModel.step() },
                    onRun = { viewModel.run(it) },
                    onPause = { viewModel.pause() },
                    onReset = { viewModel.reset() },
                    onSave = {
                        viewModel.refreshSavedFiles()
                        showSaveDialog = true
                    },
                    onLoad = {
                        viewModel.refreshSavedFiles()
                        showLoadDialog = true
                    },
                    onCopy = {
                        viewModel.copySource()
                        scope.launch { snackbarHostState.showSnackbar("Copied!") }
                    },
                    onShare = if (hasPlatformShare) viewModel::shareSource else null,
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
                    )
                } else {
                    DesktopLayout(
                        uiState = uiState,
                        viewModel = viewModel,
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
            onDelete = { name ->
                viewModel.deleteSource(name)
            },
            onRename = { oldName, newName ->
                viewModel.renameSource(oldName, newName)
            },
            onDismiss = { showLoadDialog = false },
        )
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
@Composable
private fun ColumnScope.DesktopLayout(
    uiState: dev.dominikstahl.emu_8051.ui.EmulatorUiState,
    viewModel: EmulatorViewModel,
) {
    var optimalWidth by remember { mutableStateOf(650.dp) }

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
        ) {
            SourceEditor(
                text = uiState.sourceCode,
                onTextChange = { viewModel.updateSource(it) },
                currentLine = uiState.currentLine,
                breakpoints = uiState.breakpoints,
                onToggleBreakpoint = { viewModel.toggleBreakpoint(it) },
                modifier = Modifier.weight(1f),
            )
        }

        MemoryViewer(
            getRom = { viewModel.getRomForDisplay() },
            getRam = { viewModel.getRamForDisplay() },
            getSfr = { viewModel.getSfrForDisplay() },
            stateKey = Pair(uiState.assemblyVersion, uiState.totalCycles),
            onWidthCalculated = { optimalWidth = it },
            overrideBytesPerLine = 8,
            modifier = Modifier.width(optimalWidth).fillMaxHeight(),
        )
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
@Composable
private fun ColumnScope.MobileLayout(
    uiState: dev.dominikstahl.emu_8051.ui.EmulatorUiState,
    viewModel: EmulatorViewModel,
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Editor", "CPU", "Hardware", "Memory")

    Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }
        Spacer(Modifier.height(6.6.dp))

        BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f)) {
            when (selectedTab) {
                0 -> { // Editor Tab
                    Column(modifier = Modifier.fillMaxSize()) {
                        SourceEditor(
                            text = uiState.sourceCode,
                            onTextChange = { viewModel.updateSource(it) },
                            currentLine = uiState.currentLine,
                            breakpoints = uiState.breakpoints,
                            onToggleBreakpoint = { viewModel.toggleBreakpoint(it) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                1 -> { // CPU Status Tab
                    Column(modifier = Modifier.fillMaxSize()) {
                        CpuInfoPanel(
                            state = uiState,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                        )
                    }
                }
                2 -> { // Hardware Peripherals Tab
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
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                3 -> { // Hex Memory Dump Tab
                    MemoryViewer(
                        getRom = { viewModel.getRomForDisplay() },
                        getRam = { viewModel.getRamForDisplay() },
                        getSfr = { viewModel.getSfrForDisplay() },
                        stateKey = Pair(uiState.assemblyVersion, uiState.totalCycles),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}