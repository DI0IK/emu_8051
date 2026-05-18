; Poll 4x4 matrix keypad, output key number (0-15) to P2
; Default config: P1 Keypad (4x4)
; wiring: rows (R0-R3) on P1.0-P1.3, cols (C0-C3) on P1.4-P1.7
; internal pull-ups on columns, rows driven low when scanning

        ORG 0000h
        LJMP MAIN

MAIN:   MOV SP, #2Fh
        MOV P1, #0FFh         ; enable pull-ups
        MOV P2, #0FFh
        SJMP LOOP

LOOP:   LCALL SCAN_KEYPAD
        CJNE A, #10h, SHOW   ; 10h = no key pressed
        SJMP LOOP

SHOW:   MOV P2, A             ; output key number to P2
        ; display on LEDs briefly
        MOV R0, #0FFh
WAIT:   DJNZ R0, WAIT
        SJMP LOOP

SCAN_KEYPAD:
        MOV R0, #00h         ; R0 = row index
ROW_LOOP:
        MOV A, #0FFh
        MOV R1, A
        ANL A, #0F0h         ; mask upper nibble (cols)
        ORL A, R0            ; set one row low
        MOV P1, A             ; drive the row
        NOP
        NOP
        MOV A, P1             ; read back port
        ANL A, #0F0h         ; mask upper nibble (cols)
        CJNE A, #0F0h, KEY_FOUND
        INC R0
        MOV A, R0
        CJNE A, #04h, ROW_LOOP
        MOV A, #10h          ; no key pressed
        RET

KEY_FOUND:
        MOV R2, A             ; save column bits
        MOV R3, #04h         ; R3 = col index
COL_LOOP:
        MOV A, R2
        RRC A
        JC COL_DONE
        DEC R3
        SJMP COL_LOOP
COL_DONE:
        MOV A, R0
        MOV B, #04h
        MUL AB              ; A = row * 4
        ADD A, R3           ; A = row * 4 + col = key number
        RET