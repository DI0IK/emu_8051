package dev.dominikstahl.emu_8051.platform

import kotlinx.browser.localStorage

class WasmJsFileStorage : FileStorage {
    companion object {
        private const val PREFIX = "asm_"
    }

    override suspend fun save(name: String, content: String): Boolean {
        return try {
            localStorage.setItem("$PREFIX$name", content)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun load(name: String): String? {
        return localStorage.getItem("$PREFIX$name")
    }

    override suspend fun list(): List<String> {
        val result = mutableListOf<String>()
        for (i in 0 until localStorage.length) {
            val key = localStorage.key(i) ?: continue
            if (key.startsWith(PREFIX)) {
                result.add(key.removePrefix(PREFIX))
            }
        }
        return result.sorted()
    }

    override suspend fun delete(name: String): Boolean {
        return try {
            localStorage.removeItem("$PREFIX$name")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

actual fun createFileStorage(): FileStorage = WasmJsFileStorage()
