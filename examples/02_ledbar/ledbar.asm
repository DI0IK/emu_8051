; Knight rider wave on P1 (8 LEDs)
; Default config: P1 LED Bar
; wiring: all 8 LEDs connected to P1 (anodes to VCC, cathodes to P1.0-P1.7 via resistors)

        ORG 0000h
        LJMP MAIN

        ORG 000Bh
        LJMP TIMER0_ISR

MAIN:   MOV SP, #2Fh
        MOV P1, #0FFh

        MOV TMOD, #01h        ; Timer 0 mode 1 (16-bit)
        MOV TH0, #0F8h        ; ~2ms @ 12MHz
        MOV TL0, #030h
        SETB ET0
        SETB EA
        SETB TR0

        MOV A, #01h           ; start pattern: only bit 0 set
        MOV R0, #00h         ; direction flag: 0=左, 1=
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
        MOV A, R0
        CJNE A, #00h, SET_LEFT
        MOV R0, #01h
        SJMP DONE
SET_LEFT:
        MOV R0, #00h
        MOV A, #80h          ; restart from right side

DONE:   RETI