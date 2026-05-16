package dev.dominikstahl.emu_8051.engine

internal const val CY_BIT = 0x80
internal const val AC_BIT = 0x40
internal const val OV_BIT = 0x04

internal const val TF1_BIT = 0x80
internal const val TR1_BIT = 0x40
internal const val TF0_BIT = 0x20
internal const val TR0_BIT = 0x10
internal const val IE1_BIT = 0x08
internal const val IT1_BIT = 0x04
internal const val IE0_BIT = 0x02
internal const val IT0_BIT = 0x01

internal const val EA_BIT = 0x80
internal const val ET2_BIT = 0x20
internal const val ES_BIT = 0x10
internal const val ET1_BIT = 0x08
internal const val EX1_BIT = 0x04
internal const val ET0_BIT = 0x02
internal const val EX0_BIT = 0x01

internal const val PT2_BIT = 0x20
internal const val PS_BIT = 0x10
internal const val PT1_BIT = 0x08
internal const val PX1_BIT = 0x04
internal const val PT0_BIT = 0x02
internal const val PX0_BIT = 0x01

internal const val TF2_BIT = 0x80
internal const val EXF2_BIT = 0x40
internal const val RCLK_BIT = 0x20
internal const val TCLK_BIT = 0x10
internal const val EXEN2_BIT = 0x08
internal const val TR2_BIT = 0x04
internal const val CT2_BIT = 0x02
internal const val CPRL2_BIT = 0x01

internal const val GATE1_BIT = 0x80
internal const val CT1_BIT = 0x40
internal const val M11_BIT = 0x20
internal const val M01_BIT = 0x10
internal const val GATE0_BIT = 0x08
internal const val CT0_BIT = 0x04
internal const val M10_BIT = 0x02
internal const val M00_BIT = 0x01

internal const val T2OE_BIT = 0x02
internal const val DCEN_BIT = 0x01

internal const val SM0_BIT = 0x80
internal const val SM1_BIT = 0x40
internal const val SM2_BIT = 0x20
internal const val REN_BIT = 0x10
internal const val TB8_BIT = 0x08
internal const val RB8_BIT = 0x04
internal const val TI_BIT = 0x02
internal const val RI_BIT = 0x01

internal const val TCON_ADDR = 0x88
internal const val TMOD_ADDR = 0x89
internal const val IE_ADDR = 0xA8
internal const val IP_ADDR = 0xB8
internal const val SCON_ADDR = 0x98
internal const val T2CON_ADDR = 0xC8
internal const val T2MOD_ADDR = 0xC9
