package org.home.zaval.zavalbackend.persistence

import org.home.zaval.zavalbackend.dto.IdentifiedDto
import org.home.zaval.zavalbackend.util.filesInTheDir
import java.nio.file.Path
import kotlin.io.path.name

class IdIndex<T>(
    indexName: String = "indices-id",
    storageDirPath: Path,
) : SavedMap<Long, String>(
    fileName = indexName,
    storageDirPath = storageDirPath,
    keySerializer = LongSerializer(),
    valueSerializer = StringSerializer(),
)

data class EntityFileStatus(
    val filename: String,
    val entityNumber: Int,
    val totalRecordNumber: Int,
)

class EntityFiles<T : IdentifiedDto>(
    subdirName: String,
    storageDirPath: Path,
    private val entityClass: Class<T>,
) {
    private val entitiesDirPath = storageDirPath.resolve(subdirName)
    private val ENTITIES_FILENAME_TEMPLATE = "entities-%s.txt"
    private val RECORD_ID_ENTITY_SEPARATOR = ">>>>>>"
    private val ENTITY_RECORD_SEPARATOR = "\n:::::%#%&@$::::::\n"

    private fun saveEntity(entity: T, fileNumber: Int?): EntityFileStatus {
        val entityFilePath = fileNumber
            ?.let { numberToEntityFilePath(it) }
            ?: newEntityFilePath()
        val newRecord = entity.entityToRecord()
        // save to file
        StorageFileWorker.appendToFile(newRecord, entityFilePath)
        // construct file status
        val allRecords = getAllRecordsFromFile(entityFilePath)
        var totalRecordNumber = allRecords.size
        val entitiesIndex = mutableMapOf<Long, String>()  // id -> entity repeat number
        for (record in allRecords) {
            entitiesIndex[record.idOfRecord()] = record
        }
        return EntityFileStatus(
            filename = entityFilePath.name,
            entityNumber = entitiesIndex.size,
            totalRecordNumber = totalRecordNumber,
        )
    }

    fun readEntities(ids: Collection<Long>, fileNumber: Int): List<T> {
        return numberToEntityFilePath(fileNumber)
            ?.let { findEntityRecordsInFile(it, ids) }
            ?.map { it.recordToEntity() }
            ?: emptyList()
    }

    fun removeEntities(removeIds: Collection<Long>, fileNumber: Int): List<T> {
        val filePath = numberToEntityFilePath(fileNumber)
        val allRecords = filePath
            ?.let(::getAllRecordsFromFile)
            ?: return emptyList()
        // id -> record with no repetitions
        val allEntityRecordsMap = foldRecords(allRecords).toMutableMap()
        val removedRecords = removeIds.mapNotNull {
            allEntityRecordsMap.remove(it)
        }
        if (removedRecords.isNotEmpty()) {
            val clearedRecords = allEntityRecordsMap.values
            rewriteEntitiesOrDeleteFile(clearedRecords, filePath)
            return removedRecords.map { it.recordToEntity() }
        }
        return emptyList()
    }

    fun reduceFileSizeSynchronously(fileNumber: Int) {
        val filePath = numberToEntityFilePath(fileNumber)
            ?: return
        val allRecords = getAllRecordsFromFile(filePath)
        val foldedRecords = foldRecords(allRecords)
        rewriteEntitiesOrDeleteFile(foldedRecords.values, filePath)
    }

    private fun rewriteEntitiesOrDeleteFile(entityRecords: Collection<String>, filePath: Path) {
        val newFileContent = entityRecords.joinToString(ENTITY_RECORD_SEPARATOR)
        if (newFileContent.isEmpty()) {
            StorageFileWorker.removeFile(filePath)
        } else {
            StorageFileWorker.writeToFile(newFileContent, filePath)
        }
    }

    /**
     * Make map where all records are actual entity records with no old version records.
     */
    private fun foldRecords(allRecords: Collection<String>): Map<Long, String> {
        return allRecords.associateBy { it.idOfRecord() }
    }

    private fun String.recordToEntity(): T {
        val (id, entityString) = this.split(RECORD_ID_ENTITY_SEPARATOR)
        return JsonHelper.deserializeObject(entityString, entityClass)
    }

    private fun T.entityToRecord(): String {
        val id = this.id
        val entityStr = JsonHelper.serializeObject(this)
        return "${id}${RECORD_ID_ENTITY_SEPARATOR}${entityStr}"
    }

    private fun String.idOfRecord(): Long {
        val firstSeparatorChar = RECORD_ID_ENTITY_SEPARATOR.first()
        return this.takeWhile { it != firstSeparatorChar }.toLong()
    }

    // TODO: migrate to RandomAccessFile usage with inverse file reading.
    private fun findEntityRecordsInFile(filePath: Path, ids: Collection<Long>): List<String> {
        val records = getAllRecordsFromFile(filePath)
        val idStrsSet = ids.map(Long::toString).toMutableSet()
        val resultRecords = mutableListOf<String>()
        for (i in records.indices.reversed()) { // because actual entity is always the nearest to the end version
            val record = records[i]
            for (idStr in idStrsSet) {
                if (record.startsWith(idStr)) {
                    resultRecords.add(record)
                    idStrsSet.remove(idStr)
                    break
                }
            }
        }
        return resultRecords
    }

    private fun getAllRecordsFromFile(filePath: Path): List<String> {
        return StorageFileWorker.readFile(filePath)
            ?.split(ENTITY_RECORD_SEPARATOR)
            ?: return emptyList()
    }

    private fun getAllEntityFilePaths(): List<Path> = filesInTheDir(entitiesDirPath)

    private fun numberToEntityFilePath(fileNumber: Int): Path? {
        val entityFilename = ENTITIES_FILENAME_TEMPLATE.format(fileNumber)
        val entityFilePath = entitiesDirPath.resolve(entityFilename)
        return if (StorageFileWorker.fileExists(entityFilePath))
            entityFilePath
        else null
    }

    private fun newEntityFilePath(): Path {
        val fileNames = filesInTheDir(entitiesDirPath)
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
        return entitiesDirPath.resolve(ENTITIES_FILENAME_TEMPLATE.format(freeNumber))
    }
}