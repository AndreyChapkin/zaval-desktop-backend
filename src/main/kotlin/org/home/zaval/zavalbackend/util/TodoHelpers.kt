package org.home.zaval.zavalbackend.util

import org.home.zaval.zavalbackend.dto.*
import org.home.zaval.zavalbackend.entity.Todo
import org.home.zaval.zavalbackend.entity.TodoHistory
import org.home.zaval.zavalbackend.entity.TodoLightView
import org.home.zaval.zavalbackend.entity.value.TodoStatus
import java.util.*

const val TODO_HISTORY_DELIMITER = "<;>"

val TODO_ROOT: Todo = Todo(
    id = -1000,
    name = "Root",
    status = TodoStatus.BACKLOG,
)

// TODO get rid of
fun TodoLightView.toLightDto() = LightTodoDto(
    id = this.getId() ?: -100,
    name = this.getName(),
    status = this.getStatus(),
    priority = this.getPriority(),
    parentId = this.getParentId(),
)

fun Todo.toLightDto() = LightTodoDto(
    id = this.id!!,
    name = this.name,
    priority = this.priority,
    status = this.status,
    parentId = this.parent?.id
)

fun Todo.toDetailedDto(parents: List<LightTodoDto>, children: List<LightTodoDto>) = DetailedTodoDto(
    id = this.id!!,
    name = this.name,
    description = this.description,
    priority = this.priority,
    status = this.status,
    parents = parents,
    children = children
)

fun CreateTodoDto.toEntity() = Todo(
    id = null,
    name = this.name,
    status = this.status,
    parent = this.parentId?.takeIf { it != TODO_ROOT.id }?.let {
        Todo(id = this.parentId, name = "", status = TodoStatus.BACKLOG)
    }
)

fun LightTodoDto.toEntity() = Todo(
    id = null,
    name = this.name,
    status = this.status,
    parent = this.parentId?.takeIf { it != TODO_ROOT.id }?.let {
        Todo(id = this.parentId, name = "", status = TodoStatus.BACKLOG)
    }
)

fun TodoHistory.toLightDto() = TodoHistoryDto(
    todoId = this.id!!,
    records = extractTodoRecords(this.records)
)

fun extractTodoRecords(records: String) = records.split(TODO_HISTORY_DELIMITER)

fun mergeHistoryRecordsToPersist(records: List<String>) = records.joinToString(TODO_HISTORY_DELIMITER)

fun extractPrioritizedTodosList(todoLightViews: List<TodoLightView>): TodosListDto {
    val allIdsAndTodos = mutableMapOf<Long, TodoLightView>()
    val leaveIdsAndTodos = mutableMapOf<Long, TodoLightView>()
    val parentIdsSet = mutableSetOf<Long>()
    todoLightViews.forEach {
        val parentId = it.getParentId()
        allIdsAndTodos[it.getId()!!] = it
        if (parentId != null) {
            parentIdsSet.add(parentId)
        }
        if (!parentIdsSet.contains(it.getId()!!)) {
            leaveIdsAndTodos[it.getId()!!] = it
        }
        leaveIdsAndTodos.remove(parentId)
    }
    var idSequence = 0L
    val resultParentBranchesMap: MutableMap<Long, List<LightTodoDto>> = mutableMapOf()
    val resultTodosList: MutableList<TodoAndParentBranchIdDto> = mutableListOf()
    val startParentIdAndParentsListIdMap: MutableMap<Long, Long> = mutableMapOf()
    leaveIdsAndTodos.values.forEach { value ->
        var parentsListId = startParentIdAndParentsListIdMap[value.getParentId()]
        if (parentsListId == null) {
            val parentsList = mutableListOf<LightTodoDto>()
            var curParent = allIdsAndTodos[value.getParentId()]
            while (curParent != null) {
                parentsList.add(curParent.toLightDto())
                curParent = allIdsAndTodos[curParent.getParentId()]
            }
            if (parentsList.isNotEmpty()) {
                parentsListId = idSequence++
                resultParentBranchesMap[parentsListId] = parentsList.reversed()
                startParentIdAndParentsListIdMap[value.getParentId()!!] = parentsListId
            }
        }
        resultTodosList.add(
            TodoAndParentBranchIdDto(
                todo = value.toLightDto(),
                parentBranchId = parentsListId
            )
        )
    }
    val sortedResultTodosList = resultTodosList.toMutableList().apply {
        sortByDescending {
            it.todo.priority
        }
    }
    return TodosListDto(todos = sortedResultTodosList, parentBranchesMap = resultParentBranchesMap)
}