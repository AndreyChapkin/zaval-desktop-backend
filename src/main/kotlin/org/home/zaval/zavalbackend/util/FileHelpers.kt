package org.home.zaval.zavalbackend.util

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

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

enum class LoadingResult {
    LOADED, DEFAULT
}

data class LoadingInfo(val filename: String, val result: LoadingResult)

val String.path: Path
    get() = Paths.get(this)