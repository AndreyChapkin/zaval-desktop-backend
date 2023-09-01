package org.home.zaval.zavalbackend.util.singleton

import org.home.zaval.zavalbackend.util.dto.ApplicationConfig

object ApplicationConfigStore {
    const val CONFIG_FILE_PATH = "./config.json"

    lateinit var config: ApplicationConfig

    fun loadApplicationConfig(): ApplicationConfig? {
        return StorageFileWorker.readObjectFromAbsoluteFilePath(CONFIG_FILE_PATH)
    }

    fun createDefaultConfig(): ApplicationConfig = ApplicationConfig(
        storageDirectory = ".",
        numberOfTodosInFile = 4,
    )
}