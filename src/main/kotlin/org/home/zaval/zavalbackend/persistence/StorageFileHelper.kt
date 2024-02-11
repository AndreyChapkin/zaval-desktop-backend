package org.home.zaval.zavalbackend.persistence

import org.home.zaval.zavalbackend.configuration.ApplicationConfig
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import kotlin.io.path.fileSize
import kotlin.io.path.name

inline fun <reified T> readObjectFromAbsoluteFilePath(absolutePath: String): T? {
    val filePath = Paths.get(absolutePath)
    if (!Files.exists(filePath)) {
        return null
    }
    return deserializeObject((Files.readString(filePath)))
}

/**
 * By default, object works with files in storage directory.
 * Use absolute methods to work with absolute files.
 */
@Component
class StorageFileHelper(applicationConfig: ApplicationConfig) {

    private val globalStoragePath: Path = applicationConfig.storageDirectoryPath

    fun resolveRelative(relativeFilePath: Path): Path {
        return globalStoragePath.resolve(relativeFilePath)
    }

    fun toAbsolute(relativeFilePath: Path): Path {
        return resolveRelative(relativeFilePath).toAbsolutePath()
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
        val serializedObj = serializeObject(obj)
        Files.writeString(existingFilePath, serializedObj, StandardOpenOption.TRUNCATE_EXISTING)
    }

    fun renameFile(oldFilename: Path, newFilename: Path) {
        val oldFilePath = resolveRelative(oldFilename)
        val newFilePath = resolveRelative(newFilename)
        Files.move(oldFilePath, newFilePath)
    }

    fun writeToFile(data: String, filename: Path) {
        val existingFilePath = ensureFile(filename)
        Files.writeString(existingFilePath, data, StandardOpenOption.TRUNCATE_EXISTING)
    }

    fun fileExists(filename: Path): Boolean {
        val resolvedFilePath = resolveRelative(filename)
        return Files.exists(resolvedFilePath)
    }

    fun checkFileSizeInBytes(relativeFilePath: Path): Long {
        val resolvedFilePath = resolveRelative(relativeFilePath)
        if (!fileExists(resolvedFilePath)) {
            return 0L
        }
        return resolvedFilePath.fileSize()
    }

    fun allFilenamesInDir(dir: Path): List<Path> {
        val resolvedDirPath = resolveRelative(dir)
        if (!Files.exists(resolvedDirPath)) {
            return emptyList()
        }
        return Files.list(resolvedDirPath).toList()
    }

    fun <T> readObjectFromFile(filename: Path, objClass: Class<T>): T? {
        val filePath = resolveRelative(filename)
        if (!Files.exists(filePath)) {
            return null
        }
        return deserializeObject(Files.readString(filePath), objClass)
    }

    fun readFile(filename: Path): String? {
        val filePath = resolveRelative(filename)
        if (!Files.exists(filePath)) {
            return null
        }
        return Files.readString(filePath)
    }

    fun readAllLinesFromFile(filename: Path): List<String>? {
        val filePath = resolveRelative(filename)
        if (!Files.exists(filePath)) {
            return null
        }
        return Files.readAllLines(filePath)
    }

    fun removeFile(filename: Path) {
        val filePath = resolveRelative(filename)
        Files.deleteIfExists(filePath)
    }
}