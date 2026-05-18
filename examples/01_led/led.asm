; Blink LED on P1.7
; Default config: P1.7 LED
; wiring: anode to VCC, cathode to P1.7 via resistor

        ORG 0000h
        LJMP MAIN

        ORG 000Bh
        LJMP TIMER0_ISR

MAIN:   MOV SP, #2Fh
        MOV P1, #0FFh          ; all inputs (LEDs off, pull-ups)

        MOV TMOD, #01h         ; Timer 0 mode 1 (16-bit)
        MOV TH0, #0FCh        ; ~1ms @ 12MHz
        MOV TL0, #018h
        SETB ET0              ; enable timer 0 interrupt
        SETB EA               ; enable all interrupts
        SETB TR0              ; start timer

        SETB P1.7             ; LED off (pulled high)
        SJMP $

TIMER0_ISR:
        MOV TH0, #0FCh
        MOV TL0, #018h
        CPL P1.7              ; toggle LED
        RETI