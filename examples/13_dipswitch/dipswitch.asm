; 8-bit DIP Switch on P0 mirrored to LED Bar on P1
; Default config: P0 DIP Switch, P1 LED Bar

        ORG 0000h
        LJMP MAIN

MAIN:   MOV SP, #5Fh
        MOV P0, #0FFh       ; P0 as input
        MOV P1, #00h
LOOP:   MOV A, P0           ; read 8-bit DIP switch
        MOV P1, A           ; display on LED bar
        SJMP LOOP
