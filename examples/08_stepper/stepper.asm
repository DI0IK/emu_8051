; Stepper motor full rotation on P1 (lower nibble)
; Default config: P1 Stepper Motor
; wiring: 4 phases to P1.0-P1.3 via driver transistor array
; 4-step sequence: A-B-C-D (full step)

        ORG 0000h
        LJMP MAIN

        ORG 000Bh
        LJMP TIMER0_ISR

MAIN:   MOV SP, #2Fh
        MOV P1, #0FFh

        MOV TMOD, #01h       ; Timer 0 mode 1 (16-bit)
        MOV TH0, #0FEh       ; ~2ms @ 12MHz
        MOV TL0, #0C8h
        SETB ET0
        SETB EA
        SETB TR0

        MOV R0, #00h         ; sequence index
        MOV DPTR, #PHASES   ; address of phase table
        SJMP $

TIMER0_ISR:
        MOV TH0, #0FEh
        MOV TL0, #0C8h

        MOV A, R0
        MOVC A, @A+DPTR      ; get phase pattern
        ANL A, #0Fh         ; mask lower nibble
        MOV P1, A

        INC R0
        MOV A, R0
        CJNE A, #04h, DONE
        MOV R0, #00h        ; wrap sequence

        ; after 4 steps, step count in R1 increments
        ; full rotation = 4096 steps / 4 = 1024 phase changes
        INC R1
        MOV A, R1
        CJNE A, #04h, DONE  ; 1024/256 = 4 full rotations per cycle
        MOV R1, #00h
DONE:   RETI

PHASES:
        DB 11h              ; 0001  (A)
        DB 22h              ; 0010  (B)
        DB 44h              ; 0100  (C)
        DB 88h              ; 1000  (D)