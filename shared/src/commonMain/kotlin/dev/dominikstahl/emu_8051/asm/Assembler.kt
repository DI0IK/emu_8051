package dev.dominikstahl.emu_8051.asm

fun assemble(source: String): AssemblyResult {
    val tokens = tokenize(source)
    val statements = parseStatements(tokens)
    return assembleInternal(statements)
}

class AssemblyResult private constructor(
    val rom: UByteArray?,
    val errors: List<AssemblyError>,
    val warnings: List<AssemblyWarning>,
    val sourceMap: Map<Int, Int> = emptyMap(),
) {
    val isSuccess: Boolean get() = rom != null

    companion object {
        fun success(rom: UByteArray, sourceMap: Map<Int, Int> = emptyMap(), warnings: List<AssemblyWarning> = emptyList()) =
            AssemblyResult(rom, emptyList(), warnings, sourceMap)
        fun failure(errors: List<AssemblyError>, warnings: List<AssemblyWarning> = emptyList()) =
            AssemblyResult(null, errors, warnings)
    }
}

data class AssemblyError(val line: Int, val message: String)
data class AssemblyWarning(val line: Int, val message: String)
