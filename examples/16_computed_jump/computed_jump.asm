; Computed jump example
; JMP @A+DPTR dispatches through a table of relative SJMP entries.
; R0 selects one of three cases; P1 shows the selected output value.
; Default config: P1 LED Bar (active-low)

        ORG 0000h
        LJMP MAIN

MAIN:   MOV SP, #2Fh
        MOV P1, #0FFh
        MOV R0, #01h
        MOV DPTR, #DISPATCH
        MOV A, R0
        MOV B, #02h
        MUL AB                 ; each SJMP table entry is two bytes
        JMP @A+DPTR

CASE0:  MOV P1, #0FEh
        SJMP $
CASE1:  MOV P1, #0FDh
        SJMP $
CASE2:  MOV P1, #0FBh
        SJMP $

DISPATCH:
        SJMP CASE0
        SJMP CASE1
        SJMP CASE2
