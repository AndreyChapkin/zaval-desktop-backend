package org.home.zaval.zavalbackend.util

import org.home.zaval.zavalbackend.dto.TodoDto
import org.home.zaval.zavalbackend.model.Todo
import org.home.zaval.zavalbackend.dto.TodoHierarchyDto
import org.home.zaval.zavalbackend.dto.TodoHistoryDto
import org.home.zaval.zavalbackend.model.TodoHistory
import org.home.zaval.zavalbackend.model.TodoShallowView

const val TODO_HISTORY_DELIMITER = "<;>"

fun Todo.toShallowHierarchyDto() = TodoHierarchyDto(id = this.id!!, name = this.name, status = this.status)

// TODO get rid of
fun TodoShallowView.toDto() = TodoDto(
    id = this.getId() ?: -100,
    name = this.getName(),
    status = this.getStatus(),
    parentId = this.getParentId()
)

fun Todo.toDto() = TodoDto(
    id = this.id!!,
    name = this.name,
    status = this.status,
    parentId = this.parent?.id
)

fun TodoHistory.toDto() = TodoHistoryDto(
    todoId = this.id!!,
    records = extractTodoRecords(this.records)
)

fun extractTodoRecords(records: String) = records.split(TODO_HISTORY_DELIMITER)

fun mergeHistoryRecordsToPersist(records: List<String>) = records.joinToString(TODO_HISTORY_DELIMITER)