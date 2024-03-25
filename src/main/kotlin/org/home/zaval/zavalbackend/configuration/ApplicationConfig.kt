package org.home.zaval.zavalbackend.configuration

import org.home.zaval.zavalbackend.persistence.deserializeObject
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.lang.RuntimeException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.name

class ApplicationConfig(
    val databasePath: Path,
    val storageDirectoryPath: Path,
    val obsidianVaultPath: Path? = null,
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
        val obsidianVaultPath = rawAppConfig[ApplicationConfig::obsidianVaultPath.name].also {
            // check if the vault path is present and exists
            var vaultDirIsIncorrect = it == null || Files.notExists(Paths.get(it)) || !Files.isDirectory(Paths.get(it))
            if (!vaultDirIsIncorrect) {
                Files.newDirectoryStream(Paths.get(it!!)).use { stream ->
                    stream.forEach { path ->
                        if (path.name === ".obsidian") {
                            vaultDirIsIncorrect = false
                            return@use
                        }
                    }
                }
            }
            if (vaultDirIsIncorrect) {
                println("Obsidian vault path does not exist or incorrect.\n!!!Obsidian integration will be disabled")
            }
        }?.let {
            println("Obsidian vault path detected.\nObsidian integration is enabled")
            Paths.get(it)
        }
        return ApplicationConfig(
            databasePath = databasePath,
            storageDirectoryPath = storageDirectoryPath,
            obsidianVaultPath = obsidianVaultPath
        )
    }
}