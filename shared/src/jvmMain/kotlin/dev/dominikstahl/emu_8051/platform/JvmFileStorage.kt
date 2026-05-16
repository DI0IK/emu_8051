package dev.dominikstahl.emu_8051.platform

import java.io.File

class JvmFileStorage : FileStorage {
    private val storageDir: File = run {
        val osName = System.getProperty("os.name").lowercase()
        val base = when {
            osName.contains("linux") -> {
                System.getenv("XDG_DATA_HOME") ?: "${System.getProperty("user.home")}/.local/share"
            }
            osName.contains("mac") || osName.contains("darwin") -> {
                "${System.getProperty("user.home")}/Library/Application Support"
            }
            osName.contains("win") -> {
                System.getenv("APPDATA") ?: "${System.getProperty("user.home")}/AppData/Roaming"
            }
            else -> "${System.getProperty("user.home")}/.local/share"
        }
        File(base, "emu_8051/asm").also { it.mkdirs() }
    }

    override suspend fun save(name: String, content: String): Boolean {
        return try {
            File(storageDir, name).writeText(content)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun load(name: String): String? {
        return try {
            val file = File(storageDir, name)
            if (file.exists()) file.readText() else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun list(): List<String> {
        return storageDir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".asm") }
            ?.sortedByDescending { it.lastModified() }
            ?.map { it.name }
            ?: emptyList()
    }

    override suspend fun delete(name: String): Boolean {
        return try {
            File(storageDir, name).delete()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun rename(oldName: String, newName: String): Boolean {
        return try {
            File(storageDir, oldName).renameTo(File(storageDir, newName))
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

actual fun createFileStorage(): FileStorage = JvmFileStorage()
