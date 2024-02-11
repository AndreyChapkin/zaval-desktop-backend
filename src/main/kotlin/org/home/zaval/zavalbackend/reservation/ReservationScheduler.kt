package org.home.zaval.zavalbackend.reservation

import org.home.zaval.zavalbackend.configuration.ApplicationConfig
import org.home.zaval.zavalbackend.configuration.DatabaseProperties
import org.home.zaval.zavalbackend.persistence.StorageFileHelper
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.Scope
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.RuntimeException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import kotlin.io.path.absolutePathString
import kotlin.io.path.name

data class DumpFileDescriptor(
    val number: Int,
)

@Component
@Scope("singleton")
@Profile("local")
class ReservationScheduler(
    private val storageFileHelper: StorageFileHelper,
    applicationConfig: ApplicationConfig,
    databaseProperties: DatabaseProperties
) {

    private final val databaseDirPath: Path = applicationConfig.databasePath
    val psqlFilePath: Path = databaseDirPath.resolve("bin/psql.exe").also {
        if (Files.notExists(it)) {
            throw RuntimeException("psql file does not exist")
        }
    }
    val pgDumpFilePath: Path = databaseDirPath.resolve("bin/pg_dump.exe").also {
        if (Files.notExists(it)) {
            throw RuntimeException("pgDump file does not exist")
        }
    }
    val databaseUsername = databaseProperties.username
    val databasePassword = databaseProperties.password
    val databaseHost = databaseProperties.url.let {
        // parse database url
        // jdbc:postgresql://localhost:5432/zaval_backend
        it.substring(it.indexOf("//") + 2, it.lastIndexOf(":"))
    }
    val databasePort = databaseProperties.url.let {
        it.substring(it.lastIndexOf(":") + 1, it.lastIndexOf("/"))
    }
    val databaseName = databaseProperties.url.let {
        it.substring(it.lastIndexOf("/") + 1)
    }
    val MAX_DUMP_NUMBER = 4
    val dumpFilesDirRelativePath = Paths.get("database_dump")
    // dump file name = zaval_dump-1-active or zaval_dump-1
    final val DUMP_FILE_PREFIX = "zaval_dump-"

    @Scheduled(fixedRate = 3, timeUnit = TimeUnit.HOURS)
    fun reserve() {
        println("start database data reservation...")
        val activeFileDescriptor = newActiveFileDumpDescriptor()
        val activeFilePath = descriptorToFilePath(activeFileDescriptor)
        readDumpFromDb(activeFilePath)
        normalizeDumpFiles()
        println("database reservation is finished")
    }

    private fun writeDumpToDb(dumpFilePath: Path) {
        val absoluteDumpFilePath = storageFileHelper.toAbsolute(dumpFilePath)
        val processBuilder = ProcessBuilder(
            psqlFilePath.absolutePathString(),
            "-h", databaseHost,
            "-p", databasePort,
            "-U", databaseUsername,
            databaseName,
        )
            .redirectErrorStream(true)
            .redirectInput(absoluteDumpFilePath.toFile())
        processBuilder.environment().putAll(mapOf(
            "PGPASSWORD" to databasePassword
        ))
        val process = processBuilder.start()
        val exitCode = process.waitFor()
        println("\nExited with error code : $exitCode")
    }

    fun readDumpFromDb(dumpFilePath: Path) {
        val processBuilder = ProcessBuilder(
            pgDumpFilePath.absolutePathString(),
            "--encoding", "utf8",
            "-h", databaseHost,
            "-p", databasePort,
            "-U", databaseUsername,
            databaseName
        )
            .redirectErrorStream(true)
        processBuilder.environment().putAll(mapOf(
            "PGPASSWORD" to databasePassword
        ))
        val process = processBuilder.start()
        val reader = BufferedReader(InputStreamReader(process.inputStream, Charsets.UTF_8))
        var line: String? = reader.readLine()
        val stringBuilder = StringBuilder()
        while (line != null) {
            stringBuilder.append(line).append("\n")
            line = reader.readLine()
        }
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            throw RuntimeException("Failed to dump database with code: ${exitCode}")
        }
        storageFileHelper.writeToFile(stringBuilder.toString(), dumpFilePath)
    }

    private fun descriptorToFilePath(descriptor: DumpFileDescriptor): Path {
        return dumpFilesDirRelativePath.resolve(
            DUMP_FILE_PREFIX + descriptor.number
        )
    }

    private fun filePathToDescriptor(filePath: Path): DumpFileDescriptor {
        val filename = filePath.name
        val number = filename.substringAfter(DUMP_FILE_PREFIX)
            .toInt()
        return DumpFileDescriptor(number)
    }

    private fun allSortedDumpFileDescriptors(): List<DumpFileDescriptor> {
        val allDumpFileNames = storageFileHelper.allFilenamesInDir(dumpFilesDirRelativePath)
        return allDumpFileNames
            .map(::filePathToDescriptor)
            .sortedBy { it.number }
    }

    private fun newActiveFileDumpDescriptor(): DumpFileDescriptor {
        val allSortedDumpFileDescriptors = allSortedDumpFileDescriptors()
        if (allSortedDumpFileDescriptors.isEmpty()) {
            return DumpFileDescriptor(1)
        }
        return DumpFileDescriptor(allSortedDumpFileDescriptors.last().number + 1)
    }

    private fun normalizeDumpFiles() {
        val allSortedDumpFileDescriptors = allSortedDumpFileDescriptors()
        // if more files than MAX_DUMP_NUMBER - remove the oldest ones
        if (allSortedDumpFileDescriptors.size > MAX_DUMP_NUMBER) {
            val excessiveNumber = allSortedDumpFileDescriptors.size - MAX_DUMP_NUMBER
            val oldestDumpFileDescriptors = allSortedDumpFileDescriptors.subList(0, excessiveNumber)
            for (descriptor in oldestDumpFileDescriptors) {
                val filePath = descriptorToFilePath(descriptor)
                storageFileHelper.removeFile(filePath)
            }
        }
    }
}