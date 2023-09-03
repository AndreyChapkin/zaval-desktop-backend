package org.home.zaval.zavalbackend.util.dto

class TodoPersistedValues(
    var idSequence: Long,
)

class FilesInfoCache(
    val incompleteFilenames: MutableMap<String, Int>
)

class AggregationInfoDto(
    val childToParentIds: MutableMap<Long, Long>,
    val parentToChildrenIds: MutableMap<Long, MutableList<Long>>
)