; HD44780 16x2 LCD: print "HELLO"
; Default config: P2 LCD (16x2)
; wiring: RS = P2.0, RW = P2.1 (GND/0), E = P2.2, D4-D7 = P2.4-P2.7
; 4-bit mode

        ORG 0000h
        LJMP MAIN

MAIN:   MOV SP, #5Fh
        MOV P2, #00h

        LCALL DELAY_5MS
        LCALL INIT_LCD
        LCALL SET_2LINE
        LCALL DISP_ON
        LCALL CLEAR_DSP
        LCALL HOME

        MOV DPTR, #MSG
PRINT_LOOP:
        CLR A
        MOVC A, @A+DPTR
        JZ DONE
        LCALL WRITE_CHAR
        INC DPTR
        SJMP PRINT_LOOP

DONE:   SJMP $

MSG:    DB "HELLO", 00h

; 4-bit LCD driver
INIT_LCD:
        MOV A, #30h
        LCALL PULSE_NIBBLE
        LCALL DELAY_5MS
        MOV A, #30h
        LCALL PULSE_NIBBLE
        LCALL DELAY_5MS
        MOV A, #30h
        LCALL PULSE_NIBBLE
        LCALL DELAY_5MS
        MOV A, #20h         ; switch to 4-bit mode
        LCALL PULSE_NIBBLE
        LCALL DELAY_5MS
        RET

SET_2LINE:
        MOV A, #28h         ; 4-bit, 2 lines, 5x8 font
        LCALL SEND_CMD
        RET

DISP_ON:
        MOV A, #0Ch         ; display on, cursor off
        LCALL SEND_CMD
        RET

CLEAR_DSP:
        MOV A, #01h
        LCALL SEND_CMD
        LCALL DELAY_5MS
        RET

HOME:
        MOV A, #02h
        LCALL SEND_CMD
        LCALL DELAY_5MS
        RET

WRITE_CHAR:
        MOV R2, #01h        ; RS = 1 (data), RW = 0
        SJMP SEND_BYTE

SEND_CMD:
        MOV R2, #00h        ; RS = 0 (command), RW = 0
SEND_BYTE:
        MOV R3, A           ; save byte
        ANL A, #0F0h        ; upper nibble -> P2.4..P2.7
        ORL A, R2
        LCALL PULSE_NIBBLE
        MOV A, R3
        SWAP A              ; lower nibble -> P2.4..P2.7
        ANL A, #0F0h
        ORL A, R2
        LCALL PULSE_NIBBLE
        RET

PULSE_NIBBLE:
        MOV P2, A
        SETB P2.2           ; E = 1
        NOP
        CLR P2.2            ; E = 0 (falling edge latches nibble)
        RET

DELAY_5MS:
        MOV R0, #20h
D1:     MOV R1, #0FFh
        DJNZ R1, $
        DJNZ R0, D1
        RET
