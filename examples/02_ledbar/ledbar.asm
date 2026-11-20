; Knight rider wave on P1 (8 LEDs)
; Default config: P1 LED Bar (active HIGH)
; wiring: P1.0-P1.7 -> resistors -> LEDs -> GND

        ORG 0000h
        LJMP MAIN

        ORG 000Bh
        LJMP TIMER0_ISR

MAIN:   MOV SP, #5Fh
        MOV P1, #00h

        MOV TMOD, #01h        ; Timer 0 mode 1 (16-bit)
        MOV TH0, #0F8h        ; ~2ms @ 12MHz
        MOV TL0, #030h
        SETB ET0
        SETB EA
        SETB TR0

        MOV A, #01h           ; start pattern: bit 0 set
        MOV R0, #00h          ; direction flag: 0=left, 1=right
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
        MOV R0, #01h          ; switch to shifting right
        POP ACC
        SJMP DONE
SET_LEFT:
        MOV R0, #00h          ; switch to shifting left
        POP ACC

DONE:   RETI
