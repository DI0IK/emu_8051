; Count 0-9 on 7-segment display connected to P2
; Default config: P2 7-Segment
; wiring: P2.0=a, P2.1=b, P2.2=c, P2.3=d, P2.4=e, P2.5=f, P2.6=g (P2.7=DP unused)

        ORG 0000h
        LJMP MAIN

        ORG 000Bh
        LJMP TIMER0_ISR

MAIN:   MOV SP, #2Fh
        MOV P2, #0FFh

        MOV TMOD, #01h        ; Timer 0 mode 1 (16-bit)
        MOV TH0, #0FCh        ; ~1ms @ 12MHz
        MOV TL0, #018h
        SETB ET0
        SETB EA
        SETB TR0

        MOV DPTR, #SEG_TABLE ; point to lookup table
        MOV R0, #00h         ; current digit
        SJMP $

TIMER0_ISR:
        MOV TH0, #0FCh
        MOV TL0, #018h

        INC R1
        MOV A, R1
        CJNE A, #0FAh, DONE  ; count to 1000 (1 sec @ 1ms)
        MOV R1, #00h

        MOV A, R0
        MOVC A, @A+DPTR      ; look up segment pattern
        MOV P2, A

        INC R0
        MOV A, R0
        CJNE A, #0Ah, DONE
        MOV R0, #00h         ; wrap after 9
DONE:   RETI

SEG_TABLE:
        DB 3Fh              ; 0  0111111
        DB 06h              ; 1  0000110
        DB 5Bh              ; 2  1011011
        DB 4Fh              ; 3  1001111
        DB 66h              ; 4  1100110
        DB 6Dh              ; 5  1101101
        DB 7Dh              ; 6  1111101
        DB 07h              ; 7  0000111
        DB 7Fh              ; 8  1111111
        DB 6Fh              ; 9  1110111