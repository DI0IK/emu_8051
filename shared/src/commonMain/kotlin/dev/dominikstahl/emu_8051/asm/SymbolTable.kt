package dev.dominikstahl.emu_8051.asm

enum class SymType { SFR, BIT, EQU, DATA, CODE, LABEL }

class SymbolTable {
    private val values = mutableMapOf<String, Int>()
    private val types = mutableMapOf<String, SymType>()
    private val deferred = mutableMapOf<String, Expr>()

    fun predefined(name: String, value: Int, type: SymType) {
        values[name] = value
        types[name] = type
    }

    fun set(name: String, value: Int, type: SymType) {
        values[name] = value
        types[name] = type
    }

    fun defer(name: String, expr: Expr) {
        deferred[name] = expr
    }

    fun resolveDeferred(pc: Int) {
        var changed = true
        while (changed) {
            changed = false
            val keys = deferred.keys.toList()
            for (name in keys) {
                val expr = deferred[name] ?: continue
                try {
                    val v = evalExpr(expr, this, pc)
                    values[name] = v
                    types[name] = types[name] ?: SymType.EQU
                    deferred.remove(name)
                    changed = true
                } catch (_: Exception) {
                }
            }
        }
    }

    fun get(name: String): Int? {
        val upper = name.uppercase()
        return values[upper] ?: values[name]
    }

    fun getType(name: String): SymType? {
        val upper = name.uppercase()
        return types[upper] ?: types[name]
    }

    fun has(name: String): Boolean = values.containsKey(name) || values.containsKey(name.uppercase())
}

fun initPredefinedSymbols(table: SymbolTable) {
    // SFRs
    table.predefined("P0", 0x80, SymType.SFR)
    table.predefined("SP", 0x81, SymType.SFR)
    table.predefined("DPL", 0x82, SymType.SFR)
    table.predefined("DPH", 0x83, SymType.SFR)
    table.predefined("PCON", 0x87, SymType.SFR)
    table.predefined("TCON", 0x88, SymType.SFR)
    table.predefined("TMOD", 0x89, SymType.SFR)
    table.predefined("TL0", 0x8A, SymType.SFR)
    table.predefined("TL1", 0x8B, SymType.SFR)
    table.predefined("TH0", 0x8C, SymType.SFR)
    table.predefined("TH1", 0x8D, SymType.SFR)
    table.predefined("AUXR", 0x8E, SymType.SFR)
    table.predefined("CKCON", 0x8F, SymType.SFR)
    table.predefined("P1", 0x90, SymType.SFR)
    table.predefined("SCON", 0x98, SymType.SFR)
    table.predefined("SBUF", 0x99, SymType.SFR)
    table.predefined("P2", 0xA0, SymType.SFR)
    table.predefined("IE", 0xA8, SymType.SFR)
    table.predefined("P3", 0xB0, SymType.SFR)
    table.predefined("IP", 0xB8, SymType.SFR)
    table.predefined("T2CON", 0xC8, SymType.SFR)
    table.predefined("T2MOD", 0xC9, SymType.SFR)
    table.predefined("RCAP2L", 0xCA, SymType.SFR)
    table.predefined("RCAP2H", 0xCB, SymType.SFR)
    table.predefined("TL2", 0xCC, SymType.SFR)
    table.predefined("TH2", 0xCD, SymType.SFR)
    table.predefined("PSW", 0xD0, SymType.SFR)
    table.predefined("ACC", 0xE0, SymType.SFR)
    table.predefined("B", 0xF0, SymType.SFR)

    // PSW bits
    table.predefined("CY", 0xD7, SymType.BIT)
    table.predefined("AC", 0xD6, SymType.BIT)
    table.predefined("F0", 0xD5, SymType.BIT)
    table.predefined("RS1", 0xD4, SymType.BIT)
    table.predefined("RS0", 0xD3, SymType.BIT)
    table.predefined("OV", 0xD2, SymType.BIT)
    table.predefined("P", 0xD0, SymType.BIT)

    // TCON bits
    table.predefined("TF1", 0x8F, SymType.BIT)
    table.predefined("TR1", 0x8E, SymType.BIT)
    table.predefined("TF0", 0x8D, SymType.BIT)
    table.predefined("TR0", 0x8C, SymType.BIT)
    table.predefined("IE1", 0x8B, SymType.BIT)
    table.predefined("IT1", 0x8A, SymType.BIT)
    table.predefined("IE0", 0x89, SymType.BIT)
    table.predefined("IT0", 0x88, SymType.BIT)

    // SCON bits
    table.predefined("SM0", 0x9F, SymType.BIT)
    table.predefined("SM1", 0x9E, SymType.BIT)
    table.predefined("SM2", 0x9D, SymType.BIT)
    table.predefined("REN", 0x9C, SymType.BIT)
    table.predefined("TB8", 0x9B, SymType.BIT)
    table.predefined("RB8", 0x9A, SymType.BIT)
    table.predefined("TI", 0x99, SymType.BIT)
    table.predefined("RI", 0x98, SymType.BIT)

    // IE bits
    table.predefined("EA", 0xAF, SymType.BIT)
    table.predefined("ET2", 0xAD, SymType.BIT)
    table.predefined("ES", 0xAC, SymType.BIT)
    table.predefined("ET1", 0xAB, SymType.BIT)
    table.predefined("EX1", 0xAA, SymType.BIT)
    table.predefined("ET0", 0xA9, SymType.BIT)
    table.predefined("EX0", 0xA8, SymType.BIT)

    // IP bits
    table.predefined("PT2", 0xBD, SymType.BIT)
    table.predefined("PS", 0xBC, SymType.BIT)
    table.predefined("PT1", 0xBB, SymType.BIT)
    table.predefined("PX1", 0xBA, SymType.BIT)
    table.predefined("PT0", 0xB9, SymType.BIT)
    table.predefined("PX0", 0xB8, SymType.BIT)

    // T2CON bits
    table.predefined("TF2", 0xCF, SymType.BIT)
    table.predefined("EXF2", 0xCE, SymType.BIT)
    table.predefined("RCLK", 0xCD, SymType.BIT)
    table.predefined("TCLK", 0xCC, SymType.BIT)
    table.predefined("EXEN2", 0xCB, SymType.BIT)
    table.predefined("TR2", 0xCA, SymType.BIT)
    table.predefined("CT2", 0xC9, SymType.BIT)
    table.predefined("CPRL2", 0xC8, SymType.BIT)
}
