package org.home.zaval.zavalbackend.dto

import org.home.zaval.zavalbackend.entity.value.TodoStatus
import org.home.zaval.zavalbackend.util.IdentifiedDto
import org.home.zaval.zavalbackend.util.asStringFormattedWithISO8601withOffset
import java.time.OffsetDateTime

data class MoveTodoDto(
    val todoId: Long,
    val parentId: Long?,
)

class TodoDto(
    id: Long,
    val name: String,
    val priority: Int = 0,
    val status: TodoStatus,
    val parentId: Long? = null,
) : IdentifiedDto(id)

data class CreateTodoDto(
    val name: String,
    val status: TodoStatus,
    val parentId: Long? = null,
)

data class UpdateTodoDto(
    val name: String,
    val status: TodoStatus,
    val priority: Int,
)

class TodoHierarchyDto(
    id: Long,
    val name: String,
    val priority: Int = 0,
    val status: TodoStatus,
    var parents: List<TodoHierarchyDto> = listOf(),
    var children: List<TodoHierarchyDto>? = listOf()
) : IdentifiedDto(id)

class TodoHistoryDto(val todoId: Long, val records: List<String>)

class TodoBranchDto(
    var parents: List<TodoDto>,
    var leaves: List<TodoDto>,
)
