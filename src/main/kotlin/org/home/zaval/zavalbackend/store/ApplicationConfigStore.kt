package org.home.zaval.zavalbackend.store

import org.home.zaval.zavalbackend.persistence.ApplicationConfig
import org.home.zaval.zavalbackend.persistence.StorageFileWorker

object ApplicationConfigStore {
    const val CONFIG_FILE_PATH = "./config.json"

    lateinit var config: ApplicationConfig

    fun loadApplicationConfig(): ApplicationConfig? {
        return StorageFileWorker.readObjectFromAbsoluteFilePath(CONFIG_FILE_PATH)
    }

    fun createDefaultConfig(): ApplicationConfig = ApplicationConfig(
        storageDirectory = ".\\zaval-data",
        saveArchivesToDirectory = ".\\zaval-data-archives",
        maxArchivesNumber = 2,
    )
}