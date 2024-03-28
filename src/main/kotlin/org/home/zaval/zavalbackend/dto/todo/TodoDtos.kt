package org.home.zaval.zavalbackend.dto.todo

import org.home.zaval.zavalbackend.dto.IdentifiedDto
import org.home.zaval.zavalbackend.entity.Todo
import org.home.zaval.zavalbackend.entity.projection.TodoIdsProjection
import org.home.zaval.zavalbackend.entity.projection.TodoLightProjection
import org.home.zaval.zavalbackend.entity.value.TodoStatus
import org.home.zaval.zavalbackend.util.asStringFormattedWithISO8601withOffset
import org.home.zaval.zavalbackend.util.asUtc

class TodoDto(
    id: Long,
    var name: String,
    var description: String,
    var status: TodoStatus,
    var priority: Int = 0,
    var createdOn: String,
    var interactedOn: String,
    var parentId: Long?,
) : IdentifiedDto(id)

class TodoLightDto(
    id: Long,
    val name: String,
    val priority: Int = 0,
    val status: TodoStatus,
    val parentId: Long? = null,
    val interactedOn: String,
) : IdentifiedDto(id)

fun TodoLightProjection.toDto() = TodoLightDto(
    id = this.getId(),
    name = this.getName(),
    priority = this.getPriority(),
    status = this.getStatus(),
    parentId = this.getParentId(),
    interactedOn = this.getInteractedOn().asUtc.asStringFormattedWithISO8601withOffset()
)

fun Todo.toDto() = TodoLightDto(
    id = this.id!!,
    name = this.name,
    priority = this.priority,
    status = this.status,
    parentId = this.parentId,
    interactedOn = this.interactedOn.asStringFormattedWithISO8601withOffset()
)

class TodoIdsDto(
    val id: Long,
    val parentId: Long?,
)

fun TodoIdsProjection.toDto() = TodoIdsDto(
    id = this.getId(),
    parentId = this.getParentId()
)

class TodoFamilyDto(
    id: Long,
    val name: String,
    val description: String,
    val priority: Int = 0,
    val status: TodoStatus,
    val interactedOn: String,
    var parents: List<TodoLightDto> = listOf(),
    var children: List<TodoLightDto>? = listOf()
) : IdentifiedDto(id)

fun Todo.toFamilyDto(parents: List<TodoLightDto>, children: List<TodoLightDto>) = TodoFamilyDto(
    id = this.id!!,
    name = this.name,
    description = this.description,
    priority = this.priority,
    status = this.status,
    interactedOn = this.interactedOn.asStringFormattedWithISO8601withOffset(),
    parents = parents,
    children = children
)

class TodoLeafWithBranchIdDto(
    var leafTodo: TodoLightDto,
    var parentBranchId: Long? = null,
)

class TodoLeavesAndBranchesDto(
    var leafTodos: List<TodoLeafWithBranchIdDto>,
    var parentBranchesMap: Map<Long, List<TodoLightDto>>,
)

data class TodoMoveDto(
    val todoId: Long,
    val parentId: Long?,
)

data class TodoCreateDto(
    val name: String,
    val status: TodoStatus,
    val priority: Int,
    val parentId: Long? = null,
)

fun TodoCreateDto.toEntity() = Todo(
    id = null,
    name = this.name,
    status = this.status,
    priority = this.priority,
    parentId = this.parentId
)

class TodoUpdateDto(
    val general: TodoUpdateGeneralDto?,
    val description: String?
)

class TodoUpdateGeneralDto(
    val name: String,
    val status: TodoStatus,
    val priority: Int,
)