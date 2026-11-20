; Poll 4x4 matrix keypad on P1, output key number (0-15) to P2 LED Bar
; Default config: P1 Keypad (4x4), P2 LED Bar
; wiring: rows (R0-R3) on P1.0-P1.3, cols (C0-C3) on P1.4-P1.7
; internal pull-ups on columns, rows driven low one at a time when scanning

        ORG 0000h
        LJMP MAIN

MAIN:   MOV SP, #5Fh
        MOV P1, #0FFh         ; enable pull-ups
        MOV P2, #00h
LOOP:   LCALL SCAN_KEYPAD
        CJNE A, #10h, SHOW    ; 10h = no key pressed
        SJMP LOOP

SHOW:   MOV P2, A             ; output key number (0..15) to P2
        MOV R2, #40h
WAIT:   MOV R1, #0FFh
        DJNZ R1, $
        DJNZ R2, WAIT
        SJMP LOOP

SCAN_KEYPAD:
        MOV R0, #00h          ; R0 = row index (0..3)
        MOV DPTR, #ROW_MASKS
ROW_LOOP:
        MOV A, R0
        MOVC A, @A+DPTR       ; pull one row low, keep cols high
        MOV P1, A
        NOP
        NOP
        MOV A, P1             ; read back port
        ANL A, #0F0h          ; mask upper nibble (cols)
        CJNE A, #0F0h, KEY_FOUND
        INC R0
        CJNE R0, #04h, ROW_LOOP
        MOV A, #10h           ; no key pressed
        RET

KEY_FOUND:
        SWAP A                ; move col bits to lower nibble (0..3)
        MOV R3, #00h          ; R3 = col index (0..3)
COL_LOOP:
        RRC A
        JNC COL_DONE          ; found pulled-low column bit
        INC R3
        CJNE R3, #04h, COL_LOOP
COL_DONE:
        MOV A, R0
        MOV B, #04h
        MUL AB                ; A = row * 4
        ADD A, R3             ; A = row * 4 + col = key number
        RET

ROW_MASKS:
        DB 0FEh, 0FDh, 0FBh, 0F7h
