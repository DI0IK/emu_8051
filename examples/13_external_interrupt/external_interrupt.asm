; External interrupt 0 example
; A switch connected between P3.2 / INT0 and GND toggles the LED on each
; falling edge. Configure a Toggle component for P3.2 to try this example.
; Default config: P1.7 LED (active-low), active-low toggle on P3.2.

        ORG 0000h
        LJMP MAIN

        ORG 0003h
        LJMP INT0_ISR

MAIN:   MOV SP, #2Fh
        MOV P1, #0FFh
        SETB IT0            ; INT0 edge-triggered
        SETB EX0            ; enable external interrupt 0
        SETB EA
        SJMP $

INT0_ISR:
        CPL P1.7
        RETI
