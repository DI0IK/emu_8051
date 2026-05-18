# 8051 Hardware Examples

Example assembly programs demonstrating each hardware component in the emulator.

## Quick Start

1. Open the emulator
2. Copy the contents of any `.asm` file into the source editor
3. Click **Assemble** — fix any errors if needed
4. Click **Run**
5. Add the corresponding component from the **Hardware** panel and connect it to the matching port(s)

## Examples

| # | File | Component | Description |
|---|------|-----------|-------------|
| 01 | `01_led/led.asm` | LED | Blinks a single LED on P1.7 using Timer 0 interrupt |
| 02 | `02_ledbar/ledbar.asm` | LED Bar | Knight rider wave pattern across 8 LEDs on P1 |
| 03 | `03_sevenseg/sevenseg.asm` | 7-Segment | Counts 0–9 on a single-digit display on P2 |
| 04 | `04_toggle/toggle.asm` | Toggle Switch | Reads a toggle switch on P1.0 and toggles LED on P1.7 |
| 05 | `05_keypad/keypad.asm` | Matrix Keypad | Polls a 4×4 keypad and outputs the key number to P2 |
| 06 | `06_lcd/lcd.asm` | HD44780 LCD | Initializes a 16×2 LCD and prints "HELLO" |
| 07 | `07_uart/uart.asm` | UART Terminal | Sends "HELLO" via serial UART |
| 08 | `08_stepper/stepper.asm` | Stepper Motor | Rotates a 4-phase stepper motor via 4-step sequence on P1 |
| 09 | `09_buzzer/buzzer.asm` | Buzzer | Toggles a buzzer at ~1kHz using Timer 0 |
| 10 | `10_servo/servo.asm` | Servo Motor | Generates PWM pulses to control a servo on P1.0 |
| 11 | `11_ledmatrix/ledmatrix.asm` | LED Matrix | Scans an 8×8 matrix and displays the letter "A" |

## Default Port Configuration

All examples use the default component configuration. Wiring notes are included in each file's header comment.

| Component | Ports | Pin/Notes |
|-----------|-------|-----------|
| LED | P1 | Pin 7 |
| LED Bar | P1 | All 8 pins |
| 7-Segment | P2 | All 8 pins (a–g + DP) |
| Toggle Switch | P1 | Pin 0 |
| Matrix Keypad | P1 | Rows P1.0–P1.3, Cols P1.4–P1.7 |
| HD44780 LCD | P2 | 4-bit mode: P2.0–P2.7 |
| UART Terminal | P3.1 | TX pin |
| Stepper Motor | P1 | Lower nibble P1.0–P1.3 |
| Buzzer | P1 | Pin 0 |
| Servo | P1 | Pin 0 |
| LED Matrix | P1/P2 | Rows P1.0–P1.7, Cols P2.0–P2.7 |

## Timing Notes

All examples target a **12MHz crystal**. Timer values and delays are calibrated accordingly. If running on a different frequency, adjust TH/TL reload values proportionally (e.g., divide by 2 at 6MHz, multiply by 2 at 24MHz).