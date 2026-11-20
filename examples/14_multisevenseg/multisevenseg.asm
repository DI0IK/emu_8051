; 4-digit multiplexed 7-segment counter (0000-9999)
; Default config: P2/P0 Multi 7-Segment (Seg=P2, Digit=P0.0..P0.3)

        ORG 0000h
        LJMP MAIN

        ORG 000Bh
        LJMP TIMER0_ISR

MAIN:   MOV SP, #5Fh
        MOV P2, #00h
        MOV P0, #00h

        ; Digits stored in RAM: 30h=ones, 31h=tens, 32h=hundreds, 33h=thousands
        MOV 30h, #00h
        MOV 31h, #00h
        MOV 32h, #00h
        MOV 33h, #00h
        MOV R0, #00h        ; active multiplexed digit (0..3)
        MOV R2, #00h        ; refresh tick counter

        MOV TMOD, #01h      ; Timer 0 Mode 1
        MOV TH0, #0FCh      ; ~1ms scan tick @ 12MHz
        MOV TL0, #18h
        SETB ET0
        SETB EA
        SETB TR0
        SJMP $

TIMER0_ISR:
        MOV TH0, #0FCh
        MOV TL0, #18h

        ; Blank digits before changing segments
        MOV P0, #00h

        ; Read digit value (30h + R0)
        MOV A, #30h
        ADD A, R0
        MOV R1, A
        MOV A, @R1
        MOV DPTR, #SEG_TABLE
        MOVC A, @A+DPTR
        MOV P2, A

        ; Enable digit bit (1 << R0)
        MOV A, R0
        MOV DPTR, #DIGIT_MASK
        MOVC A, @A+DPTR
        MOV P0, A

        INC R0
        CJNE R0, #04h, COUNT_TICK
        MOV R0, #00h

COUNT_TICK:
        INC R2
        CJNE R2, #32h, ISR_DONE ; increment counter every ~50ms
        MOV R2, #00h
        LCALL INC_BCD
ISR_DONE:
        RETI

INC_BCD:
        INC 30h
        MOV A, 30h
        CJNE A, #0Ah, BCD_DONE
        MOV 30h, #00h
        INC 31h
        MOV A, 31h
        CJNE A, #0Ah, BCD_DONE
        MOV 31h, #00h
        INC 32h
        MOV A, 32h
        CJNE A, #0Ah, BCD_DONE
        MOV 32h, #00h
        INC 33h
        MOV A, 33h
        CJNE A, #0Ah, BCD_DONE
        MOV 33h, #00h
BCD_DONE:
        RET

DIGIT_MASK:
        DB 01h, 02h, 04h, 08h

SEG_TABLE:
        DB 3Fh, 06h, 5Bh, 4Fh, 66h, 6Dh, 7Dh, 07h, 7Fh, 6Fh
