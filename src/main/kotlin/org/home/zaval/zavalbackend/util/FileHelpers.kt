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

val String.path: Path
    get() = Paths.get(this)