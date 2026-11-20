package dev.dominikstahl.emu_8051.examples

import dev.dominikstahl.emu_8051.ui.HwComponentConfig
import dev.dominikstahl.emu_8051.ui.Port

data class BuiltinExample(
    val id: String,
    val title: String,
    val subtitle: String,
    val fileName: String,
    val sourceCode: String,
    val buildHwConfig: () -> List<HwComponentConfig>,
)

object BuiltinExamples {
    val all: List<BuiltinExample> = listOf(
        BuiltinExample(
            id = "01_led",
            title = "01. Blinking LED",
            subtitle = "Timer 0 ISR blinks LED on P1.7",
            fileName = "led.asm",
            sourceCode = """
; Blink LED on P1.7
; Default config: P1.7 LED (active HIGH)

        ORG 0000h
        LJMP MAIN

        ORG 000Bh
        LJMP TIMER0_ISR

MAIN:   MOV SP, #5Fh
        MOV P1, #00h

        MOV TMOD, #01h
        MOV TH0, #0FCh
        MOV TL0, #018h
        SETB ET0
        SETB EA
        SETB TR0
        SJMP $

TIMER0_ISR:
        MOV TH0, #0FCh
        MOV TL0, #018h
        CPL P1.7
        RETI
            """.trimIndent(),
            buildHwConfig = {
                listOf(HwComponentConfig("hw_1", "P1.7 LED", "LED", ports = listOf(Port.P1), pin = 7))
            },
        ),
        BuiltinExample(
            id = "02_ledbar",
            title = "02. LED Bar Wave",
            subtitle = "Knight Rider bounce across P1 LED bar",
            fileName = "ledbar.asm",
            sourceCode = """
; Knight rider wave on P1 (8 LEDs)
; Default config: P1 LED Bar (active HIGH)

        ORG 0000h
        LJMP MAIN

        ORG 000Bh
        LJMP TIMER0_ISR

MAIN:   MOV SP, #5Fh
        MOV P1, #00h

        MOV TMOD, #01h
        MOV TH0, #0F8h
        MOV TL0, #030h
        SETB ET0
        SETB EA
        SETB TR0

        MOV A, #01h
        MOV R0, #00h
        SJMP $

TIMER0_ISR:
        MOV TH0, #0F8h
        MOV TL0, #030h
        CJNE R0, #00h, SHIFT_RIGHT

SHIFT_LEFT:
        MOV P1, A
        RL A
        JB ACC.7, TOGGLE_DIR
        SJMP DONE

SHIFT_RIGHT:
        MOV P1, A
        RR A
        JB ACC.0, TOGGLE_DIR
        SJMP DONE

TOGGLE_DIR:
        PUSH ACC
        MOV A, R0
        CJNE A, #00h, SET_LEFT
        MOV R0, #01h
        POP ACC
        SJMP DONE
SET_LEFT:
        MOV R0, #00h
        POP ACC

DONE:   RETI
            """.trimIndent(),
            buildHwConfig = {
                listOf(HwComponentConfig("hw_1", "P1 LED Bar", "LED_BAR", ports = listOf(Port.P1)))
            },
        ),
        BuiltinExample(
            id = "03_sevenseg",
            title = "03. 7-Segment Counter",
            subtitle = "Counts 0–9 on P2 7-segment display",
            fileName = "sevenseg.asm",
            sourceCode = """
; Count 0-9 on 7-segment display connected to P2
; Default config: P2 7-Segment

        ORG 0000h
        LJMP MAIN

        ORG 000Bh
        LJMP TIMER0_ISR

MAIN:   MOV SP, #5Fh
        MOV P2, #0FFh

        MOV TMOD, #01h
        MOV TH0, #0FCh
        MOV TL0, #018h
        SETB ET0
        SETB EA
        SETB TR0

        MOV DPTR, #SEG_TABLE
        MOV R0, #00h
        SJMP $

TIMER0_ISR:
        MOV TH0, #0FCh
        MOV TL0, #018h

        INC R1
        MOV A, R1
        CJNE A, #0FAh, DONE
        MOV R1, #00h

        MOV A, R0
        MOVC A, @A+DPTR
        MOV P2, A

        INC R0
        MOV A, R0
        CJNE A, #0Ah, DONE
        MOV R0, #00h
DONE:   RETI

SEG_TABLE:
        DB 3Fh, 06h, 5Bh, 4Fh, 66h, 6Dh, 7Dh, 07h, 7Fh, 6Fh
            """.trimIndent(),
            buildHwConfig = {
                listOf(HwComponentConfig("hw_1", "P2 7-Seg", "SEVEN_SEG", ports = listOf(Port.P2)))
            },
        ),
        BuiltinExample(
            id = "04_toggle",
            title = "04. Toggle Switch",
            subtitle = "Switch on P1.0 toggles LED on P1.7",
            fileName = "toggle.asm",
            sourceCode = """
; Read toggle switch on P1.0, toggle LED on P1.7

        ORG 0000h
        LJMP MAIN

MAIN:   MOV SP, #5Fh
        MOV P1, #00h
        SJMP LOOP

LOOP:   JNB P1.0, LOOP

        MOV R0, #14h
DEBOUNCE_DELAY:
        MOV R1, #0FFh
        DJNZ R1, $
        DJNZ R0, DEBOUNCE_DELAY

        JB P1.0, $
        CPL P1.7
        SJMP LOOP
            """.trimIndent(),
            buildHwConfig = {
                listOf(
                    HwComponentConfig("hw_1", "P1.0 Toggle", "TOGGLE", ports = listOf(Port.P1), pin = 0),
                    HwComponentConfig("hw_2", "P1.7 LED", "LED", ports = listOf(Port.P1), pin = 7),
                )
            },
        ),
        BuiltinExample(
            id = "05_keypad",
            title = "05. Matrix Keypad",
            subtitle = "Scans 4×4 keypad on P1, displays key on P2",
            fileName = "keypad.asm",
            sourceCode = """
; Poll 4x4 matrix keypad on P1, output key number (0-15) to P2 LED Bar

        ORG 0000h
        LJMP MAIN

MAIN:   MOV SP, #5Fh
        MOV P1, #0FFh
        MOV P2, #00h
LOOP:   LCALL SCAN_KEYPAD
        CJNE A, #10h, SHOW
        SJMP LOOP

SHOW:   MOV P2, A
        MOV R2, #40h
WAIT:   MOV R1, #0FFh
        DJNZ R1, $
        DJNZ R2, WAIT
        SJMP LOOP

SCAN_KEYPAD:
        MOV R0, #00h
        MOV DPTR, #ROW_MASKS
ROW_LOOP:
        MOV A, R0
        MOVC A, @A+DPTR
        MOV P1, A
        NOP
        NOP
        MOV A, P1
        ANL A, #0F0h
        CJNE A, #0F0h, KEY_FOUND
        INC R0
        CJNE R0, #04h, ROW_LOOP
        MOV A, #10h
        RET

KEY_FOUND:
        SWAP A
        MOV R3, #00h
COL_LOOP:
        RRC A
        JNC COL_DONE
        INC R3
        CJNE R3, #04h, COL_LOOP
COL_DONE:
        MOV A, R0
        MOV B, #04h
        MUL AB
        ADD A, R3
        RET

ROW_MASKS:
        DB 0FEh, 0FDh, 0FBh, 0F7h
            """.trimIndent(),
            buildHwConfig = {
                listOf(
                    HwComponentConfig("hw_1", "P1 Keypad (4x4)", "MATRIX_KEYPAD", ports = listOf(Port.P1), rows = 4, cols = 4),
                    HwComponentConfig("hw_2", "P2 LED Bar", "LED_BAR", ports = listOf(Port.P2)),
                )
            },
        ),
        BuiltinExample(
            id = "06_lcd",
            title = "06. HD44780 LCD",
            subtitle = "Initializes 16×2 LCD on P2 and prints HELLO",
            fileName = "lcd.asm",
            sourceCode = """
; HD44780 16x2 LCD: print "HELLO"
; wiring: RS=P2.0, RW=P2.1(0), E=P2.2, D4-D7=P2.4-P2.7

        ORG 0000h
        LJMP MAIN

MAIN:   MOV SP, #5Fh
        MOV P2, #00h

        LCALL DELAY_5MS
        LCALL INIT_LCD
        LCALL SET_2LINE
        LCALL DISP_ON
        LCALL CLEAR_DSP
        LCALL HOME

        MOV DPTR, #MSG
PRINT_LOOP:
        CLR A
        MOVC A, @A+DPTR
        JZ DONE
        LCALL WRITE_CHAR
        INC DPTR
        SJMP PRINT_LOOP

DONE:   SJMP $

MSG:    DB "HELLO", 00h

INIT_LCD:
        MOV A, #30h
        LCALL PULSE_NIBBLE
        LCALL DELAY_5MS
        MOV A, #30h
        LCALL PULSE_NIBBLE
        LCALL DELAY_5MS
        MOV A, #30h
        LCALL PULSE_NIBBLE
        LCALL DELAY_5MS
        MOV A, #20h
        LCALL PULSE_NIBBLE
        LCALL DELAY_5MS
        RET

SET_2LINE:
        MOV A, #28h
        LCALL SEND_CMD
        RET

DISP_ON:
        MOV A, #0Ch
        LCALL SEND_CMD
        RET

CLEAR_DSP:
        MOV A, #01h
        LCALL SEND_CMD
        LCALL DELAY_5MS
        RET

HOME:
        MOV A, #02h
        LCALL SEND_CMD
        LCALL DELAY_5MS
        RET

WRITE_CHAR:
        MOV R2, #01h
        SJMP SEND_BYTE

SEND_CMD:
        MOV R2, #00h
SEND_BYTE:
        MOV R3, A
        ANL A, #0F0h
        ORL A, R2
        LCALL PULSE_NIBBLE
        MOV A, R3
        SWAP A
        ANL A, #0F0h
        ORL A, R2
        LCALL PULSE_NIBBLE
        RET

PULSE_NIBBLE:
        MOV P2, A
        SETB P2.2
        NOP
        CLR P2.2
        RET

DELAY_5MS:
        MOV R0, #20h
D1:     MOV R1, #0FFh
        DJNZ R1, $
        DJNZ R0, D1
        RET
            """.trimIndent(),
            buildHwConfig = {
                listOf(HwComponentConfig("hw_1", "P2 LCD (16x2)", "HD44780_LCD", ports = listOf(Port.P2), lcdCols = 16, lcdLines = 2))
            },
        ),
        BuiltinExample(
            id = "07_uart",
            title = "07. UART Echo",
            subtitle = "Sends HELLO and echoes typed RX chars",
            fileName = "uart.asm",
            sourceCode = """
; Send "HELLO\r\n" via UART and echo received characters

        ORG 0000h
        LJMP MAIN

        ORG 0023h
        LJMP UART_ISR

MAIN:   MOV SP, #5Fh
        MOV P3, #0FFh

        MOV TMOD, #20h
        MOV TH1, #0FDh
        MOV TL1, #0FDh
        SETB TR1

        MOV SCON, #50h
        SETB ES
        SETB EA

        MOV DPTR, #MSG
        LCALL SEND_STRING
        SJMP $

UART_ISR:
        JNB TI, CHECK_RX
        CLR TI
        CLR F0
CHECK_RX:
        JNB RI, RETI_UART
        CLR RI
        MOV A, SBUF
        SETB F0
        MOV SBUF, A
RETI_UART:
        RETI

SEND_STRING:
        CLR A
        MOVC A, @A+DPTR
        JZ STRING_DONE
        LCALL SEND_CHAR
        INC DPTR
        SJMP SEND_STRING
STRING_DONE:
        RET

SEND_CHAR:
        SETB F0
        MOV SBUF, A
WAIT_TX:
        JB F0, WAIT_TX
        RET

MSG:    DB "HELLO", 0Dh, 0Ah, 00h
            """.trimIndent(),
            buildHwConfig = {
                listOf(HwComponentConfig("hw_1", "UART Terminal", "UART_TERMINAL", ports = listOf(Port.P3)))
            },
        ),
        BuiltinExample(
            id = "08_stepper",
            title = "08. Stepper Motor",
            subtitle = "4-phase rotation on P1.0–P1.3",
            fileName = "stepper.asm",
            sourceCode = """
; Stepper motor rotation on P1 (lower nibble)

        ORG 0000h
        LJMP MAIN

        ORG 000Bh
        LJMP TIMER0_ISR

MAIN:   MOV SP, #5Fh
        MOV P1, #00h

        MOV TMOD, #01h
        MOV TH0, #0FEh
        MOV TL0, #0C8h
        SETB ET0
        SETB EA
        SETB TR0

        MOV R0, #00h
        MOV DPTR, #PHASES
        SJMP $

TIMER0_ISR:
        MOV TH0, #0FEh
        MOV TL0, #0C8h

        MOV A, R0
        MOVC A, @A+DPTR
        ANL A, #0Fh
        MOV P1, A

        INC R0
        MOV A, R0
        CJNE A, #04h, DONE
        MOV R0, #00h
DONE:   RETI

PHASES:
        DB 01h, 02h, 04h, 08h
            """.trimIndent(),
            buildHwConfig = {
                listOf(HwComponentConfig("hw_1", "P1 Stepper", "STEPPER", ports = listOf(Port.P1)))
            },
        ),
        BuiltinExample(
            id = "09_buzzer",
            title = "09. 1 kHz Buzzer",
            subtitle = "Timer 0 toggles buzzer on P1.0 at 1 kHz",
            fileName = "buzzer.asm",
            sourceCode = """
; Buzzer toggle on P1.0 at ~1kHz

        ORG 0000h
        LJMP MAIN

        ORG 000Bh
        LJMP TIMER0_ISR

MAIN:   MOV SP, #5Fh
        MOV P1, #00h

        MOV TMOD, #01h
        MOV TH0, #0FEh
        MOV TL0, #0C4h
        SETB ET0
        SETB EA
        SETB TR0
        SJMP $

TIMER0_ISR:
        MOV TH0, #0FEh
        MOV TL0, #0C4h
        CPL P1.0
        RETI
            """.trimIndent(),
            buildHwConfig = {
                listOf(HwComponentConfig("hw_1", "P1.0 Buzzer", "BUZZER", ports = listOf(Port.P1), pin = 0))
            },
        ),
        BuiltinExample(
            id = "10_servo",
            title = "10. Servo PWM Sweep",
            subtitle = "Sweeps P1.1 servo across 0°, 90°, and 180°",
            fileName = "servo.asm",
            sourceCode = """
; Servo PWM control on P1.1 — sweeps 0° -> 90° -> 180° -> 90°

        ORG 0000h
        LJMP MAIN

        ORG 000Bh
        LJMP TIMER0_ISR

        ORG 001Bh
        LJMP TIMER1_ISR

MAIN:   MOV SP, #5Fh
        MOV P1, #00h

        MOV 30h, #0FCh
        MOV 31h, #18h
        MOV R2, #00h
        MOV R3, #00h

        MOV TMOD, #11h
        MOV TH0, #0B1h
        MOV TL0, #0E0h
        SETB ET0
        SETB ET1
        SETB EA
        SETB TR0
        SJMP $

TIMER0_ISR:
        MOV TH0, #0B1h
        MOV TL0, #0E0h

        CLR TR1
        MOV TH1, 30h
        MOV TL1, 31h
        SETB P1.1
        SETB TR1

        INC R3
        CJNE R3, #19h, T0_DONE
        MOV R3, #00h
        INC R2
        CJNE R2, #04h, LOAD_POS
        MOV R2, #00h
LOAD_POS:
        MOV A, R2
        RL A
        MOV DPTR, #ANGLES
        PUSH ACC
        MOVC A, @A+DPTR
        MOV 30h, A
        POP ACC
        INC A
        MOVC A, @A+DPTR
        MOV 31h, A
T0_DONE:
        RETI

TIMER1_ISR:
        CLR TR1
        CLR P1.1
        RETI

ANGLES:
        DB 0FCh, 18h
        DB 0FAh, 24h
        DB 0F8h, 30h
        DB 0FAh, 24h
            """.trimIndent(),
            buildHwConfig = {
                listOf(HwComponentConfig("hw_1", "P1.1 Servo", "SERVO", ports = listOf(Port.P1), pin = 1))
            },
        ),
        BuiltinExample(
            id = "11_ledmatrix",
            title = "11. 8×8 LED Matrix",
            subtitle = "Row-scanned letter 'A' on P1/P2 LED matrix",
            fileName = "ledmatrix.asm",
            sourceCode = """
; 8x8 LED Matrix: display "A" with row scanning

        ORG 0000h
        LJMP MAIN

        ORG 000Bh
        LJMP TIMER0_ISR

MAIN:   MOV SP, #5Fh
        MOV P1, #00h
        MOV P2, #00h

        MOV TMOD, #01h
        MOV TH0, #0FCh
        MOV TL0, #018h
        SETB ET0
        SETB EA
        SETB TR0

        MOV R0, #00h
        SJMP $

TIMER0_ISR:
        MOV TH0, #0FCh
        MOV TL0, #018h

        MOV P1, #00h
        MOV A, R0
        MOV DPTR, #FONT_A
        MOVC A, @A+DPTR
        MOV P2, A

        MOV A, R0
        MOV DPTR, #ROW_BITS
        MOVC A, @A+DPTR
        MOV P1, A

        INC R0
        CJNE R0, #08h, ISR_DONE
        MOV R0, #00h
ISR_DONE:
        RETI

ROW_BITS:
        DB 01h, 02h, 04h, 08h, 10h, 20h, 40h, 80h

FONT_A:
        DB 18h, 3Ch, 66h, 66h, 7Eh, 66h, 66h, 00h
            """.trimIndent(),
            buildHwConfig = {
                listOf(HwComponentConfig("hw_1", "P1/P2 Matrix", "LED_MATRIX", ports = listOf(Port.P1, Port.P2)))
            },
        ),
        BuiltinExample(
            id = "12_button",
            title = "12. Push Button (INT0)",
            subtitle = "Button on P3.2 triggers INT0 to toggle LED",
            fileName = "button.asm",
            sourceCode = """
; Push Button on P3.2 (INT0) toggles LED on P1.7

        ORG 0000h
        LJMP MAIN

        ORG 0003h
        LJMP INT0_ISR

MAIN:   MOV SP, #5Fh
        MOV P1, #00h
        SETB P3.2
        SETB IT0
        SETB EX0
        SETB EA
        SJMP $

INT0_ISR:
        CPL P1.7
        RETI
            """.trimIndent(),
            buildHwConfig = {
                listOf(
                    HwComponentConfig("hw_1", "P3.2 Button", "PUSH_BUTTON", ports = listOf(Port.P3), pin = 2, activeLow = true),
                    HwComponentConfig("hw_2", "P1.7 LED", "LED", ports = listOf(Port.P1), pin = 7),
                )
            },
        ),
        BuiltinExample(
            id = "13_dipswitch",
            title = "13. 8-bit DIP Switch",
            subtitle = "Mirrors P0 DIP switches onto P1 LED bar",
            fileName = "dipswitch.asm",
            sourceCode = """
; 8-bit DIP Switch on P0 mirrored to LED Bar on P1

        ORG 0000h
        LJMP MAIN

MAIN:   MOV SP, #5Fh
        MOV P0, #0FFh
        MOV P1, #00h
LOOP:   MOV A, P0
        MOV P1, A
        SJMP LOOP
            """.trimIndent(),
            buildHwConfig = {
                listOf(
                    HwComponentConfig("hw_1", "P0 DIP Switch", "DIP_SWITCH", ports = listOf(Port.P0)),
                    HwComponentConfig("hw_2", "P1 LED Bar", "LED_BAR", ports = listOf(Port.P1)),
                )
            },
        ),
        BuiltinExample(
            id = "14_multisevenseg",
            title = "14. 4-Digit 7-Seg Counter",
            subtitle = "Multiplexed 0000–9999 counter on P2/P0",
            fileName = "multisevenseg.asm",
            sourceCode = """
; 4-digit multiplexed 7-segment counter (0000-9999)

        ORG 0000h
        LJMP MAIN

        ORG 000Bh
        LJMP TIMER0_ISR

MAIN:   MOV SP, #5Fh
        MOV P2, #00h
        MOV P0, #00h

        MOV 30h, #00h
        MOV 31h, #00h
        MOV 32h, #00h
        MOV 33h, #00h
        MOV R0, #00h
        MOV R2, #00h

        MOV TMOD, #01h
        MOV TH0, #0FCh
        MOV TL0, #18h
        SETB ET0
        SETB EA
        SETB TR0
        SJMP $

TIMER0_ISR:
        MOV TH0, #0FCh
        MOV TL0, #18h

        MOV P0, #00h

        MOV A, #30h
        ADD A, R0
        MOV R1, A
        MOV A, @R1
        MOV DPTR, #SEG_TABLE
        MOVC A, @A+DPTR
        MOV P2, A

        MOV A, R0
        MOV DPTR, #DIGIT_MASK
        MOVC A, @A+DPTR
        MOV P0, A

        INC R0
        CJNE R0, #04h, COUNT_TICK
        MOV R0, #00h

COUNT_TICK:
        INC R2
        CJNE R2, #32h, ISR_DONE
        MOV R2, #00h
        LCALL INC_BCD
ISR_DONE:
        RETI

INC_BCD:
        INC 30h
        MOV A, 30h
        CJNE A, #0Ah, BCD_DONE
        MOV 30h, #00h
        INC 31h
        MOV A, 31h
        CJNE A, #0Ah, BCD_DONE
        MOV 31h, #00h
        INC 32h
        MOV A, 32h
        CJNE A, #0Ah, BCD_DONE
        MOV 32h, #00h
        INC 33h
        MOV A, 33h
        CJNE A, #0Ah, BCD_DONE
        MOV 33h, #00h
BCD_DONE:
        RET

DIGIT_MASK:
        DB 01h, 02h, 04h, 08h

SEG_TABLE:
        DB 3Fh, 06h, 5Bh, 4Fh, 66h, 6Dh, 7Dh, 07h, 7Fh, 6Fh
            """.trimIndent(),
            buildHwConfig = {
                listOf(HwComponentConfig("hw_1", "P2/P0 4×7-Seg", "MULTI_SEVEN_SEG", ports = listOf(Port.P2, Port.P0), cols = 4))
            },
        ),
    )
}
