package dev.dominikstahl.emu_8051.platform

import android.content.Context
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object AndroidContext {
    lateinit var appContext: Context
    fun isInitialized(): Boolean = ::appContext.isInitialized
}

private class InMemoryFileStorage : FileStorage {
    private val files = linkedMapOf<String, String>()
    override suspend fun save(name: String, content: String): Boolean {
        files[name] = content
        return true
    }

    override suspend fun load(name: String): String? = files[name]
    override suspend fun list(): List<String> = files.keys.sorted()
    override suspend fun delete(name: String): Boolean = files.remove(name) != null
}

class AndroidFileStorage(context: Context) : FileStorage {
    private val storageDir: File = File(context.filesDir, "asm").also { it.mkdirs() }

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
            val oldFile = File(storageDir, oldName)
            val newFile = File(storageDir, newName)
            if (!oldFile.exists()) return false
            Files.move(oldFile.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

actual fun createFileStorage(): FileStorage =
    if (AndroidContext.isInitialized()) AndroidFileStorage(AndroidContext.appContext)
    else InMemoryFileStorage()
