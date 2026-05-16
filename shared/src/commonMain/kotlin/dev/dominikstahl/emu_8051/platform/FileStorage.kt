package dev.dominikstahl.emu_8051.platform

interface FileStorage {
    suspend fun save(name: String, content: String): Boolean
    suspend fun load(name: String): String?
    suspend fun list(): List<String>
    suspend fun delete(name: String): Boolean
    suspend fun rename(oldName: String, newName: String): Boolean {
        val content = load(oldName) ?: return false
        if (!save(newName, content)) return false
        delete(oldName)
        return true
    }
}

expect fun createFileStorage(): FileStorage
