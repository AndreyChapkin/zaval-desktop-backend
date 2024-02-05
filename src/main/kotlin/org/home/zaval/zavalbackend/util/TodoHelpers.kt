package org.home.zaval.zavalbackend.util

import org.home.zaval.zavalbackend.dto.todo.*
import org.home.zaval.zavalbackend.entity.projection.TodoLightProjection

fun extractPrioritizedTodosList(todoLights: List<TodoLightProjection>): TodoLeavesAndBranchesDto {
    val leafIdsSet = mutableSetOf<Long>()
    val parentIdsSet = mutableSetOf<Long>()
    val allIdsAndTodos = todoLights.associateBy {
        // track the parent and exclude from leaves
        it.getParentId()?.let { parentId ->
            parentIdsSet.add(parentId)
            leafIdsSet.remove(parentId)
        }
        // if it was not previously noticed as parent then it is a leaf by default
        if (!parentIdsSet.contains(it.getId())) {
            leafIdsSet.add(it.getId())
        }
        // the association key
        it.getId()
    }
    var branchIdSequence = 0L
    val resultParentBranchesMap: MutableMap<Long, List<TodoLightDto>> = mutableMapOf()
    val resultTodoLeaves: MutableList<TodoLeafWithBranchIdDto> = mutableListOf()
    val startIdToBranchIdMap: MutableMap<Long, Long> = mutableMapOf()
    leafIdsSet.forEach { leafId ->
        val leafTodo = allIdsAndTodos[leafId]!!
        val startParentId = leafTodo.getParentId()
        var parentBranchId: Long? = null
        if (startParentId != null) {
            parentBranchId = startIdToBranchIdMap[startParentId]
            if (parentBranchId == null) {
                // Construct branch
                val parentsBranch = mutableListOf<TodoLightDto>()
                var curParent = allIdsAndTodos[startParentId]
                while (curParent != null) {
                    parentsBranch.add(curParent.toDto())
                    curParent = allIdsAndTodos[curParent.getParentId()]
                }
                if (parentsBranch.isNotEmpty()) {
                    parentBranchId = branchIdSequence++
                    resultParentBranchesMap[parentBranchId] = parentsBranch.reversed()
                    startIdToBranchIdMap[startParentId] = parentBranchId
                }
            }
        }
        resultTodoLeaves.add(
            TodoLeafWithBranchIdDto(
                leafTodo = leafTodo.toDto(),
                parentBranchId = parentBranchId
            )
        )
    }
    val sortedResultTodosList = resultTodoLeaves.toMutableList().apply {
        sortByDescending {
            it.leafTodo.priority
        }
    }
    return TodoLeavesAndBranchesDto(leafTodos = sortedResultTodosList, parentBranchesMap = resultParentBranchesMap)
}