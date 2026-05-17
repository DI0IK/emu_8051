# emu_8051

A Kotlin Multiplatform 8051 microcontroller emulator with a Compose Multiplatform UI. Targets Android, Desktop (JVM), Web (JS/Wasm), and iOS.

## Features

- **Full 8051 ISA** — all 256 opcodes implemented with accurate timing, flag handling, and interrupt interaction
- **Timer/Counter** — Timer 0 (modes 0–3), Timer 1 (modes 0–2), Timer 2 (capture/reload/baud-rate/clock-out), gate control
- **Interrupt System** — 6 interrupt sources with two-level priority and edge/level triggering
- **Built-in assembler** — Intel ASM51 syntax with two-pass assembly, expression evaluation, forward references, and predefined SFR/bit symbols
- **Hardware peripherals** — attach HD44780 LCD, 7-segment display, LED bar, single LED, toggle switches, matrix keypad, UART terminal, stepper motor, buzzer, and servo motor to port pins
- **Debugging** — single-step, line breakpoints, register/memory/timer/serial viewers, source-level PC tracking
- **Speed control** — manual step, custom IPS target (with presets), and unlimited mode
- **Responsive UI** — desktop 3-panel layout, mobile tabbed layout (Editor/CPU/Hardware/Memory)
- **Cross-platform** — desktop app via JVM, web via Wasm/JS, native mobile via Android/iOS

## Running

```sh
# Desktop (JVM)
./gradlew :desktopApp:run

# Desktop with hot reload
./gradlew :desktopApp:hotRun --auto

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
  ui/           — Compose Multiplatform UI
    components/ — screen panels (source editor, register view, memory viewer, control bar)
    hardware/   — 10 peripheral components (HD44780 LCD, 7-segment, LED, LED bar, toggle, matrix keypad, UART terminal, stepper, buzzer, servo)
    EmulatorViewModel.kt, EmulatorUiState.kt
  App.kt        — shared Compose entry point

Platform modules:
  androidApp/   — Android MainActivity
  desktopApp/   — JVM Compose Window
  webApp/       — JS/Wasm entry point
  iosApp/       — SwiftUI wrapper
```

## Tech Stack

- Kotlin 2.3.21, Gradle 9.1.0
- Compose Multiplatform 1.11.0
- Kotlin Multiplatform targeting JVM, Android, JS, Wasm, iOS
