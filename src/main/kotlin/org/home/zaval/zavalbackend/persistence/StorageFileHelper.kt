package org.home.zaval.zavalbackend.persistence

import org.home.zaval.zavalbackend.configuration.ApplicationConfig
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import kotlin.io.path.fileSize

inline fun <reified T> readObjectFromAbsoluteFilePath(absolutePath: String): T? {
    val filePath = Paths.get(absolutePath)
    if (!Files.exists(filePath)) {
        return null
    }
    return deserializeObject((Files.readString(filePath)))
}

class DirectoryHelper(private val rootDirPath: Path) {
    fun resolveRelative(relativeFilePath: Path): Path {
        return rootDirPath.resolve(relativeFilePath)
    }

    fun toAbsolute(relativeFilePath: Path): Path {
        return resolveRelative(relativeFilePath).toAbsolutePath()
    }

    fun ensureFile(filename: Path): Path {
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

/**
 * By default, object works with files in storage directory.
 * Use absolute methods to work with absolute files.
 */
@Component
class StorageFileHelper(applicationConfig: ApplicationConfig) {

    private val globalStoragePath: Path = applicationConfig.storageDirectoryPath
    private val directoryHelper = DirectoryHelper(globalStoragePath)

    fun resolveRelative(relativeFilePath: Path): Path {
        return directoryHelper.resolveRelative(relativeFilePath)
    }

    fun toAbsolute(relativeFilePath: Path): Path {
        return directoryHelper.toAbsolute(relativeFilePath)
    }

    fun ensureFile(filename: Path): Path {
        return directoryHelper.ensureFile(filename)
    }

    fun appendToFile(data: String, filename: Path) {
        directoryHelper.appendToFile(data, filename)
    }

    fun writeObjectToFile(obj: Any, filename: Path) {
        directoryHelper.writeObjectToFile(obj, filename)
    }

    fun renameFile(oldFilename: Path, newFilename: Path) {
        directoryHelper.renameFile(oldFilename, newFilename)
    }

    fun writeToFile(data: String, filename: Path) {
        directoryHelper.writeToFile(data, filename)
    }

    fun fileExists(filename: Path): Boolean {
        return directoryHelper.fileExists(filename)
    }

    fun checkFileSizeInBytes(relativeFilePath: Path): Long {
        return directoryHelper.checkFileSizeInBytes(relativeFilePath)
    }

    fun allFilenamesInDir(dir: Path): List<Path> {
        return directoryHelper.allFilenamesInDir(dir)
    }

    fun <T> readObjectFromFile(filename: Path, objClass: Class<T>): T? {
        return directoryHelper.readObjectFromFile(filename, objClass)
    }

    fun readFile(filename: Path): String? {
        return directoryHelper.readFile(filename)
    }

    fun readAllLinesFromFile(filename: Path): List<String>? {
        return directoryHelper.readAllLinesFromFile(filename)
    }

    fun removeFile(filename: Path) {
        directoryHelper.removeFile(filename)
    }
}

/**
 * By default, object works with files in storage directory.
 * Use absolute methods to work with absolute files.
 */
@Component
class ObsidianVaultHelper(applicationConfig: ApplicationConfig) {

    final val obsidianVaultPath: Path? = applicationConfig.obsidianVaultPath
    private val directoryHelper = obsidianVaultPath?.let { DirectoryHelper(it) }

    private fun throwVaultNotConfigured(): Nothing {
        throw RuntimeException("Obsidian vault is not configured")
    }

    fun resolveRelative(relativeFilePath: Path): Path {
        return directoryHelper?.resolveRelative(relativeFilePath)
            ?: throwVaultNotConfigured()
    }

    fun toAbsolute(relativeFilePath: Path): Path {
        return directoryHelper?.toAbsolute(relativeFilePath)
            ?: throwVaultNotConfigured()
    }

    fun ensureFile(filename: Path): Path {
        return directoryHelper?.ensureFile(filename)
            ?: throwVaultNotConfigured()
    }

    fun appendToFile(data: String, filename: Path) {
        directoryHelper?.appendToFile(data, filename)
            ?: throwVaultNotConfigured()
    }

    fun writeObjectToFile(obj: Any, filename: Path) {
        directoryHelper?.writeObjectToFile(obj, filename)
            ?: throwVaultNotConfigured()
    }

    fun renameFile(oldFilename: Path, newFilename: Path) {
        directoryHelper?.renameFile(oldFilename, newFilename)
            ?: throwVaultNotConfigured()
    }

    fun writeToFile(data: String, filename: Path) {
        directoryHelper?.writeToFile(data, filename)
            ?: throwVaultNotConfigured()
    }

    fun fileExists(filename: Path): Boolean {
        return directoryHelper?.fileExists(filename)
            ?: throwVaultNotConfigured()
    }

    fun checkFileSizeInBytes(relativeFilePath: Path): Long {
        return directoryHelper?.checkFileSizeInBytes(relativeFilePath)
            ?: throwVaultNotConfigured()
    }

    fun allFilenamesInDir(dir: Path): List<Path> {
        return directoryHelper?.allFilenamesInDir(dir)
            ?: throwVaultNotConfigured()
    }

    fun <T> readObjectFromFile(filename: Path, objClass: Class<T>): T? {
        return directoryHelper?.readObjectFromFile(filename, objClass)
            ?: throwVaultNotConfigured()
    }

    fun readFile(filename: Path): String? {
        return directoryHelper?.readFile(filename)
            ?: throwVaultNotConfigured()
    }

    fun readAllLinesFromFile(filename: Path): List<String>? {
        return directoryHelper?.readAllLinesFromFile(filename)
            ?: throwVaultNotConfigured()
    }

    fun removeFile(filename: Path) {
        directoryHelper?.removeFile(filename)
            ?: throwVaultNotConfigured()
    }
}