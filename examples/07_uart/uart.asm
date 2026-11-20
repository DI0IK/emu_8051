; Send "HELLO\r\n" via UART and echo received characters
; Default config: UART Terminal (P3.0 RXD, P3.1 TXD)
; Uses Timer 1 as baud rate generator @ 9600 baud (12MHz, SMOD=0)

        ORG 0000h
        LJMP MAIN

        ORG 0023h
        LJMP UART_ISR

MAIN:   MOV SP, #5Fh
        MOV P3, #0FFh

        ; Configure Timer 1 for 9600 baud
        MOV TMOD, #20h      ; Timer 1 mode 2 (auto-reload)
        MOV TH1, #0FDh
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
        JNB TI, CHECK_RX
        CLR TI
        CLR F0              ; clear TX-busy flag
CHECK_RX:
        JNB RI, RETI_UART
        CLR RI
        MOV A, SBUF         ; read received char
        SETB F0
        MOV SBUF, A         ; echo back to terminal
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
        SETB F0             ; mark TX busy
        MOV SBUF, A         ; start transmit (ISR clears F0 and TI)
WAIT_TX:
        JB F0, WAIT_TX
        RET

MSG:    DB "HELLO", 0Dh, 0Ah, 00h
