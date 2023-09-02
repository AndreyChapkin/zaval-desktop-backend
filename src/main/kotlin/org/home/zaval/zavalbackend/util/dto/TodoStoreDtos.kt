package org.home.zaval.zavalbackend.util.dto

class TodoPersistedValues(
    var idSequence: Long,
)

class FilesInfoCache(
    val incompleteFilenames: MutableMap<String, Int>
)