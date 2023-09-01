package org.home.zaval.zavalbackend.dto

import org.home.zaval.zavalbackend.entity.value.TodoStatus

data class MoveTodoDto(
    val todoId: Long,
    val parentId: Long?,
)

data class CreateTodoDto(
    val name: String,
    val status: TodoStatus,
    val parentId: Long? = null,
)

class UpdateTodoDto(
    val general: UpdateTodoGeneralDto?,
    val description: String?
)

class UpdateTodoGeneralDto(
    val name: String,
    val status: TodoStatus,
    val priority: Int,
)

class LightTodoDto(
    id: Long,
    val name: String,
    val priority: Int = 0,
    val status: TodoStatus,
    val parentId: Long? = null,
) : IdentifiedDto(id)

class DetailedTodoDto(
    id: Long,
    val name: String,
    val description: String,
    val priority: Int = 0,
    val status: TodoStatus,
    var parents: List<LightTodoDto> = listOf(),
    var children: List<LightTodoDto>? = listOf()
) : IdentifiedDto(id)

class TodoHierarchyDto(
    val id: Long,
    parent: TodoHierarchyDto,
    children: List<LightTodoDto>?
)

class TodoHistoryDto(val todoId: Long, val records: List<String>)

class TodoAndParentBranchIdDto(
    var todo: LightTodoDto,
    var parentBranchId: Long? = null,
)

class TodosListDto(
    var todos: List<TodoAndParentBranchIdDto>,
    var parentBranchesMap: Map<Long, List<LightTodoDto>>,
)