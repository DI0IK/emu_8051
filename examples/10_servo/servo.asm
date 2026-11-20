; Servo PWM control on P1.1 — sweeps 0° -> 90° -> 180° -> 90°
; Default config: P1.1 Servo Motor
; Pulse: 1000us (0deg), 1500us (90deg), 2000us (180deg) every 20ms
; Timer 0: 20ms frame period; Timer 1: pulse width

        ORG 0000h
        LJMP MAIN

        ORG 000Bh
        LJMP TIMER0_ISR

        ORG 001Bh
        LJMP TIMER1_ISR

MAIN:   MOV SP, #5Fh
        MOV P1, #00h

        ; Store current pulse timer reload (TH in 30h, TL in 31h)
        ; Start at 0 deg = 65536 - 1000 = FC18h
        MOV 30h, #0FCh
        MOV 31h, #18h
        MOV R2, #00h        ; position index (0..3)
        MOV R3, #00h        ; frame counter

        MOV TMOD, #11h      ; Timer 0 & Timer 1 in Mode 1 (16-bit)
        MOV TH0, #0B1h      ; 65536 - 20000 = B1E0h (20ms)
        MOV TL0, #0E0h
        SETB ET0
        SETB ET1
        SETB EA
        SETB TR0
        SJMP $

TIMER0_ISR:
        MOV TH0, #0B1h      ; reload 20ms frame
        MOV TL0, #0E0h

        CLR TR1
        MOV TH1, 30h        ; load pulse duration
        MOV TL1, 31h
        SETB P1.1           ; start pulse HIGH
        SETB TR1

        ; Advance target angle every 25 frames (~0.5s)
        INC R3
        CJNE R3, #19h, T0_DONE
        MOV R3, #00h
        INC R2
        CJNE R2, #04h, LOAD_POS
        MOV R2, #00h
LOAD_POS:
        MOV A, R2
        RL A                ; index * 2
        MOV DPTR, #ANGLES
        PUSH ACC
        MOVC A, @A+DPTR
        MOV 30h, A          ; TH1 reload
        POP ACC
        INC A
        MOVC A, @A+DPTR
        MOV 31h, A          ; TL1 reload
T0_DONE:
        RETI

TIMER1_ISR:
        CLR TR1
        CLR P1.1            ; end pulse LOW
        RETI

ANGLES:
        DB 0FCh, 18h        ; 0 deg   (1000us -> 65536 - 1000)
        DB 0FAh, 24h        ; 90 deg  (1500us -> 65536 - 1500)
        DB 0F8h, 30h        ; 180 deg (2000us -> 65536 - 2000)
        DB 0FAh, 24h        ; 90 deg  (1500us)
