package org.home.zaval.zavalbackend.persistence

import org.home.zaval.zavalbackend.store.ApplicationConfigStore
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

/**
 * By default, object works with files in storage directory.
 * Use absolute methods to work with absolute files.
 */
object StorageFileWorker {

    fun resolveRelative(relativeFilePath: Path): Path {
        return Paths.get(ApplicationConfigStore.config.storageDirectory).resolve(relativeFilePath)
    }

    private fun ensureFile(filename: Path): Path {
        var existingFilePath = resolveRelative(filename)
        if (!Files.exists(existingFilePath)) {
            Files.createDirectories(existingFilePath.parent)
            existingFilePath = Files.createFile(existingFilePath)
        }
        return existingFilePath
    }

    fun appendToFile(data: String, filename: Path) {
        val existingFilePath = ensureFile(filename)
        Files.writeString(existingFilePath, data, StandardOpenOption.APPEND)
    }

    fun writeObjectToFile(obj: Any, filename: Path) {
        val existingFilePath = ensureFile(filename)
        val serializedObj = JsonHelper.serializeObject(obj)
        Files.writeString(existingFilePath, serializedObj, StandardOpenOption.TRUNCATE_EXISTING)
    }

    fun writeToFile(data: String, filename: Path) {
        val existingFilePath = ensureFile(filename)
        Files.writeString(existingFilePath, data, StandardOpenOption.TRUNCATE_EXISTING)
    }

    fun fileExists(filename: Path): Boolean {
        val resolvedFilePath = resolveRelative(filename)
        return Files.exists(resolvedFilePath)
    }

    inline fun <reified T> readObjectFromAbsoluteFilePath(absolutePath: String): T? {
        val filePath = Paths.get(absolutePath)
        if (!Files.exists(filePath)) {
            return null
        }
        return JsonHelper.deserializeObject((Files.readString(filePath)))
    }

    inline fun <reified T> readObjectFromFile(filename: Path): T? {
        val filePath = resolveRelative(filename)
        if (!Files.exists(filePath)) {
            return null
        }
        return JsonHelper.deserializeObject((Files.readString(filePath)))
    }

    fun <T> readObjectFromFile(filename: Path, objClass: Class<T>): T? {
        val filePath = resolveRelative(filename)
        if (!Files.exists(filePath)) {
            return null
        }
        return JsonHelper.deserializeObject(Files.readString(filePath), objClass)
    }

    fun readFile(filename: Path): String? {
        val filePath = resolveRelative(filename)
        if (!Files.exists(filePath)) {
            return null
        }
        return Files.readString(filePath)
    }

    fun removeFile(filename: Path) {
        val filePath = resolveRelative(filename)
        Files.deleteIfExists(filePath)
    }

}