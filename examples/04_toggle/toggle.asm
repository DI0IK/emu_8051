; Read toggle switch on P1.0, toggle LED on P1.7
; Default config: P1.7 LED, P1.0 Toggle Switch
; wiring: toggle between P1.0 and GND (internal pull-up)

        ORG 0000h
        LJMP MAIN

MAIN:   MOV SP, #2Fh
        MOV P1, #0FFh         ; enable pull-ups on all P1 pins
        SETB P1.7            ; LED off initially
        SJMP LOOP

LOOP:   JB P1.0, LOOP       ; wait for button press (active low)

        ; debounce: wait ~20ms
        MOV R0, #14h
DEBOUNCE_DELAY:
        MOV R1, #0FFh
        DJNZ R1, $
        DJNZ R0, DEBOUNCE_DELAY

        ; wait for release
        JNB P1.0, $
        CPL P1.7             ; toggle LED
        SJMP LOOP