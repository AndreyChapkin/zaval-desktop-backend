package org.home.zaval.zavalbackend.persistence

import org.home.zaval.zavalbackend.dto.IdentifiedDto
import org.home.zaval.zavalbackend.util.filesInTheDir
import java.lang.RuntimeException
import java.nio.file.Path
import kotlin.io.path.name

/**
 * Find file number with entity id
 * id -> file number
 */
class IdMap(
    fileName: String = "indices-id",
    storageDirPath: Path,
) : SavedMap<Long, Int>(
    fileName = fileName,
    storageDirPath = storageDirPath,
    keySerializer = LongSerializer(),
    valueSerializer = IntSerializer(),
)

data class EntityFileContentInfo(
    val entityNumber: Int,
    val totalRecordNumber: Int,
)

sealed class EntityFileStatus(val fileNumber: Int)

sealed class InformedFileStatus(
    fileNumber: Int,
    val info: EntityFileContentInfo
) : EntityFileStatus(fileNumber)

class CreatedFileStatus(
    fileNumber: Int,
    info: EntityFileContentInfo
) : InformedFileStatus(fileNumber, info)

class ExistingFileStatus(
    fileNumber: Int,
    info: EntityFileContentInfo
) : InformedFileStatus(fileNumber, info)

class DeletedFileStatus(fileNumber: Int) : EntityFileStatus(fileNumber)

interface CrudStorage<T : IdentifiedDto> {
    fun saveEntity(entity: T)
    fun updateEntity(entity: T)
    fun readEntities(ids: Collection<Long>): List<T>
    fun removeEntities(ids: Collection<Long>)
}

abstract class MemoryManageable {

    abstract val isBusy: Boolean

    abstract fun estimateOccupancy(): Double
    abstract fun reduceSize()
    protected abstract fun markAsBusy()
    protected abstract fun markAsIdle()

    /**
     * Throw exception with custom busy message
     */
    protected fun throwBusyException(): Nothing {
        throw RuntimeException("${javaClass.simpleName} is busy")
    }

    protected inline fun <R> reserveWhileDo(action: () -> R): R {
        if (isBusy) {
            throwBusyException()
        }
        markAsBusy()
        val result = action()
        markAsIdle()
        return result
    }
}

class EntityFilesManager<T : IdentifiedDto>(
    subdirName: String,
    storageDirPath: Path,
    entityClass: Class<T>,
    private val maxEntityNumberInFile: Int = 10,
) : MemoryManageable() {
    private val entityFiles = EntityFiles(subdirName, storageDirPath, entityClass)

    /**
     * Entity id to file number
     */
    private val idMap = IdMap(storageDirPath = storageDirPath)

    /**
     * File number to entity ids
     */
    private val incompleteFileMap = SavedMap(
        fileName = "incomplete-files.txt",  // file number -> entity number
        storageDirPath = storageDirPath,
        keySerializer = IntSerializer(),
        valueSerializer = IntSerializer(),
    )

    override val isBusy: Boolean
        get() = entityFiles.isBusy || idMap.isBusy || incompleteFileMap.isBusy

    fun saveEntity(entity: T) {
        reserveWhileDo {
            val incompleteFileNumber = incompleteFileMap.data
                .takeIf { it.isNotEmpty() }
                ?.keys
                ?.random()
            // save
            val fileStatus = entityFiles.saveOrUpdateEntity(entity, incompleteFileNumber) as InformedFileStatus
            // update incomplete files info
            if (fileStatus.info.entityNumber < maxEntityNumberInFile) {
                incompleteFileMap.put(fileStatus.fileNumber, fileStatus.info.entityNumber)
            } else {
                incompleteFileMap.remove(fileStatus.fileNumber)
            }
            // update id map
            idMap.put(entity.id, fileStatus.fileNumber)
        }
    }

    fun updateEntity(entity: T) {
        reserveWhileDo {
            val fileNumber = idMap.get(entity.id)
                ?: throw RuntimeException("Trying to update not existing entity: ${entity.javaClass.simpleName} - ${entity.id}")
            entityFiles.saveOrUpdateEntity(entity, fileNumber)
        }
    }

    fun readEntities(ids: Collection<Long>): List<T> {
        return reserveWhileDo {
            val existingIds = ids.filter { idMap.get(it) != null }
            val fileNumberAndEntityIds = existingIds.groupBy {
                idMap.get(it)!!
            }
            return@reserveWhileDo fileNumberAndEntityIds.entries
                .flatMap { (fileNumber, ids) ->
                    entityFiles.readEntities(ids, fileNumber)
                }
        }
    }

    fun removeEntities(ids: Collection<Long>) {
        reserveWhileDo {
            val existingIds = ids.filter { idMap.get(it) != null }
            val fileNumberAndEntityIds = existingIds.groupBy {
                idMap.get(it)!!
            }
            // Remove from disk
            val fileStatuses = fileNumberAndEntityIds.entries.mapNotNull { (fileNumber, ids) ->
                entityFiles.removeEntities(ids, fileNumber)
            }
            // Update id map
            existingIds.forEach {
                idMap.remove(it)
            }
            // Update incomplete files map
            fileStatuses.forEach {
                when (it) {
                    is InformedFileStatus -> {
                        incompleteFileMap.put(it.fileNumber, it.info.entityNumber)
                    }

                    is DeletedFileStatus -> {
                        incompleteFileMap.remove(it.fileNumber)
                    }
                }
            }
        }
    }

    override fun estimateOccupancy(): Double {
        val entityFileOccupancy = entityFiles.estimateOccupancy()
        val idOccupancy = idMap.estimateOccupancy()
        val incompleteOccupancy = incompleteFileMap.estimateOccupancy()
        return (entityFileOccupancy + idOccupancy + incompleteOccupancy) / 3
    }

    override fun reduceSize() {
        entityFiles.reduceSize()
    }

    override fun markAsBusy() {
        // nothing to do
    }

    override fun markAsIdle() {
        // nothing to do
    }
}

class EntityFiles<T : IdentifiedDto>(
    subdirName: String,
    storageDirPath: Path,
    private val entityClass: Class<T>,
) : MemoryManageable() {
    private val entitiesDirPath = storageDirPath.resolve(subdirName)
    private val ENTITIES_FILENAME_TEMPLATE = "entities-%s.txt"
    private val RECORD_ID_ENTITY_SEPARATOR = ">>>>>>"
    private val ENTITY_RECORD_SEPARATOR = "\n:::::%#%&@$::::::\n"
    private var isBusyPrivate = false
    override val isBusy: Boolean
        get() = isBusyPrivate

    /**
     * if null file number then create and write to new file
     */
    fun saveOrUpdateEntity(entity: T, fileNumber: Int?): EntityFileStatus {
        return reserveWhileDo {
            var createFile = false
            val resultFileNumber = fileNumber ?: run {
                createFile = true
                seekNewFileNumber()
            }
            val newRecords = listOf(entity.entityToRecord())
            val entityFilePath = resultFileNumber.toEntityFilePath()
            return@reserveWhileDo if (createFile) {
                ensureContentOrDeleteFile(newRecords, entityFilePath)
            } else {
                entityFilePath.panicIfStrange()
                appendToFile(newRecords, entityFilePath)
            }
        }
    }

    fun readEntities(ids: Collection<Long>, fileNumber: Int): List<T> {
        return reserveWhileDo {
            val path = fileNumber.toEntityFilePath()
                .panicIfStrange()
            return@reserveWhileDo findEntityRecordsInFile(path, ids)
                .map { it.recordToEntity() }
        }
    }

    fun removeEntities(removeIds: Collection<Long>, fileNumber: Int): EntityFileStatus? {
        return reserveWhileDo {
            val filePath = fileNumber.toEntityFilePath()
                .panicIfStrange()
            val allRecords = getAllRecordsFromFile(filePath)
            // id -> record with no repetitions
            val allEntityRecordsMap = foldRecords(allRecords).toMutableMap()
            val removedRecords = removeIds.mapNotNull {
                allEntityRecordsMap.remove(it)
            }
            val survivedRecords = allEntityRecordsMap.values
            if (removedRecords.isNotEmpty()) {
                return@reserveWhileDo ensureContentOrDeleteFile(survivedRecords, filePath)
            }
            return@reserveWhileDo null
        }
    }

    /**
     * Time-consuming operation
     */
    override fun reduceSize() {
        reserveWhileDo {
            val allFilePaths = filesInTheDir(entitiesDirPath)
            for (path in allFilePaths) {
                val allRecords = getAllRecordsFromFile(path)
                val optimisedRecords = foldRecords(allRecords).values
                if (allRecords.size != optimisedRecords.size) {
                    ensureContentOrDeleteFile(optimisedRecords, path)
                }
            }
        }
    }

    /**
     * Time-consuming operation
     */
    override fun estimateOccupancy(): Double {
        return reserveWhileDo {
            var allRecordsCount = 0
            var foldedRecordsCount = 0
            filesInTheDir(entitiesDirPath).forEach {
                val allRecords = getAllRecordsFromFile(it)
                allRecordsCount += allRecords.size
                foldedRecordsCount += foldRecords(allRecords).size
            }
            return@reserveWhileDo foldedRecordsCount.toDouble() / allRecordsCount
        }
    }

    override fun markAsBusy() {
        isBusyPrivate = true
    }

    override fun markAsIdle() {
        isBusyPrivate = false
    }

    private fun ensureContentOrDeleteFile(entityRecords: Collection<String>, filePath: Path): EntityFileStatus {
        if (entityRecords.isEmpty()) {
            StorageFileWorker.removeFile(filePath)
            return DeletedFileStatus(filePath.toFileNumber())
        }
        val newFileContent = entityRecords.joinToString(ENTITY_RECORD_SEPARATOR)
        val isExistedFile = StorageFileWorker.fileExists(filePath)
        StorageFileWorker.writeToFile(newFileContent, filePath)
        val info = EntityFileContentInfo(
            entityNumber = entityRecords.size,
            totalRecordNumber = entityRecords.size
        )
        return if (isExistedFile) {
            ExistingFileStatus(filePath.toFileNumber(), info)
        } else {
            CreatedFileStatus(filePath.toFileNumber(), info)
        }
    }

    private fun appendToFile(entityRecords: Collection<String>, filePath: Path): EntityFileStatus {
        val appendPortion = "${ENTITY_RECORD_SEPARATOR}${entityRecords.joinToString(ENTITY_RECORD_SEPARATOR)}"
        StorageFileWorker.appendToFile(appendPortion, filePath)
        // construct file info
        val allRecords = getAllRecordsFromFile(filePath)
        val totalRecordNumber = allRecords.size
        val entityRecordsNumber = foldRecords(allRecords).size
        return ExistingFileStatus(
            fileNumber = filePath.toFileNumber(),
            info = EntityFileContentInfo(
                entityNumber = entityRecordsNumber,
                totalRecordNumber = totalRecordNumber,
            )
        )
    }

    /**
     * Make map where all records are actual entity records with no old version records.
     */
    private fun foldRecords(allRecords: Collection<String>): Map<Long, String> {
        return allRecords.associateBy { it.idOfRecord() }
    }

    private fun String.recordToEntity(): T {
        val (_, entityString) = this.split(RECORD_ID_ENTITY_SEPARATOR)
        return JsonHelper.deserializeObject(entityString, entityClass)
    }

    private fun T.entityToRecord(): String {
        val id = this.id
        val entityStr = JsonHelper.serializeObject(this)
        return "${id}${RECORD_ID_ENTITY_SEPARATOR}${entityStr}"
    }

    private fun String.idOfRecord(): Long {
        val idStr = this.split(RECORD_ID_ENTITY_SEPARATOR).first()
        return idStr.toLong()
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

    private fun Path.panicIfStrange(): Path {
        if (!StorageFileWorker.fileExists(this)) {
            throw RuntimeException("File does not exit: ${toAbsolutePath()}")
        }
        return this
    }

    private fun getAllRecordsFromFile(filePath: Path): List<String> {
        return StorageFileWorker.readFile(filePath)
            ?.split(ENTITY_RECORD_SEPARATOR)
            ?: return emptyList()
    }

    private fun Int.toEntityFilePath(): Path {
        val entityFilename = ENTITIES_FILENAME_TEMPLATE.format(this)
        return entitiesDirPath.resolve(entityFilename)
    }

    private fun Path.toFileNumber(): Int {
        val name = this.name
        val lastDashIndex = name.lastIndexOf("-")
        val lastDotIndex = name.lastIndexOf(".")
        return name.substring(lastDashIndex + 1, lastDotIndex).toInt()
    }

    private fun seekNewFileNumber(): Int {
        val fileNames = filesInTheDir(entitiesDirPath)
        val usedNumbers: List<Int> = fileNames
            .map { it.toFileNumber() }
            .sorted()
        var freeNumber = 0
        if (usedNumbers.isNotEmpty()) {
            freeNumber = usedNumbers.last() + 1
            for (i in 0 until usedNumbers.lastIndex) {
                // if there is a gap between numbers - use it
                val curElem = usedNumbers[i]
                val nextElem = usedNumbers[i + 1]
                if (nextElem > curElem + 1) {
                    freeNumber = curElem + 1
                    break
                }
            }
        }
        return freeNumber
    }
}