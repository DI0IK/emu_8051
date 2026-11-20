# 8051 Hardware Examples

Example assembly programs demonstrating every hardware component in the emulator.

## Quick Start

1. Open the emulator and click **Examples** in the top bar (or copy any `.asm` file into the editor)
2. Select an example — the source code and matching hardware peripherals load automatically
3. Choose a speed (e.g. `100K` or `1M` IPS) and click **Run**

## Examples

| # | File | Component | Description |
|---|------|-----------|-------------|
| 01 | `01_led/led.asm` | LED | Blinks a single LED on P1.7 using Timer 0 interrupt |
| 02 | `02_ledbar/ledbar.asm` | LED Bar | Knight rider wave pattern across 8 LEDs on P1 |
| 03 | `03_sevenseg/sevenseg.asm` | 7-Segment | Counts 0–9 on a single-digit display on P2 |
| 04 | `04_toggle/toggle.asm` | Toggle Switch | Reads a toggle switch on P1.0 and toggles LED on P1.7 |
| 05 | `05_keypad/keypad.asm` | Matrix Keypad | Polls a 4×4 keypad on P1 and outputs the key number to P2 |
| 06 | `06_lcd/lcd.asm` | HD44780 LCD | Initializes a 16×2 LCD on P2 and prints "HELLO" |
| 07 | `07_uart/uart.asm` | UART Terminal | Sends "HELLO" via serial UART and echoes typed characters |
| 08 | `08_stepper/stepper.asm` | Stepper Motor | Rotates a 4-phase stepper motor on P1.0–P1.3 |
| 09 | `09_buzzer/buzzer.asm` | Buzzer | Toggles a buzzer on P1.0 at ~1kHz using Timer 0 |
| 10 | `10_servo/servo.asm` | Servo Motor | Generates PWM pulses on P1.1 sweeping 0° → 90° → 180° |
| 11 | `11_ledmatrix/ledmatrix.asm` | LED Matrix | Scans an 8×8 matrix (P1 rows / P2 cols) displaying "A" |
| 12 | `12_button/button.asm` | Push Button | Uses Push Button on P3.2 (INT0) to toggle LED on P1.7 |
| 13 | `13_dipswitch/dipswitch.asm` | DIP Switch | Mirrors an 8-bit DIP switch bank on P0 to an LED bar on P1 |
| 14 | `14_multisevenseg/multisevenseg.asm` | Multi 7-Segment | Multiplexed 4-digit counter (0000–9999) on P2/P0 |

## Default Port Configuration

All examples match the built-in component defaults (Active HIGH unless noted).

| Component | Ports | Pin/Notes |
|-----------|-------|-----------|
| LED | P1 | Pin 7 |
| LED Bar | P1 | All 8 pins |
| 7-Segment | P2 | All 8 pins (a–g + DP) |
| Multi 7-Segment | P2 / P0 | Segments P2.0–P2.7, Digits P0.0–P0.3 |
| Toggle Switch | P3 (or P1 in ex. 04) | Pin 0 |
| Push Button | P3 | Pin 2 (INT0, Active LOW) |
| DIP Switch | P0 | All 8 pins |
| Matrix Keypad | P1 (or P1/P2 for 8×8) | Rows P1.0–P1.3, Cols P1.4–P1.7 |
| HD44780 LCD | P2 | 4-bit mode: RS=P2.0, RW=P2.1, E=P2.2, D4–D7=P2.4–P2.7 |
| UART Terminal | P3 | P3.0 RXD, P3.1 TXD |
| Stepper Motor | P1 | Lower nibble P1.0–P1.3 |
| Buzzer | P1 | Pin 0 |
| Servo | P1 | Pin 1 |
| LED Matrix | P1 / P2 | Rows P1.0–P1.7, Cols P2.0–P2.7 |

## Timing Notes

All examples target a **12MHz crystal** (1 µs per machine cycle).
