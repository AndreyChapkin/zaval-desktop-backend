package org.home.zaval.zavalbackend.dto.todo

import org.home.zaval.zavalbackend.dto.IdentifiedDto
import org.home.zaval.zavalbackend.entity.Todo
import org.home.zaval.zavalbackend.entity.value.TodoStatus
import org.home.zaval.zavalbackend.util.asUtc
import java.time.OffsetDateTime
import javax.persistence.*

class TodoDto(
    id: Long = -10000,
    var name: String,
    var description: String,
    var status: TodoStatus,
    var priority: Int = 0,
    var createdOn: OffsetDateTime = OffsetDateTime.now().asUtc,
    var interactedOn: OffsetDateTime = OffsetDateTime.now().asUtc,
    var parentId: Long?,
): IdentifiedDto(id)

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
    val interactedOn: String,
) : IdentifiedDto(id)

class FullTodoDto(
    id: Long,
    val name: String,
    val description: String,
    val priority: Int = 0,
    val status: TodoStatus,
    val createdOn: String,
    val interactedOn: String,
    val parentId: Long? = null,
) : IdentifiedDto(id)

class DetailedTodoDto(
    id: Long,
    val name: String,
    val description: String,
    val priority: Int = 0,
    val status: TodoStatus,
    val interactedOn: String,
    var parents: List<LightTodoDto> = listOf(),
    var children: List<LightTodoDto>? = listOf()
) : IdentifiedDto(id)

class HeavyDetailsDto(
    val todoId: Long,
    val description: String,
    val history: TodoHistoryDto?,
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