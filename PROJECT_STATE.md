# emu_8051 — Project State

## Overview

A Kotlin Multiplatform 8051 microcontroller emulator targeting Android, Desktop (JVM), Web (JS/Wasm), and iOS. Uses Compose Multiplatform for UI and Gradle 9.1.0 with Kotlin 2.3.21.

## Architecture

```
emu_8051
+-- shared/                 # Emulator engine + shared Compose UI
|   +-- asm/                # Built-in assembler
|   |   +-- Assembler.kt    # Tokenizer, parser, expression evaluator, two-pass assembler
|   +-- engine/             # Core emulator
|   |   +-- CpuState.kt     # CPU state: ROM/RAM/XRAM, registers, flags, memory access helpers
|   |   +-- Instruction.kt  # 111 instruction definitions, OpcodePattern enum, lookup tables
|   |   +-- InstructionTable.kt  # 256-entry dispatch table builder
|   |   +-- Interpreter.kt  # step() fetches opcode, dispatches via table, accumulates cycles
|   |   +-- InterruptController.kt  # 5-source interrupt system (INT0,1; T0,1; UART; T2)
|   |   +-- TimerController.kt  # Timer 0/1/2 with all modes (13/16-bit, auto-reload, baud gen, capture)
|   |   +-- ops/            # 22 instruction handler classes
|   +-- App.kt              # Shared Compose entry point (MaterialTheme)
+-- androidApp/             # Android: MainActivity hosts App()
+-- desktopApp/             # Desktop: Compose Window hosts App()
+-- webApp/                 # Web: ComposeViewport hosts App()
+-- iosApp/                 # iOS: SwiftUI wraps MainViewController from shared
```

## Emulator Engine

### CpuState (`CpuState.kt`)
- 64KB ROM, 256B internal RAM, 64KB external XRAM, 128B SFR space
- Typed register properties: P0-P3, SP, DPL/DHP/DPTR, PCON, TCON, TMOD, TL0/1, TH0/1, AUXR, CKCON, SCON, SBUF, IE, IP, T2CON, RCAP2L/H, TL2, TH2, T2MOD, PSW, ACC, B
- Program counter `pc`, `reset()`, `updateParity()`
- `readDirect`/`writeDirect` and `readIndirect`/`writeIndirect` helpers
- References to `InterruptController` and `TimerController` (set by `Interpreter`)
- `interruptControllerAccessFlag` tracking for IE/IP writes

### Instruction Definitions (`Instruction.kt`)
- Sealed class `OpcodePattern` with four variants: `Fixed`, `RegisterRange`, `IndirectRange`, `PageRange`
- `Operand` enum: A, C, AB, DPTR, RN, RI, DIRECT, IMM8, IMM16, REL, ADDR11, ADDR16, BIT, NOT_BIT, AT_DPTR, AT_A_DPTR, AT_A_PC
- `Instruction` enum: ~111 entries covering the full 8051 ISA, each with mnemonic, bytes, cycles, operands, and opcode pattern
- Companion object provides `byOpcode` (256-entry array), `byMnemonic` (grouped map), `lookup(mnemonic, operands)`

### Dispatch Table (`InstructionTable.kt`)
- 256-element `Array<(Int) -> Int>` for O(1) opcode dispatch
- `register(handler)` auto-maps each handler's `handledInstructions` into dispatch entries
- Default entry for unhandled opcodes: advance PC by 1, return 1 cycle

### Interpreter (`Interpreter.kt`)
- Registers all 22 handlers into `InstructionTable` at construction
- Creates and wires `InterruptController` and `TimerController`
- `step()` reads opcode from `rom[pc]`, dispatches, calls `timerController.tick()` per cycle, samples/polls interrupts, returns total cycles consumed

### Interrupt Controller (`InterruptController.kt`)
- 5 interrupt sources: INT0, T0, INT1, T1, UART, T2
- Two priority levels (low/high) with in-service tracking
- `sample()` — samples external interrupt pins (P3.2/INT0, P3.3/INT1), edge/level-triggered
- `poll()` — checks pending interrupts against enable/priority, acknowledges highest priority
- RETI support via `clearInService()`
- Auto-clears edge-triggered flags on acknowledge

### Timer Controller (`TimerController.kt`)
- Timer 0: modes 0-3 (13-bit, 16-bit, 8-bit auto-reload, dual 8-bit), gate/counter select
- Timer 1: modes 0-2 (mode 3 = baud rate only, TH0 takes over TF1)
- Timer 2: capture/reload with up/down counting, baud rate generator, clock-out (T2OE)
- `tick()` called once per cycle by Interpreter
- External clock input via T0/T1/T2 pins, falling-edge detection

### Instruction Handler Base (`InstructionHandler.kt`)
- Abstract class with `handledInstructions: Set<Instruction>` and `handle(instruction, rawOpcode)`
- Helpers: `pushStack`, `popStack`, `setFlag`, `getRegister`, `setRegister`, `getBit`, `setBit`, `complementBit`
- PSW bit constants: CY, AC, F0, RS1, RS0, OV, F1, P
- SFR and bit constants for TCON, SCON, IE, IP, T2CON, T2MOD

## Instruction Handlers (22 total)

| Handler | Instructions |
|---------|-------------|
| `ADDCHandler` | ADD A, ADDC A (all addressing modes) |
| `AJMPHandler` | AJMP |
| `ANLHandler` | ANL A/dir/C (all modes, inc. /bit) |
| `BitHandler` | CLR, SETB, CPL (A, C, bit) |
| `CJNEHandler` | CJNE (all modes) |
| `DAHandler` | DA A |
| `DECHandler` | DEC (all modes) |
| `DIVHandler` | DIV AB |
| `DJNZHandler` | DJNZ dir/rel, Rn/rel |
| `INCHandler` | INC (all modes, inc. DPTR) |
| `JumpHandler` | LJMP, JC, JNC, JZ, JNZ, JB, JNB, JBC, JMP @A+DPTR |
| `MOVHandler` | MOV (all internal modes), MOVC, MOVX (all variants) |
| `MULHandler` | MUL AB |
| `NOPHandler` | NOP |
| `ORLHandler` | ORL A/dir/C (all modes, inc. /bit) |
| `RotateHandler` | RR, RRC, RL, RLC |
| `SJMPHandler` | SJMP |
| `StackHandler` | ACALL, LCALL, RET, RETI, PUSH, POP |
| `SUBBHandler` | SUBB (all modes) |
| `SWAPHandler` | SWAP A |
| `XCHHandler` | XCH A (all modes), XCHD A |
| `XRLHandler` | XRL A/dir (all modes) |

## Assembler (`asm/Assembler.kt`)

A single-file internal assembler converting Intel ASM51 assembly source to a 64KB `UByteArray` ROM for the emulator.

### Features
- **Two-pass design** supporting forward references (labels, EQU with forward refs)
- **Intel ASM51 syntax**: `DATA`/`BIT`/`CODE` directives, `;` comments, `h`-suffixed hex, `#` immediate, `@R0`/`@R1` indirect, `SFR.bit` dot notation, labels, standard mnemonics
- **Expression evaluator**: hex (`0x`, `h` suffix), decimal, binary (`b` suffix), `$` (PC), `+`, `-`, `*`, `/`, `MOD`, `AND`, `OR`, `XOR`, `NOT`, `SHR`, `SHL`, `HIGH`, `LOW`, parentheses, `.` (bit select = `byte_addr | bit_index`), char/string literals
- **Auto-disambiguation**: tries all operand type combinations against `Instruction.lookup()` to resolve ambiguous bare numbers (DIRECT vs BIT vs REL vs ADDR11 vs ADDR16)
- **Predefined symbols**: all 33+ standard SFR addresses and 48+ bit-name symbols
- **Output**: `AssemblyResult` sealed type — `Success(rom: UByteArray)` or `Failure(errors: List<String>)`

### Directives
| Directive | Description |
|-----------|-------------|
| `ORG addr` | Set origin/orientation point |
| `EQU expr` | Define symbol value |
| `DATA expr` | Define symbol as data address |
| `BIT expr` | Define symbol as bit address |
| `CODE expr` | Define symbol in code space |
| `DB expr,...` | Emit bytes |
| `DW expr,...` | Emit words (big-endian) |
| `DS expr` | Reserve storage bytes |
| `END` | End of source |

## Platform Modules

### Android
- `MainActivity.kt` — `ComponentActivity` with `enableEdgeToEdge()`, renders `App()`
- minSdk 24, targetSdk 36, compileSdk 36
- Material Light NoActionBar theme

### Desktop
- `main.kt` — Compose `Window("emu_8051")` running `App()`
- Native distribution targets: DMG, MSI, DEB

### Web
- `main.kt` — `ComposeViewport` rendering `App()`
- Built for both JS and Wasm JS browser targets

### iOS
- `MainViewController.kt` (shared/iosMain) — `ComposeUIViewController { App() }`
- `iOSApp.swift` — SwiftUI `@main` entry
- `ContentView.swift` — wraps shared controller via `UIViewControllerRepresentable`

## Tests

Run with: `./gradlew :shared:<target>Test` (e.g. `jvmTest`, `wasmJsTest`, `androidHostTest`)

### Existing stub tests (placeholder `assertEquals(3, 1 + 2)`):
- `commonTest` — `SharedCommonTest`
- `jvmTest` — `SharedLogicDesktopTest`
- `iosTest` — `SharedLogicIOSTest`
- `androidHostTest` — `SharedLogicAndroidHostTest`

### Assembler tests (41 tests, all passing):
- `commonTest` — `AssemblerTest` covering:
  - All instruction categories (arithmetic, logical, MOV, jumps, calls, bit ops, MOVC/MOVX, rotate, exchange, stack)
  - All addressing modes (immediate, direct, register, indirect, @A+DPTR, @A+PC)
  - Directives (EQU, DATA, BIT, CODE, ORG, END, DB, DW, DS)
  - Forward references (labels, EQU forward refs)
  - Bit addressing (SFR.bit, predefined bit symbols)
  - NOT_BIT (/bit) operands
  - Expressions in operands
  - `SJMP $` infinite loop pattern
  - Comments, END directive, full small program
  - Edge cases (ORG ordering, label+instruction on same line)

## UI

The shared UI (`App.kt`) renders `MaterialTheme { Text("Hello World") }` — no emulator controls built yet.

## Build Configuration

- Gradle 9.1.0, Kotlin 2.3.21, AGP 9.0.0-alpha06, Compose Multiplatform 1.11.0
- Version catalog at `gradle/libs.versions.toml`
- Configuration caching enabled
