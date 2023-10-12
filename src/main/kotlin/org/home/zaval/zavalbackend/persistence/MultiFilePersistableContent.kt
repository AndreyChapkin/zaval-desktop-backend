package org.home.zaval.zavalbackend.persistence

import org.home.zaval.zavalbackend.util.*
import org.home.zaval.zavalbackend.dto.persistence.FilesInfoCache
import org.home.zaval.zavalbackend.exception.ExcessiveEntitiesException
import org.home.zaval.zavalbackend.exception.IncorrectEntityUpdateException
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import kotlin.io.path.name

class MultiFilePersistableObjects<T : Any>(
    private val relativeToStorageRootDirPath: String,
    private val entityClass: Class<T>,
    private val idExtractor: (T) -> String,
    baseEntityFilename: String = "entities",
    private val separator: String = "\n::::::::::::::::\n",
    private val maxEntitiesInFile: Int = 4,
) {
    val ENTITIES_DIR_NAME = "entities"
    val ENTITIES_FILENAME_TEMPLATE = "${baseEntityFilename}-%s.txt"
    val INDICIES_FILENAME = "indicies-${baseEntityFilename}.json"
    val FILES_INFO_CACHE = "files-info-cache.json"

    val filesInfoCache = PersistableObject<FilesInfoCache>(resolve(FILES_INFO_CACHE))
    val indices = PersistableObject<MutableMap<String, String>>(resolve(INDICIES_FILENAME))

    fun loadTechnicalFiles(): Array<LoadingInfo> {
        val infoCacheResult = filesInfoCache.load {
            FilesInfoCache(mutableMapOf())
        }
        val indicesResult = indices.load {
            mutableMapOf()
        }
        return arrayOf(
            LoadingInfo("${relativeToStorageRootDirPath}/${FILES_INFO_CACHE}", infoCacheResult.result),
            LoadingInfo("${relativeToStorageRootDirPath}/${INDICIES_FILENAME}", indicesResult.result)
        )
    }

    fun readEntity(entityId: Any): T? {
        val idKey = entityId.toString()
        val fileName = indices.readObj[idKey]
        if (fileName != null) {
            val entities = readEntitiesInFilename(fileName)
            return entities.find { idExtractor(it) == idKey }
        }
        return null
    }

    fun readAllEntities(): List<T> {
        val result = mutableListOf<T>()
        filesInTheDir(resolve(ENTITIES_DIR_NAME)).forEach {
            result.addAll(readEntitiesInFilename(it.fileName.toString()))
        }
        return result
    }

    fun saveEntity(entity: T) {
        val entityId = idExtractor(entity)
        val incompleteFileFilename = filesInfoCache.readObj.incompleteFilenames
            .keys
            .takeIf { it.size > 0 }
            ?.first()
        // TODO: move to separate methods
        ensurePersistenceForAll(indices, filesInfoCache) {
            if (incompleteFileFilename != null) {
                // Save to incomplete file
                appendEntity(entity, incompleteFileFilename)
                val newCount = filesInfoCache.readObj.incompleteFilenames[incompleteFileFilename]!! + 1
                if (newCount < maxEntitiesInFile) {
                    filesInfoCache.modObj.incompleteFilenames[incompleteFileFilename] = newCount
                } else {
                    filesInfoCache.modObj.incompleteFilenames.remove(incompleteFileFilename)
                }
                indices.modObj[entityId] = incompleteFileFilename
            } else {
                // Or save to new file
                val newFilename = newEntitiesFileName()
                writeEntities(listOf(entity), newFilename)
                filesInfoCache.modObj.incompleteFilenames[newFilename] = 1
                indices.modObj[entityId] = newFilename
            }
        }
        // Perhaps this files info is outdated if there is no incomplete filenames
        CompletableFuture.runAsync {
            updateFilesInfo()
        }
    }

    fun updateEntity(freshEntity: T): T {
        val entityId = idExtractor(freshEntity)
        val updater: () -> T? = {
            // Try to update
            var outdated: T? = null
            val existingFilename = indices.readObj[entityId]
            if (existingFilename != null) {
                // Update in file
                val savedEntities = readEntitiesInFilename(existingFilename)
                val updatedEntities = savedEntities.map { savedEntity ->
                    if (idExtractor(savedEntity) == entityId) {
                        outdated = savedEntity
                        freshEntity
                    } else savedEntity
                }
                if (outdated != null) {
                    writeEntities(updatedEntities, existingFilename)
                }
            }
            outdated
        }
        // Try to update
        var outdatedEntity: T? = updater()
        if (outdatedEntity == null) {
            // Update didn't happen. Indices could be corrupted
            restoreIndices()
            // Make one more try
            outdatedEntity = updater()
            if (outdatedEntity == null) {
                throw IncorrectEntityUpdateException(entityId)
            }
        }
        return outdatedEntity
    }

    fun removeEntity(entityId: Any): T? {
        val idKey = entityId.toString()
        val filename = indices.readObj[idKey]
        if (filename != null) {
            val savedEntities = readEntitiesInFilename(filename)
            var outdatedEntity: T? = null
            val updatedEntities = savedEntities.filter { savedEntity ->
                val curId = idExtractor(savedEntity)
                if (idKey == curId) {
                    outdatedEntity = savedEntity
                }
                idKey != curId
            }
            val noMoreEntitiesInFile = updatedEntities.isEmpty()
            if (!noMoreEntitiesInFile) {
                writeEntities(updatedEntities, filename)
            }
            // TODO: move to separate methods
            // Manage excessive empty files and cache
            ensurePersistenceForAll(filesInfoCache, indices) {
                val newCount = filesInfoCache.modObj.incompleteFilenames
                    .computeIfAbsent(filename) { maxEntitiesInFile } - 1
                if (noMoreEntitiesInFile || newCount < 1) {
                    filesInfoCache.modObj.incompleteFilenames.remove(filename)
                    StorageFileWorker.removeFile(resolve(filename))
                } else {
                    filesInfoCache.modObj.incompleteFilenames[filename] = newCount
                }
                indices.modObj.remove(idKey)
            }
            return outdatedEntity
        }
        return null
    }

    // TODO: update indices
    fun deleteAllEntities() {
        val fileNames = getAllFullEntityFilePaths()
        fileNames.forEach { StorageFileWorker.removeFile(it) }
        StorageFileWorker.removeFile(resolve(ENTITIES_DIR_NAME))
        StorageFileWorker.removeFile(resolve(INDICIES_FILENAME))
        StorageFileWorker.removeFile(resolve(FILES_INFO_CACHE))
    }

    private fun readEntitiesInFilename(filename: String): List<T> {
        val entitiesStr = StorageFileWorker.readFile(resolve(filename))
        if (entitiesStr != null) {
            return extractEntities(entitiesStr)
        }
        return emptyList()
    }

    private fun writeEntities(todos: List<T>, filename: String) {
        val todosStr = mergeEntities(todos)
        StorageFileWorker.writeToFile(todosStr, resolve(filename))
    }

    private fun appendEntity(entity: T, filename: String) {
        val appendPortion = "${separator}${JsonHelper.serializeObject(entity)}"
        StorageFileWorker.appendToFile(appendPortion, resolve(filename))
    }

    private fun extractEntities(entitiesStr: String): List<T> {
        return entitiesStr.split(separator).map {
            JsonHelper.deserializeObject(it, entityClass)
        }.toList()
    }

    private fun mergeEntities(entities: List<T>): String {
        return entities.map { JsonHelper.serializeObject(it) }
            .joinToString(separator)
    }

    private fun getAllFullEntityFilePaths(): List<Path> = filesInTheDir(resolve(ENTITIES_DIR_NAME))

    private fun newEntitiesFileName(): String {
        val entitiesDir = resolve(ENTITIES_DIR_NAME)
        val fileNames = filesInTheDir(entitiesDir)
        val usedNumbers: List<Int> = fileNames.map {
            val name = it.fileName.toString()
            val lastDashIndex = name.lastIndexOf("-")
            val lastDotIndex = name.lastIndexOf(".")
            val number = name.substring(lastDashIndex + 1, lastDotIndex).toInt()
            number
        }.sorted()
        var freeNumber = 0
        if (usedNumbers.isNotEmpty()) {
            freeNumber = usedNumbers.last() + 1
            var i = 0
            while (i < usedNumbers.lastIndex) {
                // if there is a gap between numbers - use it
                val curElem = usedNumbers[i]
                val nextElem = usedNumbers[i + 1]
                if (nextElem > curElem + 1) {
                    freeNumber = curElem + 1
                    break
                }
                i++
            }
        }
        return ENTITIES_FILENAME_TEMPLATE.format(freeNumber)
    }

    private fun updateFilesInfo() {
        val entityFileFullPaths = getAllFullEntityFilePaths()
        val incompleteFilenamesAndCounts = mutableMapOf<String, Int>()
        for (entityFilePath in entityFileFullPaths) {
            val fileContent = StorageFileWorker.readFile(entityFilePath)!!
            val separatorCount = countPatternInString(separator, fileContent)
            // If there are no max count of entities in the file
            val entitiesCount = separatorCount + 1
            if (entitiesCount < maxEntitiesInFile) {
                val fileName = entityFilePath.name
                incompleteFilenamesAndCounts[fileName] = entitiesCount
            }
        }
        ensurePersistence(filesInfoCache) {
            incompleteFilenamesAndCounts.entries.forEach { (filename, count) ->
                filesInfoCache.modObj.incompleteFilenames[filename] = count
            }
        }
    }

    private fun updateIndices(entityId: String, filename: String, remove: Boolean = false) {
        ensurePersistence(indices) {
            if (remove) {
                indices.modObj.remove(entityId)
            } else {
                indices.modObj[entityId] = filename
            }
        }
    }

    private fun restoreIndices() {
        val excessiveEntities: MutableMap<String, MutableList<String>> = mutableMapOf()
        ensurePersistence(indices) {
            indices.modObj.clear()
            val entityFilePaths = getAllFullEntityFilePaths()
            val facedEntities: MutableSet<String> = mutableSetOf()
            for (entityFilePath in entityFilePaths) {
                val fileName = entityFilePath.fileName.toString()
                val savedEntities = readEntitiesInFilename(fileName)
                savedEntities.forEach {
                    val savedId = idExtractor(it)
                    if (facedEntities.contains(savedId)) {
                        excessiveEntities
                            .computeIfAbsent(savedId) { mutableListOf() }
                            .apply { add(fileName) }
                    } else {
                        facedEntities.add(savedId)
                        indices.modObj[savedId] = fileName
                    }
                }
            }
        }
        if (excessiveEntities.isNotEmpty()) {
            throw ExcessiveEntitiesException(excessiveEntities.toString())
        }
    }

    private fun resolve(filename: String): Path {
        val fullDirPath = StorageFileWorker.resolveRelative(relativeToStorageRootDirPath.path)
        return when (filename) {
            INDICIES_FILENAME, ENTITIES_DIR_NAME, FILES_INFO_CACHE -> fullDirPath.resolve(filename)
            else -> fullDirPath.resolve("${ENTITIES_DIR_NAME}/${filename}")
        }
    }
}