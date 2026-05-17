# Changelog

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
