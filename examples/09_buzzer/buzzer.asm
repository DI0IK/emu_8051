; Buzzer toggle on P1.0 at ~1kHz
; Default config: P1.0 Buzzer
; wiring: buzzer transistor base to P1.0, buzzer between VCC and collector

        ORG 0000h
        LJMP MAIN

        ORG 000Bh
        LJMP TIMER0_ISR

MAIN:   MOV SP, #2Fh
        MOV P1, #0FFh

        MOV TMOD, #01h      ; Timer 0 mode 1 (16-bit)
        ; ~500us half-period = 1kHz
        ; 65536 - 500us*12MHz/12 = 65536 - 500 = 65036 = FEC4h
        MOV TH0, #0FEh
        MOV TL0, #0C4h
        SETB ET0
        SETB EA
        SETB TR0

        SETB P1.0           ; buzzer off (active low)
        SJMP $

TIMER0_ISR:
        MOV TH0, #0FEh
        MOV TL0, #0C4h
        CPL P1.0            ; toggle buzzer pin
        RETI