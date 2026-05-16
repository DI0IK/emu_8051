package dev.dominikstahl.emu_8051.platform

import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask

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
            (content as NSString).writeToFile(path, true, NSUTF8StringEncoding, null)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun load(name: String): String? {
        return try {
            val path = "$storageDir/$name"
            NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null)
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
