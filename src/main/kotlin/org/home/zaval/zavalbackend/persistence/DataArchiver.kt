package org.home.zaval.zavalbackend.persistence;

import org.home.zaval.zavalbackend.util.safeNewFileAbsolutePath
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.io.path.name


class DataArchiver(val storageDirAbsolutePath: Path, val dirToSaveZipAbsolutePath: Path, val zipName: String) {

    fun createArchive() {
        if (Files.exists(storageDirAbsolutePath)) {
            val prevArchivePath = dirToSaveZipAbsolutePath.resolve("$zipName.zip")
            if (Files.exists(prevArchivePath)) {
                throw RuntimeException("There is already archive: $prevArchivePath")
            }
            zipDir(storageDirAbsolutePath, dirToSaveZipAbsolutePath, zipName)
        }
    }

    fun restoreDataFromArchive() {
        val storageDirName = storageDirAbsolutePath.name
        val outdatedStorageDirPath = storageDirAbsolutePath.parent.resolve("$storageDirName-outdated")
        Files.move(storageDirAbsolutePath, outdatedStorageDirPath, StandardCopyOption.REPLACE_EXISTING)
        val zipAbsolutePath = dirToSaveZipAbsolutePath.resolve("$zipName.zip")
        unzipDir(storageDirAbsolutePath.parent, zipAbsolutePath)
    }

    // Zip dir with all children of the dir
    private fun zipDir(dirToZipAbsolutePath: Path, dirToSaveZipAbsolutePath: Path, zipName: String) {
        if (!Files.isDirectory(dirToZipAbsolutePath)) {
            return
        }
        if (!Files.exists(dirToSaveZipAbsolutePath)) {
            Files.createDirectories(dirToSaveZipAbsolutePath)
        }
        val zipFullPathStr = dirToSaveZipAbsolutePath.resolve("$zipName.zip").toString()
        FileOutputStream(zipFullPathStr).use { fos ->
            ZipOutputStream(fos).use { zipOut ->
                val absoluteFilePathsToZip = LinkedList<Path>()
                // Add all root children
                Files.newDirectoryStream(dirToZipAbsolutePath).use { dirStream ->
                    dirStream.forEach {
                        absoluteFilePathsToZip.add(it)
                    }
                }
                // Process all-level children
                while (absoluteFilePathsToZip.isNotEmpty()) {
                    val curAbsolutePath = absoluteFilePathsToZip.pollFirst()!!
                    val curRelativeFilename = dirToZipAbsolutePath.relativize(curAbsolutePath)
                    val curRelativeToZipRootFilenameStr =
                        dirToZipAbsolutePath.fileName.resolve(curRelativeFilename).toString()
                    if (Files.isDirectory(curAbsolutePath)) {
                        // plan to process all inner files and directories
                        Files.newDirectoryStream(curAbsolutePath).use { dirStream ->
                            dirStream.forEach {
                                absoluteFilePathsToZip.add(it)
                            }
                        }
                        // Create zip entry for the directory and that's it
                        continue
                    }
                    // It's file. Write the file to the zip
                    FileInputStream(curAbsolutePath.toString()).use { fis ->
                        zipOut.putNextEntry(ZipEntry(curRelativeToZipRootFilenameStr))
                        val bytes = ByteArray(1024)
                        var length: Int = fis.read(bytes)
                        while (length >= 0) {
                            zipOut.write(bytes, 0, length)
                            length = fis.read(bytes)
                        }
                    }
                }
            }
        }
    }

    private fun unzipDir(dirToUnzipAbsolutePath: Path, zipAbsolutePath: Path) {
        if (!Files.exists(dirToUnzipAbsolutePath)) {
            Files.createDirectories(dirToUnzipAbsolutePath)
        }
        val zis = ZipInputStream(FileInputStream(zipAbsolutePath.toString()))
        try {
            var zipEntry = zis.nextEntry
            while (zipEntry != null) {
                val newSafeFileAbsolutePath = safeNewFileAbsolutePath(dirToUnzipAbsolutePath, Paths.get(zipEntry.name))
                if (zipEntry.isDirectory) {
                    // It's directory
                    // create it inside root if it doesn't exist
                    if (!Files.isDirectory(newSafeFileAbsolutePath)) {
                        throw IOException("Failed to create directory $newSafeFileAbsolutePath. It's not a directory.")
                    }
                    if (!Files.exists(newSafeFileAbsolutePath)) {
                        Files.createDirectories(newSafeFileAbsolutePath)
                    }
                } else {
                    // it's file
                    // create parent dir inside root dir if it doesn't exist
                    val parent = newSafeFileAbsolutePath.parent
                    if (parent != null && !Files.exists(parent) && parent != dirToUnzipAbsolutePath) {
                        Files.createDirectories(parent)
                    }
                    // write file content
                    FileOutputStream(newSafeFileAbsolutePath.toString()).use { fos ->
                        val buffer = ByteArray(1024)
                        var length = zis.read(buffer)
                        while (length >= 0) {
                            fos.write(buffer, 0, length)
                            length = zis.read(buffer)
                        }
                    }
                }
                zipEntry = zis.nextEntry
            }
        } finally {
            zis.closeEntry()
            zis.close()
        }
    }
}
