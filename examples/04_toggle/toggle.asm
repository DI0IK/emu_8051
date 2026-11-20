; Read toggle switch on P1.0, toggle LED on P1.7
; Default config: P1.7 LED, P1.0 Toggle Switch (active HIGH)
; wiring: Toggle Switch drives P1.0 (0=OFF, 1=ON), LED on P1.7

        ORG 0000h
        LJMP MAIN

MAIN:   MOV SP, #5Fh
        MOV P1, #00h          ; LED off initially
        SJMP LOOP

LOOP:   JNB P1.0, LOOP        ; wait for switch ON (active HIGH)

        ; debounce: wait ~20ms
        MOV R0, #14h
DEBOUNCE_DELAY:
        MOV R1, #0FFh
        DJNZ R1, $
        DJNZ R0, DEBOUNCE_DELAY

        ; wait for switch OFF
        JB P1.0, $
        CPL P1.7              ; toggle LED
        SJMP LOOP
