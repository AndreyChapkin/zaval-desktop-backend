package org.home.zaval.zavalbackend.util

import org.home.zaval.zavalbackend.util.dto.FilesInfoCache
import org.home.zaval.zavalbackend.util.singleton.JsonHelper
import org.home.zaval.zavalbackend.util.singleton.StorageFileWorker
import java.nio.file.Path

class MultiFilePersistableObjects<T : Any>(
    private val relativeStorageRootDirPath: String,
    private val entityClass: Class<T>,
    private val idExtractor: (T) -> Long,
    baseEntityFilename: String = "entities",
    private val separator: String = "\n::::::::::::::::\n",
    private val maxEntitiesInFile: Int = 4,
) {
    val ENTITIES_DIR_NAME = "entities"
    val ENTITIES_FILENAME_TEMPLATE = "${baseEntityFilename}-%s.txt"
    val INDICIES_FILENAME = "indicies-${baseEntityFilename}.json"
    val FILES_INFO_CACHE = "files-info-cache.json"

    val filesInfoCache = PersistableObject<FilesInfoCache>(resolve(FILES_INFO_CACHE))
    val indices = PersistableObject<MutableMap<Long, String>>(resolve(INDICIES_FILENAME))

    fun loadTechnicalFiles(): Array<LoadingInfo> {
        val infoCacheResult = filesInfoCache.load {
            FilesInfoCache(mutableMapOf())
        }
        val indicesResult = indices.load {
            mutableMapOf()
        }
        return arrayOf(
            LoadingInfo("${relativeStorageRootDirPath}/${FILES_INFO_CACHE}", infoCacheResult.result),
            LoadingInfo("${relativeStorageRootDirPath}/${INDICIES_FILENAME}", indicesResult.result)
        )
    }

    fun readAllEntities(): List<T> {
        val result = mutableListOf<T>()
        filesInTheDir(resolve(ENTITIES_DIR_NAME)).forEach {
            result.addAll(readEntities(it.fileName.toString()))
        }
        return result
    }

    fun saveOrUpdateEntity(entity: T): T? {
        val entityId = idExtractor(entity)
        // Try to update
        val existingFilename = indices.readObj[entityId]
        if (existingFilename != null) {
            // Entity already was persisted. Update in file
            val savedEntities = readEntities(existingFilename)
            var outdatedEntity: T? = null
            val updatedEntities = savedEntities.map {
                val curId = idExtractor(it)
                if (curId == entityId) {
                    outdatedEntity = it
                    entity
                } else it
            }
            writeEntities(updatedEntities, existingFilename)
            return outdatedEntity
        }
        // Or save
        val incompleteFileFilename = filesInfoCache.readObj.incompleteFilenames
            .keys
            .takeIf { it.size > 0 }
            ?.first()
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
        return null
    }

    fun removeEntity(entity: T): T? {
        val entityId = idExtractor(entity)
        val filename = indices.readObj[entityId]
        if (filename != null) {
            val savedEntities = readEntities(filename)
            var outdatedEntity: T? = null
            val updatedEntities = savedEntities.filter {
                val curId = idExtractor(it)
                if (entityId == curId) {
                    outdatedEntity = it
                }
                entityId != curId
            }
            val noMoreEntitiesInFile = updatedEntities.isEmpty()
            if (!noMoreEntitiesInFile) {
                writeEntities(updatedEntities, filename)
            }
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
                indices.modObj.remove(entityId)
            }
            return outdatedEntity
        }
        return null
    }

    private fun readEntities(filename: String): List<T> {
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

    private fun resolve(filename: String): Path {
        val fullDirPath = StorageFileWorker.resolveRelative(relativeStorageRootDirPath.path)
        return when (filename) {
            INDICIES_FILENAME, ENTITIES_DIR_NAME, FILES_INFO_CACHE -> fullDirPath.resolve(filename)
            else -> fullDirPath.resolve("${ENTITIES_DIR_NAME}/${filename}")
        }
    }
}