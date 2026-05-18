; 8x8 LED Matrix: display "A" with row scanning
; Default config: P1/P2 LED Matrix (rows=P1, cols=P2)
; wiring: rows (R0-R7) to P1.0-P1.7 via transistor drivers
;          cols (C0-C7) to P2.0-P2.7 with pull-down resistors (active high)
; Software scans rows one at a time; columns show which LEDs are on.
; Active HIGH: row bit = 1 selects row, column bit = 1 turns LED on.

        ORG 0000h
        LJMP MAIN

        ORG 000Bh
        LJMP TIMER0_ISR

MAIN:   MOV SP, #2Fh
        MOV P1, #00h          ; all rows low (off)
        MOV P2, #00h          ; all columns low (all LEDs off)

        MOV TMOD, #01h        ; Timer 0 mode 1 (16-bit)
        MOV TH0, #0FCh        ; ~1ms @ 12MHz (fast scan)
        MOV TL0, #018h
        SETB ET0
        SETB EA
        SETB TR0

        MOV R0, #00h          ; current row (0-7)
        SJMP $

TIMER0_ISR:
        MOV TH0, #0FCh
        MOV TL0, #018h

        ; turn off all rows first (all low)
        CLR P1.0
        CLR P1.1
        CLR P1.2
        CLR P1.3
        CLR P1.4
        CLR P1.5
        CLR P1.6
        CLR P1.7

        ; look up column data for current row
        MOV A, R0
        MOV DPTR, #FONT_A
        MOVC A, @A+DPTR
        MOV P2, A             ; drive columns (1 = LED on)

        ; drive current row high
        CJNE R0, #00h, ROW1
        SETB P1.0
        SJMP ROW_DONE
ROW1:   CJNE R0, #01h, ROW2
        SETB P1.1
        SJMP ROW_DONE
ROW2:   CJNE R0, #02h, ROW3
        SETB P1.2
        SJMP ROW_DONE
ROW3:   CJNE R0, #03h, ROW4
        SETB P1.3
        SJMP ROW_DONE
ROW4:   CJNE R0, #04h, ROW5
        SETB P1.4
        SJMP ROW_DONE
ROW5:   CJNE R0, #05h, ROW6
        SETB P1.5
        SJMP ROW_DONE
ROW6:   CJNE R0, #06h, ROW7
        SETB P1.6
        SJMP ROW_DONE
ROW7:   SETB P1.7

ROW_DONE:
        INC R0
        MOV A, R0
        CJNE A, #08h, ISR_DONE
        MOV R0, #00h
ISR_DONE:
        RETI

; 5x7 font for letter "A" (bitmaps for rows 0-7)
; Each byte: bits 0-7 = columns 0-7 (1 = LED on)
FONT_A:
        DB 18h              ; 00011000  row 0 (top)
        DB 3Ch              ; 00111100  row 1
        DB 66h              ; 01100110  row 2
        DB 66h              ; 01100110  row 3
        DB 7Eh              ; 01111110  row 4
        DB 66h              ; 01100110  row 5
        DB 66h              ; 01100110  row 6
        DB 00h              ; 00000000  row 7 (bottom gap)