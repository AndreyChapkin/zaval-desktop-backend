package org.home.zaval.zavalbackend.persistence

import java.nio.file.Path

class PersistableStorage<T : Any>(
    private val globalStorageDirPath: Path,
    private val storageDirName: String,
    private val entityClass: Class<T>,
    private val idExtractor: (T) -> String,
    private val maxEntitiesInFile: Int = 4,
    baseEntityFilename: String = "entities",
) {


}