; Send "HELLO\r\n" via UART
; Default config: UART Terminal
; wiring: TX (P3.1) connected to terminal
; Uses Timer 1 as baud rate generator @ 9600 baud (12MHz, SMOD=0)

        ORG 0000h
        LJMP MAIN

        ORG 0023h
        LJMP UART_ISR

MAIN:   MOV SP, #2Fh
        MOV P3, #0FFh

        ; Configure Timer 1 for 9600 baud
        MOV TMOD, #20h      ; Timer 1 mode 2 (auto-reload)
        MOV TH1, #0FDh      ; 9600 baud (12MHz crystal)
        MOV TL1, #0FDh
        SETB TR1

        ; Configure UART mode 1
        MOV SCON, #50h      ; Mode 1, REN enabled
        SETB ES             ; enable UART interrupt

        SETB EA
        MOV DPTR, #MSG
        LCALL SEND_STRING
        SJMP $

UART_ISR:
        JNB TI, CHECK_Rx
        CLR TI
        RETI

CHECK_Rx:
        JNB RI, RETI_UART
        CLR RI
        MOV A, SBUF         ; read received char (echo it back)
        MOV SBUF, A
        RETI

RETI_UART:
        RETI

SEND_STRING:
        CLR A
        MOVC A, @A+DPTR
        JZ STRING_DONE
        LCALL SEND_CHAR
        INC DPTR
        SJMP SEND_STRING
STRING_DONE:
        RET

SEND_CHAR:
        JNB TI, $           ; wait for previous transmit to finish
        CLR TI
        MOV SBUF, A
        RET

MSG:    DB "HELLO", 0Dh, 0Ah, 00h