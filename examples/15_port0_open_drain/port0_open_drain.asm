; Port 0 open-drain example
; P0.0 drives low for a logic zero and releases the line for a logic one.
; Real hardware requires an external pull-up to observe a released high.
; The simulator's digital pin API uses a deterministic low fallback for a
; released P0 pin when no external pull-up component is attached.

        ORG 0000h
        LJMP MAIN

MAIN:   MOV SP, #2Fh
        MOV P0, #0FEh        ; P0.0 actively drives low
        MOV A, P0
        MOV P1, A

        MOV P0, #0FFh        ; release all P0 pins (open-drain)
        MOV A, P0
        MOV P1, A
        SJMP $
