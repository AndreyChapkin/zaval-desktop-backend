package org.home.zaval.zavalbackend.configuration

import org.home.zaval.zavalbackend.persistence.deserializeObject
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.lang.RuntimeException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class ApplicationConfig(
    val databasePath: Path,
    val storageDirectoryPath: Path,
)

const val CONFIG_FILE_PATH = "./config.json"
const val DEFAULT_STORAGE_DIRECTORY_PATH = "./zaval-storage"

@Configuration
class ApplicationConfiguration {

    @Bean
    fun applicationConfig(): ApplicationConfig {
        val configurationFileContent = Files.readString(Path.of(CONFIG_FILE_PATH), Charsets.UTF_8)
        val rawAppConfig: Map<String, String> = deserializeObject(configurationFileContent)
        val databasePath = rawAppConfig[ApplicationConfig::databasePath.name]?.let {
            if (Files.notExists(Paths.get(it))) {
                throw RuntimeException("Database path does not exist")
            }
            Paths.get(it)
        }
            ?: throw RuntimeException("Database path is not specified")
        val storageDirectoryPath = rawAppConfig[ApplicationConfig::storageDirectoryPath.name]?.let {
            if (Files.notExists(Paths.get(it))) {
                throw RuntimeException("Storage directory does not exist")
            }
            Paths.get(it)
        }
            ?: Paths.get(DEFAULT_STORAGE_DIRECTORY_PATH)
        return ApplicationConfig(
            databasePath = databasePath,
            storageDirectoryPath = storageDirectoryPath,
        )
    }
}