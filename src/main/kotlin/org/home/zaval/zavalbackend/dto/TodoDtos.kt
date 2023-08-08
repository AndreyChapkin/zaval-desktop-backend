package org.home.zaval.zavalbackend.dto

import org.home.zaval.zavalbackend.entity.value.TodoStatus
import org.home.zaval.zavalbackend.util.IdentifiedDto

data class MoveTodoDto(
    val todoId: Long,
    val parentId: Long?,
)

class TodoDto(
    id: Long,
    val name: String,
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
)

class TodoHierarchyDto(
    id: Long,
    val name: String,
    val status: TodoStatus,
    var parents: List<TodoHierarchyDto> = listOf(),
    var children: List<TodoHierarchyDto>? = listOf()
) : IdentifiedDto(id)

class TodoHistoryDto(val todoId: Long, val records: List<String>)

class TodoBranchDto(
    var parents: List<TodoDto>,
    var leaves: List<TodoDto>,
)
