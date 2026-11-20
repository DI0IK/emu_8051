; Push Button on P3.2 (INT0) toggles LED on P1.7 using external interrupt
; Default config: P3.2 Push Button (Active LOW), P1.7 LED

        ORG 0000h
        LJMP MAIN

        ORG 0003h           ; External Interrupt 0 vector
        LJMP INT0_ISR

MAIN:   MOV SP, #5Fh
        MOV P1, #00h        ; LED off initially
        SETB P3.2           ; INT0 input pull-up
        SETB IT0            ; falling-edge triggered INT0
        SETB EX0            ; enable INT0
        SETB EA             ; global interrupt enable
        SJMP $

INT0_ISR:
        CPL P1.7            ; toggle LED on each button press
        RETI
