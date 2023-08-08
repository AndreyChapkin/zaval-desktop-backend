package org.home.zaval.zavalbackend.util

import org.home.zaval.zavalbackend.dto.TodoBranchDto
import org.home.zaval.zavalbackend.dto.TodoDto
import org.home.zaval.zavalbackend.dto.TodoHierarchyDto
import org.home.zaval.zavalbackend.dto.TodoHistoryDto
import org.home.zaval.zavalbackend.entity.Todo
import org.home.zaval.zavalbackend.entity.TodoHistory
import org.home.zaval.zavalbackend.entity.TodoShallowView
import org.home.zaval.zavalbackend.entity.value.TodoStatus
import org.home.zaval.zavalbackend.service.TodoService
import java.util.*

const val TODO_HISTORY_DELIMITER = "<;>"

fun Todo.toShallowHierarchyDto() = TodoHierarchyDto(id = this.id!!, name = this.name, status = this.status)
fun TodoHierarchyDto.toDto() = TodoDto(
    id = this.id,
    name = this.name,
    status = this.status,
    parentId = this.parents.takeIf { it.isNotEmpty() }?.last()?.id
)

// TODO get rid of
fun TodoShallowView.toDto() = TodoDto(
    id = this.getId() ?: -100,
    name = this.getName(),
    status = this.getStatus(),
    parentId = this.getParentId()
)

fun TodoShallowView.toShallowHierarchyDto() = TodoHierarchyDto(
    id = this.getId() ?: -100,
    name = this.getName(),
    status = this.getStatus(),
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

fun extractBranches(todoHierarchyDtos: List<TodoHierarchyDto>): List<TodoBranchDto> {
    if (todoHierarchyDtos.isEmpty()) {
        return emptyList();
    }
    val DELIMITER_VALUE = TodoHierarchyDto(
        id = Long.MIN_VALUE,
        name = "",
        status = TodoStatus.BACKLOG,
    )
    val result: MutableList<TodoBranchDto> = mutableListOf()
    val parentDepth: LinkedList<TodoDto> = LinkedList()
    val visitingParentsQueue: Deque<TodoHierarchyDto> = LinkedList()
    for (hierarchyDto in todoHierarchyDtos) {
        if (hierarchyDto.children?.isNotEmpty() == true) {
            visitingParentsQueue.addLast(hierarchyDto)
        } else {
            val newTodoBranchDto = TodoBranchDto(
                parents = emptyList(),
                leaves = listOf(hierarchyDto.toDto())
            )
            result.add(newTodoBranchDto)
        }
    }
    while (visitingParentsQueue.isNotEmpty()) {
        // take and remove the first parent from the queue to visit it
        val parent = visitingParentsQueue.pollFirst()!!
        if (parent.id == DELIMITER_VALUE.id) {
            // finish considering current depth level, rise one step upper from the 'depth'
            parentDepth.removeLast()
            continue
        }
        // dive into the parent and track the 'depth'
        parentDepth.add(parent.toDto())
        val newParentsToVisit: MutableList<TodoHierarchyDto> = mutableListOf()
        val currentLeaves: MutableList<TodoDto> = mutableListOf()
        for (hierarchyDto in parent.children!!) {
            if (hierarchyDto.children?.isNotEmpty() == true) {
                // if child is parent too, schedule it for visiting before previous level parents
                newParentsToVisit.add(hierarchyDto)
            } else {
                // if child is not parent put to the children array for current depth
                currentLeaves.add(hierarchyDto.toDto())
            }
        }
        if (currentLeaves.isNotEmpty()) {
            // add result leaves with current 'depth'
            val newTodoBranchDto = TodoBranchDto(
                parents = parentDepth.toList(),
                leaves = currentLeaves
            )
            result.add(newTodoBranchDto)
        }
        if (newParentsToVisit.isNotEmpty()) {
            // schedule visiting new parents firstly and preserve the 'depth'
            visitingParentsQueue.addFirst(DELIMITER_VALUE)
            newParentsToVisit.reversed().forEach {
                visitingParentsQueue.addFirst(it)
            }
        } else {
            // no new parents, rise one step upper from the 'depth'
            parentDepth.removeLast()
        }
    }
    return result;
}

fun buildFullHierarchy(todoShallowViews: List<TodoShallowView>): List<TodoHierarchyDto> {
    val parentIdAndChildrenResultDtos = mutableMapOf<Long, MutableList<TodoHierarchyDto>>()
    todoShallowViews.forEach {
        val resultId = it.getId()!!
        val resultChildrenDtoList = parentIdAndChildrenResultDtos.computeIfAbsent(resultId) { mutableListOf() }
        val resultTodoDto = it.toShallowHierarchyDto().apply {
            children = resultChildrenDtoList
        }
        val resultParentId = (it.getParentId() ?: TodoService.TODO_ROOT.id)!!
        val resultParentChildrenDtoList =
            parentIdAndChildrenResultDtos.computeIfAbsent(resultParentId) { mutableListOf() }
        resultParentChildrenDtoList.add(resultTodoDto)
    }
    return parentIdAndChildrenResultDtos[TodoService.TODO_ROOT.id]!!
}