; Servo control with PWM on P1.0
; Default config: P1.0 Servo
; wiring: servo signal wire to P1.0, VCC and GND to servo
; Pulse: 1ms = 0deg, 1.5ms = 90deg, 2ms = 180deg
; Uses Timer 0 for the pulse width, Timer 1 for the cycle period

        ORG 0000h
        LJMP MAIN

        ORG 000Bh
        LJMP TIMER0_ISR

        ORG 001Bh
        LJMP TIMER1_ISR

MAIN:   MOV SP, #2Fh
        MOV P1, #0FFh

        ; Timer 1: 20ms period for servo cycle
        MOV TMOD, #20h      ; Timer 1 mode 2 (auto-reload)
        MOV TH1, #0E8h      ; 20ms * 12MHz / 12 / 256 ≈ 0E8h
        MOV TL1, #0E8h
        SETB ET1
        SETB EA
        SETB TR1

        ; start at 90 degrees (1.5ms)
        MOV 30h, #0F4h      ; store target pulse (1.5ms)
        SETB P1.0
        SJMP $

TIMER0_ISR:
        CLR TR0
        CLR P1.0            ; end pulse
        RETI

TIMER1_ISR:
        CLR TF1
        SETB P1.0           ; start servo pulse

        ; Timer 0: pulse width
        MOV TMOD, #01h      ; Timer 0 mode 1 (16-bit)
        ; pulse width loaded from RAM
        MOV A, 30h
        MOV R0, A
        MOV TH0, #0FFh      ; start near top
        MOV TL0, R0
        SETB TR0
        RETI