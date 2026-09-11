; Timer 2 periodic interrupt example
; AT89S52-style Timer 2 overflow interrupt toggles an LED on P1.7.
; Default config: P1.7 LED (active-low)
; Timer 2 is configured as a timer with automatic reload from RCAP2H:RCAP2L.

        ORG 0000h
        LJMP MAIN

        ORG 002Bh
        LJMP TIMER2_ISR

MAIN:   MOV SP, #2Fh
        MOV P1, #0FFh

        ; Reload value for a short demonstration interval.
        MOV RCAP2H, #0FCh
        MOV RCAP2L, #018h
        MOV TH2, #0FCh
        MOV TL2, #018h

        ; Timer mode (CT2=0), auto-reload (CPRL2=0), run (TR2=1).
        MOV T2CON, #04h
        SETB ET2
        SETB EA
        SJMP $

TIMER2_ISR:
        CPL P1.7
        RETI
