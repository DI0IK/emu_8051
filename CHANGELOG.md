# Changelog

## v2.0.0 — 2026-11-20

### Added

- **Buzzer Audio Synthesis** — cross-platform square-wave audio output (JVM `javax.sound`, Android `AudioTrack`, Web Audio API on JS/WasmJS) with live frequency detection and per-component mute toggle
- **3 New Hardware Peripherals** — Momentary Push Button (`PUSH_BUTTON`, defaults to `P3.2`/`INT0`), 8-bit DIP Switch bank (`DIP_SWITCH`), and 2/4-digit Multiplexed 7-Segment Display (`MULTI_SEVEN_SEG`)
- **Built-in Examples Browser** — one-click loading of all 14 examples with pre-wired peripheral configs from the ControlBar
- **Debugger Upgrades** — Step Over (`F11`), Step Out, active R0–R7 register bank viewer, XRAM (64KB) tab, Go-to-address & PC/SP highlighting in Memory Viewer, `AssemblyStatusBar` error/warning navigation, and desktop keyboard shortcuts (`F5`/`F10`/`F11`/`Ctrl+R`/`Ctrl+S`/`Ctrl+O`)
- **Pin-Conflict Detection & Polarity Toggle** — `HardwarePanel` detects shared port/pin conflicts across enabled components, adds per-component enable checkboxes, and supports Active HIGH / Active LOW polarity
- **Automated Test Suite** — comprehensive unit & integration tests covering the CPU engine, timers, interrupts, assembler, peripherals, `FileFormat`, and end-to-end example programs

### Fixed

- **CPU Engine** — fixed `CpuState.reset()` clearing SFR/RAM/XRAM, `init { reset() }`, `MOVX @Ri` P2 paging, `LJMP` at `0xFFFE`, `PUSH SP` increment order, `POP` below `0x08`, `JBC` latch read, read-only `PSW.0` parity, and physically separated `SBUF` TX/RX buffers
- **Interrupt Controller** — fixed `INT1` overwriting `IE0` in `sample()`, falling-edge `INT0`/`INT1` polarity, two-pass priority arbitration, and blocking low-priority preemption of high-priority ISRs
- **Timer Controller** — fixed `GATE` polarity (runs when `INTx` is HIGH), falling-edge counter/capture polarity, spurious counter overflows when `TL == 0`, Mode 0 13-bit wrap, Timer 0 Mode 3 + Timer 1 interaction, and Timer 2 `DCEN` up/down & post-tick capture
- **ASM51 Assembler** — fixed `MOV direct, direct` (`0x85`) operand encoding order, `JMP @A+DPTR`, binary/octal/decimal literals, RAM bit-dot (`20h.x`) addressing, strict Pass 2 undefined-symbol errors, bitwise/shift operators (`AND`/`OR`/`XOR`/`SHL`/`SHR`), chained unary ops, `DB 'A'+1`, case-insensitive symbol table, and range checks
- **Hardware Peripherals** — fixed `BuzzerFactory`/`ServoFactory`/`StepperFactory` missing `needsTick(config) = true`, HD44780 128-byte DDRAM & cursor/display flags, LED Matrix invisible `offColor` & cycle-based persistence, Servo arc/arm geometry, Stepper coil indicators & orientation, Matrix Keypad `8×8` two-port support & stuck-key fixes, UART multi-char RX queueing, and Toggle Switch state/pin release
- **UI, Concurrency & Cross-Platform** — fixed syntax highlighter offset-0 crash, `CpuInfoPanel` `Gate0` & swapped `TCON` masks, `EmulatorViewModel` atomic `StateFlow` updates, fractional low-CPS throttling, `UNLIMITED` Web responsiveness, `LazyColumn` item key reuse, Desktop default window size (`1280×800`), Android dark splash theme & safe clipboard/preview fallback, iPad share popover crash, and CI fork-PR builds

## v1.2.0 — 2026-05-18

### Added

- **LED Matrix component** — 8×8 display with brightness decay (persistence of vision), active HIGH polarity, configurable row and column ports
- **Example programs** — `examples/` folder with 11 ASM files demonstrating every hardware component: LED blink, LED bar wave, 7-segment counter, toggle switch, matrix keypad, HD44780 LCD, UART terminal, stepper motor, buzzer, servo PWM, LED matrix "A"
- **Assembler warnings** — `AssemblyResult` now tracks warnings alongside errors (e.g. unknown symbol usage)

### Changed

- **`HwComponent.tick()` now accepts `List<Int>`** — refactored from single `Int` to support multi-port components (LED Matrix, Matrix Keypad)
- **`HwComponentFactory.portCount()`** — new method lets components declare how many ports they need; `EmulatorViewModel.tickComponents()` passes all relevant port values per component
- **All component tick() overrides updated** — keypad, LCD, buzzer, servo, stepper now use `portValues.firstOrNull()`
- **PC bounds to 16-bit** — `CpuState.pc` setter now masks with `0xFFFF` to prevent out-of-range values
- **Timer interrupt flag auto-clear** — `InterruptController` now correctly clears TF0/TF1 on timer interrupt dispatch
- **Assembler unknown-token handling** — `Expr.kt` treats unknown tokens as symbols instead of silent `Number(0)`, preventing infinite parse loops and enabling syntax highlighting for undefined symbols
- **Active HIGH polarity** — all hardware components use active HIGH (`1 = on`) convention, consistent with standard 8051 practice

### Fixed

- **Expr.kt infinite loop** — unknown tokens no longer return `Expr.Number(0)` at the same position, eliminating the parsing stall

## v1.1.0 — 2026-05-17

### Added

- **Paste from clipboard** — new Paste button reads clipboard content, parsing embedded metadata (hardware config header `; #emu_8051 <json>`) to restore both source code and peripheral configuration
- **Share always visible** — Share button now appears on all platforms; falls back to copying to clipboard on desktop/web

### Changed

- **Copy button replaced with Paste** — `ContentCopy` icon swapped for `ContentPaste`
- **Share fallback** — on platforms without native share sheet (JVM, JS, WasmJS), Share copies the encoded program to clipboard with a "Copied!" confirmation

### Platform support

- `pasteFromClipboard` implemented on all 5 targets: JVM (AWT), Android (ClipboardManager), iOS (UIPasteboard), JS (navigator.clipboard.readText), WasmJS (navigator.clipboard.readText)

## v1.0.1 — 2026-05-17

### Fixed

- **CI build time regression** — replaced `compose-materialIconsExtended` (37 MB) with `material-icons-core` + custom SVG icon definitions, reducing JS/WasmJS distribution builds from >30 min to ~1 min

## v1.0.0 — 2026-05-17

Initial release of emu_8051, a Kotlin Multiplatform 8051 microcontroller emulator
with a Compose Multiplatform UI.

### Features

- **Full 8051 ISA** — all 256 opcodes implemented with accurate timing, flag handling, and interrupt interaction
- **Timer/Counter** — Timer 0 (modes 0–3), Timer 1 (modes 0–2), Timer 2 (capture/reload/baud-rate/clock-out), gate control
- **Interrupt System** — 6 interrupt sources, two-level priority, edge/level triggering, RETI handling
- **Built-in Assembler** — ASM51 syntax with two-pass assembly, expression evaluation, forward references, ORG/END/EQU/DATA/BIT/CODE/DB/DW/DS directives, and predefined SFR/bit symbols
- **Source-Level Debugging** — single-step execution, line breakpoints, current-line tracking, register/memory/timer/serial viewers
- **Speed Control** — manual step, custom IPS with presets (10–12M), unlimited mode, performance feedback with SLOW indicator
- **10 Hardware Peripherals** — HD44780 LCD (8/16/20/40 cols × 1/2/4 lines), 7-segment display, LED bar, single LED, toggle switches, matrix keypad (configurable N×N), UART terminal, stepper motor, buzzer, servo motor (PWM decoding)
- **Syntax-Highlighted Editor** — tokenizer-based coloring for mnemonics, registers, directives, numbers, strings, comments
- **File Management** — save/load/rename/delete `.asm` files with embedded hardware configuration
- **Platform Utilities** — copy to clipboard, system share sheet (Android/iOS)
- **Responsive UI** — desktop 3-panel layout, mobile tabbed layout (Editor/CPU/Hardware/Memory)
- **Dark Theme** — Material 3 dark color scheme throughout

### Platforms

| Platform | Format |
|----------|--------|
| Android | APK (minSdk 24, targetSdk 36) |
| Desktop Linux | DEB + portable tar.gz |
| Desktop Windows | MSI + portable zip |
| Desktop macOS | DMG + portable tar.gz |
| Web | Wasm + JS (deployed to GitHub Pages) |
| iOS | Xcode project (requires macOS build) |

### Notes

- 44 assembler tests covering all instruction forms, directives, and edge cases
- Requires Java 21 for building
- Android release signing requires keystore configuration (see CI secrets)
- iOS builds require Xcode on macOS
