package dev.dominikstahl.emu_8051.platform

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.posix.fclose
import platform.posix.fgets
import platform.posix.fopen
import platform.posix.fputs

@OptIn(ExperimentalForeignApi::class)
class IosFileStorage : FileStorage {
    private val storageDir: String by lazy {
        val paths = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory, NSUserDomainMask, true
        )
        val dir = "${paths.first()}/asm"
        val fileManager = NSFileManager.defaultManager
        if (!fileManager.fileExistsAtPath(dir)) {
            fileManager.createDirectoryAtPath(dir, true, null, null)
        }
        dir
    }

    override suspend fun save(name: String, content: String): Boolean {
        return try {
            val path = "$storageDir/$name"
            memScoped {
                val file = fopen(path, "w")
                    ?: return@memScoped false
                fputs(content, file)
                fclose(file)
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun load(name: String): String? {
        return try {
            val path = "$storageDir/$name"
            memScoped {
                val file = fopen(path, "r")
                    ?: return@memScoped null
                val buffer = allocArray<ByteVar>(4096)
                val sb = StringBuilder()
                while (true) {
                    val line = fgets(buffer, 4096, file) ?: break
                    sb.append(buffer.toKString())
                }
                fclose(file)
                sb.toString()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun list(): List<String> {
        return try {
            val fileManager = NSFileManager.defaultManager
            val contents = fileManager.contentsOfDirectoryAtPath(storageDir, null)
                ?: return emptyList()
            @Suppress("UNCHECKED_CAST")
            (contents as List<String>)
                .filter { it.endsWith(".asm") }
                .sorted()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun delete(name: String): Boolean {
        return try {
            val fileManager = NSFileManager.defaultManager
            fileManager.removeItemAtPath("$storageDir/$name", null)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun rename(oldName: String, newName: String): Boolean {
        return try {
            val fileManager = NSFileManager.defaultManager
            fileManager.moveItemAtPath("$storageDir/$oldName", "$storageDir/$newName", null)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

actual fun createFileStorage(): FileStorage = IosFileStorage()
