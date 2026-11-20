package dev.dominikstahl.emu_8051.engine

@OptIn(ExperimentalUnsignedTypes::class)
class TimerController(private val state: CpuState) {

    // Ports reset to 0xFF (all pins high)
    private var prevT0High: Boolean = true
    private var prevT1High: Boolean = true
    private var prevT2High: Boolean = true
    private var prevT2EXHigh: Boolean = true

    fun tick() {
        val p3 = state.getEffectivePort(3)
        val p1 = state.getEffectivePort(1)

        val t0High = (p3 and 0x10) != 0
        val t1High = (p3 and 0x20) != 0
        val t2High = (p1 and 0x01) != 0
        val t2exHigh = (p1 and 0x02) != 0

        val fallingT0 = prevT0High && !t0High
        val fallingT1 = prevT1High && !t1High
        val fallingT2 = prevT2High && !t2High
        val fallingT2EX = prevT2EXHigh && !t2exHigh

        val tmod = state.TMOD.toInt()
        val tcon = state.TCON.toInt()
        val t2con = state.T2CON.toInt()

        val timer0mode = tmod and 0x03
        val timer1mode = (tmod shr 4) and 0x03

        if ((tcon and TR0_BIT) != 0) {
            tickTimer0(timer0mode, tmod, p3, fallingT0)
        }
        if (timer0mode == 3 && (tcon and TR1_BIT) != 0) {
            tickTimer0_TH0()
        }
        if (timer1mode != 3 && (timer0mode == 3 || (tcon and TR1_BIT) != 0)) {
            tickTimer1(timer1mode, timer0mode, tmod, p3, fallingT1)
        }
        if ((t2con and TR2_BIT) != 0) {
            tickTimer2(t2con, t2exHigh, fallingT2, fallingT2EX)
        }

        prevT0High = t0High
        prevT1High = t1High
        prevT2High = t2High
        prevT2EXHigh = t2exHigh
    }

    // ========================================================================
    // Timer 0
    // ========================================================================

    private fun tickTimer0(mode: Int, tmod: Int, p3: Int, fallingT0: Boolean) {
        val gate = (tmod and GATE0_BIT) != 0
        val ct = (tmod and CT0_BIT) != 0
        val int0High = (p3 and 0x04) != 0

        if (gate && !int0High) return
        if (ct && !fallingT0) return

        var tl = state.TL0.toInt()
        var th = state.TH0.toInt()

        when (mode) {
            0 -> {
                tl = (tl + 1) and 0x1F
                if (tl == 0) {
                    th = (th + 1) and 0xFF
                    if (th == 0) setTCONbit(TF0_BIT)
                }
            }
            1 -> {
                tl = (tl + 1) and 0xFF
                if (tl == 0) {
                    th = (th + 1) and 0xFF
                    if (th == 0) setTCONbit(TF0_BIT)
                }
            }
            2 -> {
                tl = (tl + 1) and 0xFF
                if (tl == 0) {
                    tl = th
                    setTCONbit(TF0_BIT)
                }
            }
            3 -> {
                tl = (tl + 1) and 0xFF
                if (tl == 0) setTCONbit(TF0_BIT)
            }
        }

        state.TL0 = tl.toUByte()
        state.TH0 = th.toUByte()
    }

    private fun tickTimer0_TH0() {
        val th = (state.TH0.toInt() + 1) and 0xFF
        if (th == 0) setTCONbit(TF1_BIT)
        state.TH0 = th.toUByte()
    }

    // ========================================================================
    // Timer 1
    // ========================================================================

    private fun tickTimer1(mode: Int, timer0mode: Int, tmod: Int, p3: Int, fallingT1: Boolean) {
        if (mode == 3) return

        val timer0StealsTF1 = timer0mode == 3
        val gate = (tmod and GATE1_BIT) != 0
        val ct = (tmod and CT1_BIT) != 0
        val int1High = (p3 and 0x08) != 0

        if (gate && !int1High) return
        if (ct && !fallingT1) return

        var tl = state.TL1.toInt()
        var th = state.TH1.toInt()

        when (mode) {
            0 -> {
                tl = (tl + 1) and 0x1F
                if (tl == 0) {
                    th = (th + 1) and 0xFF
                    if (th == 0 && !timer0StealsTF1) setTCONbit(TF1_BIT)
                }
            }
            1 -> {
                tl = (tl + 1) and 0xFF
                if (tl == 0) {
                    th = (th + 1) and 0xFF
                    if (th == 0 && !timer0StealsTF1) setTCONbit(TF1_BIT)
                }
            }
            2 -> {
                tl = (tl + 1) and 0xFF
                if (tl == 0) {
                    tl = th
                    if (!timer0StealsTF1) setTCONbit(TF1_BIT)
                }
            }
        }

        state.TL1 = tl.toUByte()
        state.TH1 = th.toUByte()
    }

    // ========================================================================
    // Timer 2
    // ========================================================================

    private fun tickTimer2(t2con: Int, t2exHigh: Boolean, fallingT2: Boolean, fallingT2EX: Boolean) {
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
        val stepped = !ct2 || fallingT2

        if (stepped) {
            val rcaph = state.RCAP2H.toInt()
            val rcapl = state.RCAP2L.toInt()
            var overflow = false

            if (dcen && !isBaudGen && !cprl2) {
                if (t2exHigh) {
                    tl = (tl + 1) and 0xFF
                    if (tl == 0) {
                        th = (th + 1) and 0xFF
                        if (th == 0) overflow = true
                    }
                    if (overflow) {
                        tl = rcapl
                        th = rcaph
                        setT2CONbit(TF2_BIT)
                        toggleT2CONbit(EXF2_BIT)
                    }
                } else {
                    if (th == rcaph && tl == rcapl) {
                        overflow = true
                        tl = 0xFF
                        th = 0xFF
                        setT2CONbit(TF2_BIT)
                        toggleT2CONbit(EXF2_BIT)
                    } else {
                        if (tl == 0) {
                            tl = 0xFF
                            th = (th - 1) and 0xFF
                        } else {
                            tl = (tl - 1) and 0xFF
                        }
                    }
                }
            } else {
                tl = (tl + 1) and 0xFF
                if (tl == 0) {
                    th = (th + 1) and 0xFF
                    if (th == 0) overflow = true
                }
                if (overflow) {
                    if (isBaudGen) {
                        tl = rcapl
                        th = rcaph
                    } else if (!cprl2) {
                        setT2CONbit(TF2_BIT)
                        tl = rcapl
                        th = rcaph
                    } else {
                        setT2CONbit(TF2_BIT)
                    }
                }
            }

            if (overflow && t2oe) {
                state.P1 = ((state.P1.toInt() xor 0x01) and 0xFF).toUByte()
            }
        }

        if (exen2 && fallingT2EX && !dcen) {
            setT2CONbit(EXF2_BIT)
            if (!isBaudGen) {
                if (cprl2) {
                    state.RCAP2H = th.toUByte()
                    state.RCAP2L = tl.toUByte()
                } else {
                    th = state.RCAP2H.toInt()
                    tl = state.RCAP2L.toInt()
                }
            }
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

    private fun toggleT2CONbit(bit: Int) {
        state.T2CON = ((state.T2CON.toInt() xor bit) and 0xFF).toUByte()
    }

    fun reset() {
        prevT0High = true
        prevT1High = true
        prevT2High = true
        prevT2EXHigh = true
    }
}
