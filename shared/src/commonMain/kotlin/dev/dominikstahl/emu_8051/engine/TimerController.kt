package dev.dominikstahl.emu_8051.engine

@OptIn(ExperimentalUnsignedTypes::class)
class TimerController(private val state: CpuState) {

    private var prevT0: Boolean = false
    private var prevT1: Boolean = false
    private var prevT2: Boolean = false
    private var prevT2EX: Boolean = false

    fun tick() {
        val tmod = state.TMOD.toInt()
        val tcon = state.TCON.toInt()
        val p3 = state.getEffectivePort(3)
        val p1 = state.getEffectivePort(1)

        val t0 = (p3 and 0x10) == 0
        val t1 = (p3 and 0x20) == 0
        val t2 = (p1 and 0x01) == 0
        val t2ex = (p1 and 0x02) == 0

        val timer0mode = tmod and 0x03
        val timer1mode = (tmod shr 4) and 0x03

        tickTimer0(timer0mode, tmod, tcon, t0)
        if (timer0mode == 3) tickTimer0_TH0(tcon)
        tickTimer1(timer1mode, timer0mode, tmod, tcon, t1)
        tickTimer2(tcon, p1, t2, prevT2EX && !t2ex)

        prevT0 = t0
        prevT1 = t1
        prevT2 = t2
        prevT2EX = t2ex
    }

    // ========================================================================
    // Timer 0
    // ========================================================================

    private fun tickTimer0(mode: Int, tmod: Int, tcon: Int, t0Pin: Boolean) {
        val gate = (tmod and GATE0_BIT) != 0
        val ct = (tmod and CT0_BIT) != 0
        val tr = (tcon and TR0_BIT) != 0
        val int0 = (state.getEffectivePort(3) and 0x04) == 0

        if (!tr) return
        if (gate && !int0) return

        var tl = state.TL0.toInt()
        var th = state.TH0.toInt()

        if (!ct) {
            tl = (tl + 1) and 0xFF
        } else if (prevT0 && !t0Pin) {
            tl = (tl + 1) and 0xFF
        }

        when (mode) {
            0 -> {
                if ((tl and 0x1F) == 0) {
                    th = (th + 1) and 0xFF
                    if (th == 0) setTCONbit(TF0_BIT)
                }
            }
            1 -> {
                if (tl == 0) {
                    th = (th + 1) and 0xFF
                    if (th == 0) setTCONbit(TF0_BIT)
                }
            }
            2 -> {
                if (tl == 0) {
                    tl = state.TH0.toInt()
                    setTCONbit(TF0_BIT)
                }
            }
            3 -> {
                if (tl == 0) setTCONbit(TF0_BIT)
            }
        }

        state.TL0 = tl.toUByte()
        state.TH0 = th.toUByte()
    }

    private fun tickTimer0_TH0(tcon: Int) {
        val tr1 = (tcon and TR1_BIT) != 0
        if (!tr1) return

        var th = state.TH0.toInt()
        th = (th + 1) and 0xFF
        if (th == 0) setTCONbit(TF1_BIT)
        state.TH0 = th.toUByte()
    }

    // ========================================================================
    // Timer 1
    // ========================================================================

    private fun tickTimer1(mode: Int, timer0mode: Int, tmod: Int, tcon: Int, t1Pin: Boolean) {
        if (mode == 3) return

        val timer0StealsTF1 = timer0mode == 3
        val gate = (tmod and GATE1_BIT) != 0
        val ct = (tmod and CT1_BIT) != 0
        val tr = (tcon and TR1_BIT) != 0
        val int1 = (state.getEffectivePort(3) and 0x08) == 0

        if (!tr) return
        if (gate && !int1) return

        var tl = state.TL1.toInt()
        var th = state.TH1.toInt()

        if (!ct) {
            tl = (tl + 1) and 0xFF
        } else if (prevT1 && !t1Pin) {
            tl = (tl + 1) and 0xFF
        }

        if (timer0StealsTF1) {
            // Timer 1 ticks for baud rate but TH0 owns TF1
            if (tl == 0) th = (th + 1) and 0xFF
        } else {
            when (mode) {
                0 -> {
                    if ((tl and 0x1F) == 0) {
                        th = (th + 1) and 0xFF
                        if (th == 0) setTCONbit(TF1_BIT)
                    }
                }
                1 -> {
                    if (tl == 0) {
                        th = (th + 1) and 0xFF
                        if (th == 0) setTCONbit(TF1_BIT)
                    }
                }
                2 -> {
                    if (tl == 0) {
                        tl = state.TH1.toInt()
                        setTCONbit(TF1_BIT)
                    }
                }
            }
        }

        state.TL1 = tl.toUByte()
        state.TH1 = th.toUByte()
    }

    // ========================================================================
    // Timer 2
    // ========================================================================

    private fun tickTimer2(tcon: Int, p1: Int, t2Pin: Boolean, fallingT2EX: Boolean) {
        val t2con = state.T2CON.toInt()
        val tr2 = (t2con and TR2_BIT) != 0
        if (!tr2) return

        val ct2 = (t2con and CT2_BIT) != 0
        val rclk = (t2con and RCLK_BIT) != 0
        val tclk = (t2con and TCLK_BIT) != 0
        val cprl2 = (t2con and CPRL2_BIT) != 0
        val exen2 = (t2con and EXEN2_BIT) != 0
        val t2mod = state.T2MOD.toInt()
        val t2oe = (t2mod and T2OE_BIT) != 0
        val dcen = (t2mod and DCEN_BIT) != 0
        val isBaudGen = rclk || tclk

        var tl = state.TL2.toInt()
        var th = state.TH2.toInt()
        var overflow = false

        if (!ct2) {
            tl = (tl + 1) and 0xFF
        } else if (prevT2 && !t2Pin) {
            tl = (tl + 1) and 0xFF
        }

        if (tl == 0) {
            th = (th + 1) and 0xFF
            if (th == 0) overflow = true
        }

        if (overflow) {
            val rcaph = state.RCAP2H.toInt()
            val rcapl = state.RCAP2L.toInt()

            if (isBaudGen) {
                tl = rcapl
                th = rcaph
            } else if (dcen) {
                val up = (p1 and 0x02) != 0
                if (up) {
                    tl = rcapl
                    th = rcaph
                }
                setT2CONbit(TF2_BIT)
            } else if (!cprl2) {
                setT2CONbit(TF2_BIT)
                tl = rcapl
                th = rcaph
            } else {
                setT2CONbit(TF2_BIT)
            }

            if (t2oe) {
                state.P1 = ((state.P1.toInt() xor 0x01) and 0xFF).toUByte()
            }
        }

        if (exen2 && fallingT2EX && !isBaudGen) {
            if (cprl2) {
                state.RCAP2H = state.TH2
                state.RCAP2L = state.TL2
            } else {
                th = state.RCAP2H.toInt()
                tl = state.RCAP2L.toInt()
            }
            setT2CONbit(EXF2_BIT)
        }

        state.TL2 = tl.toUByte()
        state.TH2 = th.toUByte()
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    private fun setTCONbit(bit: Int) {
        state.TCON = ((state.TCON.toInt() or bit) and 0xFF).toUByte()
    }

    private fun setT2CONbit(bit: Int) {
        state.T2CON = ((state.T2CON.toInt() or bit) and 0xFF).toUByte()
    }

    fun reset() {
        prevT0 = false
        prevT1 = false
        prevT2 = false
        prevT2EX = false
    }
}
