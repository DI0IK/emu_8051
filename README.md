# emu_8051

A Kotlin Multiplatform 8051 microcontroller emulator with a Compose Multiplatform UI. Targets Android, Desktop (JVM), Web (Wasm/JS), and iOS.

## Features

- **Full 8051 ISA** — all 256 opcodes implemented with accurate timing, flag handling, and interrupt interaction
- **Timer/Counter** — Timer 0 (modes 0–3), Timer 1 (modes 0–2), Timer 2 (capture/reload/baud-rate/down-counter/clock-out), gate control
- **Interrupt System** — 6 interrupt sources with two-level priority, preemption blocking, and edge/level triggering
- **Built-in assembler** — Intel ASM51 syntax with two-pass assembly, expression evaluation (arithmetic, bitwise, shifts), forward references, predefined SFR/bit symbols, and inline error/warning reporting
- **14 hardware peripherals** — HD44780 LCD, 7-segment display, multi-digit multiplexed 7-segment display, LED, LED bar, 8×8 LED matrix, toggle switch, push button, 8-bit DIP switch, matrix keypad (2×2, 4×4, 8×8), UART terminal (TX/RX), stepper motor, buzzer (audio + visual), and servo motor (PWM)
- **Built-in examples** — load 14 pre-wired hardware examples directly from the toolbar
- **Debugging** — step into, step over, step out, line breakpoints, active register bank R0–R7, ROM/RAM/XRAM/SFR viewers with Go-to-address & PC/SP highlighting, timer/interrupt/serial viewers, and keyboard shortcuts (`F5`, `F10`, `F11`, `Ctrl+R`, `Ctrl+S`, `Ctrl+O`)
- **Speed control** — manual step, fractional custom cycles/sec target (with presets), and unlimited mode
- **Responsive UI** — desktop 3-panel layout (`1280×800` default), mobile tabbed layout (Editor/CPU/Hardware/Memory)
- **Cross-platform** — desktop app via JVM, web via Wasm/JS, native mobile via Android/iOS

## Running

```sh
# Desktop (JVM)
./gradlew :desktopApp:run

# Web (Wasm — faster, modern browsers)
./gradlew :webApp:wasmJsBrowserDevelopmentRun

# Web (JS — legacy browser support)
./gradlew :webApp:jsBrowserDevelopmentRun

# Android
./gradlew :androidApp:assembleDebug

# iOS
# Open iosApp/ in Xcode and run from there
```

## Tests

```sh
# Desktop
./gradlew :shared:jvmTest

# Web (Wasm)
./gradlew :shared:wasmJsTest

# Web (JS)
./gradlew :shared:jsTest

# Android
./gradlew :shared:testAndroidHostTest

# iOS
./gradlew :shared:iosSimulatorArm64Test
```

## Architecture

```
shared/src/commonMain/kotlin/
  asm/          — ASM51 assembler (tokenizer, parser, two-pass encoder)
  engine/       — core emulator
    ArithmeticOps.kt, BitOps.kt, DataMovementOps.kt, ...
    CpuState.kt, Interpreter.kt, Instruction.kt
    InterruptController.kt, TimerController.kt
  examples/     — 14 built-in pre-wired ASM examples
  platform/     — file storage, clipboard/share, buzzer audio per target
  ui/           — Compose Multiplatform UI
    components/ — screen panels (source editor, status bar, CPU info, memory viewer, control bar)
    hardware/   — 14 peripheral components
    EmulatorViewModel.kt, EmulatorUiState.kt
  App.kt        — shared Compose entry point

Platform modules:
  androidApp/   — Android MainActivity
  desktopApp/   — JVM Compose Window
  webApp/       — Wasm/JS entry point
  iosApp/       — SwiftUI wrapper
```

## Tech Stack

- Kotlin 2.3.21, Gradle 9.1.0
- Compose Multiplatform 1.11.0
- Kotlin Multiplatform targeting JVM, Android, JS, Wasm, iOS
