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

fun Todo.toShallowHierarchyDto() =
    TodoHierarchyDto(id = this.id!!, name = this.name, priority = this.priority, status = this.status)

fun TodoHierarchyDto.toDto() = TodoDto(
    id = this.id,
    name = this.name,
    priority = this.priority,
    status = this.status,
    parentId = this.parents.takeIf { it.isNotEmpty() }?.last()?.id
)

// TODO get rid of
fun TodoLightView.toDto() = TodoDto(
    id = this.getId() ?: -100,
    name = this.getName(),
    status = this.getStatus(),
    priority = this.getPriority(),
    parentId = this.getParentId(),
)

fun TodoLightView.toShallowHierarchyDto() = TodoHierarchyDto(
    id = this.getId() ?: -100,
    name = this.getName(),
    status = this.getStatus(),
    priority = this.getPriority(),
)

fun Todo.toDto() = TodoDto(
    id = this.id!!,
    name = this.name,
    priority = this.priority,
    status = this.status,
    parentId = this.parent?.id
)

fun CreateTodoDto.toEntity() = Todo(
    id = null,
    name = this.name,
    status = this.status,
    parent = this.parentId?.takeIf { it != TODO_ROOT.id }?.let {
        Todo(id = this.parentId, name = "", status = TodoStatus.BACKLOG)
    }
)

fun TodoDto.toEntity() = Todo(
    id = null,
    name = this.name,
    status = this.status,
    parent = this.parentId?.takeIf { it != TODO_ROOT.id }?.let {
        Todo(id = this.parentId, name = "", status = TodoStatus.BACKLOG)
    }
)

fun TodoHistory.toDto() = TodoHistoryDto(
    todoId = this.id!!,
    records = extractTodoRecords(this.records)
)

fun extractTodoRecords(records: String) = records.split(TODO_HISTORY_DELIMITER)

fun mergeHistoryRecordsToPersist(records: List<String>) = records.joinToString(TODO_HISTORY_DELIMITER)

fun extractPrioritizedTodosList(todoHierarchyDtos: List<TodoHierarchyDto>): TodosListDto {
    if (todoHierarchyDtos.isEmpty()) {
        return TodosListDto(todos = emptyList(), parentBranchesMap = emptyMap());
    }
    val DELIMITER_VALUE = TodoHierarchyDto(
        id = Long.MIN_VALUE,
        name = "",
        status = TodoStatus.BACKLOG,
    )
    var idSequence = 0L
    val getId: () -> Long = {
        idSequence++
    }
    val resultParentBranchesMap: MutableMap<Long, List<TodoDto>> = mutableMapOf()
    val resultTodosList: MutableList<TodoAndParentBranchIdDto> = mutableListOf()
    val parentDepth: LinkedList<TodoDto> = LinkedList()
    val parentsToVisitQueue: Deque<TodoHierarchyDto> = LinkedList()
    for (hierarchyDto in todoHierarchyDtos) {
        if (hierarchyDto.children?.isNotEmpty() == true) {
            parentsToVisitQueue.addLast(hierarchyDto)
        } else {
            resultTodosList.add(
                TodoAndParentBranchIdDto(
                    todo = hierarchyDto.toDto()
                )
            )
        }
    }
    while (parentsToVisitQueue.isNotEmpty()) {
        // take and remove the first parent from the queue to visit it
        val curParent = parentsToVisitQueue.pollFirst()!!
        if (curParent.id == DELIMITER_VALUE.id) {
            // finish considering current depth level, rise one step upper from the 'depth'
            parentDepth.removeLast()
            continue
        }
        // dive into the parent and track the 'depth'
        parentDepth.add(curParent.toDto())
        val newParentsToVisit: MutableList<TodoHierarchyDto> = mutableListOf()
        val currentLeaves: MutableList<TodoDto> = mutableListOf()
        for (currentChild in curParent.children!!) {
            if (currentChild.children?.isNotEmpty() == true) {
                // if child is parent too, schedule it for visiting before previous level parents
                newParentsToVisit.add(currentChild)
            } else {
                // if child is not parent put to the children array for current depth
                currentLeaves.add(currentChild.toDto())
            }
        }
        if (currentLeaves.isNotEmpty()) {
            val currentParentBranchId = getId()
            currentLeaves.forEach {
                resultTodosList.add(
                    TodoAndParentBranchIdDto(
                        todo = it,
                        parentBranchId = currentParentBranchId
                    )
                )
            }
            resultParentBranchesMap[currentParentBranchId] = parentDepth.toList()
        }
        if (newParentsToVisit.isNotEmpty()) {
            // schedule visiting new parents firstly and preserve the 'depth'
            parentsToVisitQueue.addFirst(DELIMITER_VALUE)
            newParentsToVisit.forEach {
                parentsToVisitQueue.addFirst(it)
            }
        } else {
            // no new parents, rise one step upper from the 'depth'
            parentDepth.removeLast()
        }
    }
    // sort by priorities
    val resultPrioritizedTodosList = resultTodosList.apply {
        sortByDescending { it.todo.priority }
    }
    return TodosListDto(
        todos = resultPrioritizedTodosList,
        parentBranchesMap = resultParentBranchesMap
    )
}

fun buildFullHierarchy(todoLightViews: List<TodoLightView>): List<TodoHierarchyDto> {
    val parentIdAndChildrenResultDtos = mutableMapOf<Long, MutableList<TodoHierarchyDto>>()
    todoLightViews.forEach {
        val resultId = it.getId()!!
        val resultChildrenDtoList = parentIdAndChildrenResultDtos.computeIfAbsent(resultId) { mutableListOf() }
        val resultTodoDto = it.toShallowHierarchyDto().apply {
            children = resultChildrenDtoList
        }
        val resultParentId = (it.getParentId() ?: TODO_ROOT.id)!!
        val resultParentChildrenDtoList =
            parentIdAndChildrenResultDtos.computeIfAbsent(resultParentId) { mutableListOf() }
        resultParentChildrenDtoList.add(resultTodoDto)
    }
    return parentIdAndChildrenResultDtos[TODO_ROOT.id]!!
}