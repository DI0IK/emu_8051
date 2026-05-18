; HD44780 16x2 LCD: print "HELLO"
; Default config: P2 LCD (16x2)
; wiring: D4-D7 = P2.4-P2.7, RS = P2.0, RW = GND, E = P2.1
; 4-bit mode

        ORG 0000h
        LJMP MAIN

MAIN:   MOV SP, #2Fh
        MOV P2, #0FFh

        ; 15ms power-up delay
        MOV R0, #0FFh
POWER_UP:
        MOV R1, #0FFh
        DJNZ R1, $
        DJNZ R0, POWER_UP

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
        LCALL SEND_NIBBLE
        LCALL DELAY_5MS
        LCALL SEND_NIBBLE
        LCALL DELAY_5MS
        LCALL SEND_NIBBLE
        LCALL DELAY_5MS
        MOV A, #20h         ; 4-bit mode
        LCALL SEND_NIBBLE
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
        LCALL SEND_CHAR
        RET

WRITE_CHAR:
        ORL A, #01h        ; RS = 1 for data
        SJMP SEND_BYTE

SEND_CMD:
        CLR A               ; RS = 0 for command
SEND_BYTE:
        MOV R0, A
        LCALL SEND_NIBBLE
        MOV A, R0
        SWAP A             ; send upper nibble
SEND_NIBBLE:
        ANL A, #0Fh
        ANL P2, #0F0h      ; clear lower nibble
        ORL P2, A          ; set data bits
        SETB P2.1          ; E = 1
        CLR P2.1           ; E = 0 (pulse)
        RET

DELAY_5MS:
        MOV R0, #0FFh
D1:     MOV R1, #0FFh
        DJNZ R1, $
        DJNZ R0, D1
        RET