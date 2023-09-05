package org.home.zaval.zavalbackend.util

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.name

fun filesInTheDir(dirPath: Path): List<Path> {
    if (!Files.exists(dirPath)) {
        return emptyList()
    }
    val fileNames: MutableList<Path> = ArrayList()
    Files.newDirectoryStream(dirPath).use {
        it.forEach { path ->
            if (!Files.isDirectory(path)) {
                fileNames.add(path)
            }
        }
    }
    return fileNames
}

// filename example: example-1.txt. Number is there between last hyphen and last dot.
fun numberedFilenamesInDir(dirPath: Path): MutableMap<Int, String> {
    val fileNames = filesInTheDir(dirPath)
    val numberAndFilenames = mutableMapOf<Int, String>()
    fileNames.forEach { filename ->
        val name = filename.name
        val lastDashIndex = name.lastIndexOf("-")
        val lastDotIndex = name.lastIndexOf(".")
        val number = name.substring(lastDashIndex + 1, lastDotIndex).toIntOrNull()
        if (number != null) {
            numberAndFilenames[number] = name
        }
    }
    return numberAndFilenames
}

fun safeNewFileAbsolutePath(dirAbsolutePath: Path, relativeFilePath: Path): Path {
    val destDirPath = Paths.get(dirAbsolutePath.toFile().canonicalPath)
    val destFilePath = Paths.get(destDirPath.resolve(relativeFilePath).toFile().canonicalPath)
    if (!destFilePath.startsWith(destDirPath)) {
        throw RuntimeException("File '${relativeFilePath}' goes out of dir '${dirAbsolutePath}'")
    }
    return destFilePath
}

enum class LoadingResult {
    LOADED, DEFAULT
}

data class LoadingInfo(val filename: String, val result: LoadingResult)

val String.path: Path
    get() = Paths.get(this)