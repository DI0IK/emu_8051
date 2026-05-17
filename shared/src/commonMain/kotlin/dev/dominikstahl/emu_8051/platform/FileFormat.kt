package dev.dominikstahl.emu_8051.platform

import dev.dominikstahl.emu_8051.ui.HwComponentConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

object FileFormat {
    private const val HEADER = "; #emu_8051 "

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    fun encode(sourceCode: String, config: List<HwComponentConfig>): String {
        if (config.isEmpty()) return sourceCode
        val encoded = json.encodeToString(config)
        return "$HEADER$encoded\n$sourceCode"
    }

    fun decode(content: String): Pair<String, List<HwComponentConfig>> {
        val firstNewline = content.indexOf('\n')
        if (firstNewline > 0 && content.startsWith(HEADER)) {
            val jsonStr = content.substring(HEADER.length, firstNewline)
            val config = try {
                json.decodeFromString<List<HwComponentConfig>>(jsonStr)
            } catch (_: Exception) {
                emptyList()
            }
            val sourceCode = content.substring(firstNewline + 1)
            return sourceCode to config
        }
        return content to emptyList()
    }
}
