; External RAM (MOVX) example
; Stores a byte in XRAM at 1234h, reads it back, and displays it on P1.
; P0/P2 are the classic multiplexed external address/data bus; the simulator
; models the logical 64 KiB XRAM accessed by MOVX.
; Default config: P1 LED Bar (active-low)

        ORG 0000h
        LJMP MAIN

MAIN:   MOV SP, #2Fh
        MOV DPTR, #1234h
        MOV A, #05Ah
        MOVX @DPTR, A

        CLR A
        MOVX A, @DPTR
        MOV P1, A
        SJMP $
