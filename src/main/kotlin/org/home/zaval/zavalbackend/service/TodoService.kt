package org.home.zaval.zavalbackend.service

import org.home.zaval.zavalbackend.dto.*
import org.home.zaval.zavalbackend.model.Todo
import org.home.zaval.zavalbackend.model.TodoHistory
import org.home.zaval.zavalbackend.model.TodoParentPath
import org.home.zaval.zavalbackend.model.TodoParentPathSegment
import org.home.zaval.zavalbackend.model.value.TodoStatus
import org.home.zaval.zavalbackend.repository.TodoParentPathRepository
import org.home.zaval.zavalbackend.repository.TodoHistoryRepository
import org.home.zaval.zavalbackend.repository.TodoRepository
import org.home.zaval.zavalbackend.util.mergeHistoryRecordsToPersist
import org.home.zaval.zavalbackend.util.toDto
import org.home.zaval.zavalbackend.util.toShallowHierarchyDto
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class TodoService(
    val todoRepository: TodoRepository,
    val todoParentPathRepository: TodoParentPathRepository,
    val todoHistoryRepository: TodoHistoryRepository,
) {

    companion object {
        val TODO_ROOT: Todo = Todo(
            id = -1000,
            name = "Root",
            status = TodoStatus.BACKLOG,
        )

        val PARENT_PATH_IDS_SEPARATOR = "/"
    }

    @Transactional
    fun createTodo(todoDto: CreateTodoDto): TodoDto {
        val newTodo = Todo(
            id = null,
            name = todoDto.name,
            status = todoDto.status,
            parent = todoDto.parentId?.takeIf { it != TODO_ROOT.id }?.let {
                Todo(id = todoDto.parentId, name = "", status = TodoStatus.BACKLOG)
            }
        )
        val newTodoParentPath = TodoParentPath(id = null, segments = mutableListOf(), isLeave = true)
        val parentTodoParentPath = todoDto.parentId?.let {
            todoParentPathRepository.findById(it).orElse(null)
        }
        val savedTodo = todoRepository.save(newTodo).toDto()
        newTodoParentPath.apply {
            id = savedTodo.id
            if (parentTodoParentPath != null) {
                appendSegments(parentTodoParentPath.segments)
                appendIdToSegments(parentTodoParentPath.id!!)
            }
        }
        val savingPaths = mutableListOf(newTodoParentPath).apply {
            if (parentTodoParentPath != null) {
                add(parentTodoParentPath)
            }
        }
        todoParentPathRepository.saveAll(savingPaths)
        return savedTodo
    }

    fun getTodo(todoId: Long?): TodoDto? {
        return loadTodo(todoId)?.toDto()
    }

    // TODO downgrade status of parent task to all child tasks statuses
    fun updateTodo(todoId: Long, todoDto: UpdateTodoDto): TodoDto? {
        val updatingTodo = loadTodo(todoId)
        if (updatingTodo != null) {
            updatingTodo.name = todoDto.name
            updatingTodo.status = todoDto.status
            // also upgrade statuses of the parents
            // TODO optimize
            val updatedParentTodos = upgradeAllParentStatuses(updatingTodo)
            todoRepository.saveAll(mutableListOf(updatingTodo).apply {
                addAll(updatedParentTodos)
            })
            return updatingTodo.toDto()
        }
        return null
    }

    // TODO optimize. Standard delete methods generate too many queries
    @Transactional
    fun deleteTodo(todoId: Long) {
        val allLevelChildrenIds = todoParentPathRepository.findAllLevelChildrenIds(todoId)
        val resultDeleteIds = mutableListOf<Long>()
            .apply { add(todoId) }
            .apply { addAll(allLevelChildrenIds) }
        todoRepository.deleteAllById(resultDeleteIds)
        todoParentPathRepository.deleteAllById(resultDeleteIds)
        todoHistoryRepository.deleteAllForIds(resultDeleteIds)
//        val resultDeleteIds = mutableListOf<Long>()
//        // delete children recursively
//        val searchingQueue = LinkedList<Long>().apply {
//            add(todoId)
//        }
//        while (searchingQueue.isNotEmpty()) {
//            val curParentId = searchingQueue.removeFirst()
//            resultDeleteIds.add(curParentId)
//            // plan children deletion
//            val curChildrenIds = todoRepository.getAllChildrenOf(curParentId).map { it.id as Long }
//            searchingQueue.addAll(curChildrenIds)
//        }
//        todoRepository.deleteAllById(resultDeleteIds)
//        todoParentPathRepository.deleteAllById(resultDeleteIds)
//        todoHistoryRepository.deleteAllForIds(resultDeleteIds)
    }

    /**
     * @return root <- ... parent <- todoElement -> children[]
     */
    fun getTodoHierarchy(todoId: Long?): TodoHierarchyDto? {
        val todo: Todo? = if (todoId == null) TODO_ROOT else todoRepository.findById(todoId).orElse(null)
        return buildTodoHierarchy(todo)
    }

    fun getAllTodos(status: TodoStatus?): List<TodoDto> {
        return status
            ?.let { todoRepository.getAllTodosWithStatus(it.name.uppercase()).map { it.toDto() } }
            ?: todoRepository.getAllTodos().map { it.toDto() }
    }

    fun getAllUpBranches(status: TodoStatus?): List<TodoHierarchyDto> {
        val todoParentPathsWithStatus = status
            ?.let {
                todoParentPathRepository.getAllTodoParentPathsWithTodoStatus(it)
            }
            ?: todoParentPathRepository.findAll()
        val withStatusIdsSet = todoParentPathsWithStatus.map { it.id }.toSet()
        val childIdWithStatusAndOrderedParentsIds = mutableMapOf<Long, List<Long>>()
        val parentIdAndChildrenIds = mutableMapOf<Long, MutableList<Long>>()
        val allParentIdsSet = todoParentPathsWithStatus.flatMap {
            // also construct parent -> children map
            val orderedParentIds = it.segments.map { segment -> segment.parentId!! }
            for (parentId in orderedParentIds) {
                parentIdAndChildrenIds
                    .getOrPut(parentId) { mutableListOf() }
                    .apply { add(it.id!!) }
            }
            // also construct child -> parents map
            childIdWithStatusAndOrderedParentsIds[it.id!!] = orderedParentIds
            orderedParentIds
        }.toSet()
        // remove doubles from 'leave' ids set that are already in parent ids set
        // think about situation when there is parent with status, but all its children with another status
        parentIdAndChildrenIds.keys.forEach {
            childIdWithStatusAndOrderedParentsIds.remove(it)
        }
        val allIdsSet = withStatusIdsSet.toMutableSet().apply {
            addAll(allParentIdsSet)
        }
        val allTodos = todoRepository.findAllById(allIdsSet)
        val childrenWithStatusTodos = mutableListOf<Todo>()
        val childWithStatusIdAndParentsTodos = mutableMapOf<Long, MutableList<Todo>>()
        for (todo in allTodos) {
            if (childIdWithStatusAndOrderedParentsIds.containsKey(todo.id)) {
                // save leave todoHierarchy
                childrenWithStatusTodos.add(todo)
            } else if (parentIdAndChildrenIds.containsKey(todo.id)) {
                // pre-save parent for all its children
                parentIdAndChildrenIds[todo.id]?.let { childrenIds ->
                    for (childId in childrenIds) {
                        childWithStatusIdAndParentsTodos
                            .getOrPut(childId) { mutableListOf() }
                            .apply { add(todo) }
                    }
                }
            }
        }
        // construct hierarchies
        val result = childrenWithStatusTodos.map { childWithStatusTodo ->
            val orderedParentTodos = childWithStatusIdAndParentsTodos[childWithStatusTodo.id]
                ?.let { parentTodos ->
                    val orderedParentIds = childIdWithStatusAndOrderedParentsIds[childWithStatusTodo.id] ?: emptyList()
                    orderedParentIds.mapNotNull { id -> parentTodos.find { it.id == id } }
                } ?: emptyList()
            buildTodoHierarchy(childWithStatusTodo, orderedParentTodos)
        }
        return result
    }

//    fun getAllUpBranches(status: TodoStatus?): List<TodoHierarchyDto> {
//        val leavePaths = status
//            ?.let {
//                todoBranchRepository.getAllLeavesTodoParentPathsWithTodoStatus(it)
//            }
//            ?: todoBranchRepository.getAllLeavesTodoParentPaths()
//        val leavesIdsSet = leavePaths.map { it.id }.toSet()
//        val leaveBranchAndOrderedParentsIds = mutableMapOf<Long, List<Long>>()
//        val parentAndChildrenIds = mutableMapOf<Long, MutableList<Long>>()
//        val parentIdsSet = leavePaths.flatMap {
//            // also construct parent -> children map
//            val parentIds = extractIdsFromParentPath(it.parentPath)
//            for (parentId in parentIds) {
//                parentAndChildrenIds
//                    .getOrPut(parentId) { mutableListOf() }
//                    .apply { add(it.id!!) }
//            }
//            // also construct child -> parents map
//            leaveBranchAndOrderedParentsIds[it.id!!] = parentIds
//            parentIds
//        }.toSet()
//        val allIdsSet = leavesIdsSet.toMutableSet().apply {
//            addAll(parentIdsSet)
//        }
//        val allTodos = todoRepository.findAllById(allIdsSet)
//        val leaveTodos = mutableListOf<Todo>()
//        val leaveIdAndParentsTodos = mutableMapOf<Long, MutableList<Todo>>()
//        for (todo in allTodos) {
//            if (leaveBranchAndOrderedParentsIds.containsKey(todo.id)) {
//                // save leave todoHierarchy
//                leaveTodos.add(todo)
//            } else if (parentAndChildrenIds.containsKey(todo.id)) {
//                // pre-save parent for all its children
//                parentAndChildrenIds[todo.id]?.let { childrenIds ->
//                    for (childId in childrenIds) {
//                        leaveIdAndParentsTodos
//                            .getOrPut(childId) { mutableListOf() }
//                            .apply { add(todo) }
//                    }
//                }
//            }
//        }
//        // construct hierarchies
//        val result = leaveTodos.map { leaveTodo ->
//            val orderedParentTodos = leaveIdAndParentsTodos[leaveTodo.id]?.let { parentTodos ->
//                val orderedParentIds = leaveBranchAndOrderedParentsIds[leaveTodo.id] ?: emptyList()
//                orderedParentIds.mapNotNull { id ->
//                    parentTodos.find { it.id == id }
//                }
//            } ?: emptyList()
//            buildTodoHierarchy(leaveTodo, orderedParentTodos)
//        }
//        return result
//    }

    @Transactional
    fun moveTodo(moveTodoDto: MoveTodoDto) {
        val movingTodo = loadTodo(moveTodoDto.todoId)
        val finalParentTodo = loadTodo(moveTodoDto.parentId)
        if (movingTodo != null && finalParentTodo != null) {
            movingTodo.parent = finalParentTodo
            todoRepository.save(movingTodo)
            // update todoBranches
            val movingTodoParentPath = todoParentPathRepository.findById(movingTodo.id!!).get()
            val finalParentTodoParentPath = todoParentPathRepository.findById(finalParentTodo.id!!).get()
            // build new parent branch path for moving task
            movingTodoParentPath.apply {
                val oldSegmentsIds = segments.map { it.id!! }
                // NOTE: orphanRemoval delete each item with separate query
                todoParentPathRepository.removeAllSegmentsByIds(oldSegmentsIds)
                segments.clear()
                appendSegments(finalParentTodoParentPath.segments)
                appendIdToSegments(finalParentTodo.id!!)
            }
            finalParentTodoParentPath.isLeave = false
            // build new parent branch paths for all children
            val childrenTodoParentPaths = todoParentPathRepository.findAllLevelChildren(movingTodo.id!!)
            childrenTodoParentPaths.forEach { todoParentPath ->
                val parentWithChildrenIds = parentWithChildrenIdsInPath(todoParentPath, movingTodo.id!!)
                val oldSegmentsIds = todoParentPath.segments.map { it.id!! }
                // NOTE: orphanRemoval delete each item with separate query
                todoParentPathRepository.removeAllSegmentsByIds(oldSegmentsIds)
                todoParentPath.segments.clear()
                todoParentPath.appendSegments(movingTodoParentPath.segments)
                todoParentPath.appendIdsToParentPath(parentWithChildrenIds)
            }
            todoParentPathRepository.saveAll(
                mutableListOf(movingTodoParentPath)
                    .apply { add(finalParentTodoParentPath) }
                    .apply { addAll(childrenTodoParentPaths) }
            )
        }
    }

    fun getTodoHistory(todoId: Long?): TodoHistoryDto? {
        return if (todoId != null) {
            todoHistoryRepository.findById(todoId).map { it.toDto() }.orElse(null)
        } else null
    }

    fun updateTodoHistory(todoId: Long, todoHistoryDto: TodoHistoryDto): TodoHistoryDto? {
        // try to update
        val updatingTodoHistory = todoHistoryRepository.findById(todoId).orElse(null)
        if (todoHistoryDto.records.isNotEmpty()) {
            if (updatingTodoHistory != null) {
                updatingTodoHistory.records = mergeHistoryRecordsToPersist(todoHistoryDto.records)
                todoHistoryRepository.save(updatingTodoHistory)
                return updatingTodoHistory.toDto()
            } else {
                // try to create
                val owningTodoExists = todoRepository.existsById(todoId)
                if (owningTodoExists) {
                    val newTodoHistory = TodoHistory(
                        id = todoId,
                        records = mergeHistoryRecordsToPersist(todoHistoryDto.records),
                    )
                    todoHistoryRepository.save(newTodoHistory)
                    return newTodoHistory.toDto()
                }
            }
        } else {
            // try to delete
            if (updatingTodoHistory != null) {
                todoHistoryRepository.deleteById(todoId)
            }
        }
        return null
    }

    private fun loadTodo(todoId: Long?): Todo? = todoId?.let { todoRepository.findById(it).orElse(null) }

    private fun upgradeAllParentStatuses(todo: Todo): List<Todo> {
        var curParent = todo.parent
        val updatedParents = mutableListOf<Todo>()
        while (curParent != null) {
            if (curParent.status.priority < todo.status.priority) {
                curParent.status = todo.status
                updatedParents.add(curParent)
            }
            curParent = curParent.parent
        }
        return updatedParents
    }

//    private fun extractIdsFromParentPath(parentPath: String?): List<Long> = parentPath
//        ?.takeIf { it.isNotEmpty() }
//        ?.let { notEmptyPath ->
//            notEmptyPath.split(PARENT_PATH_IDS_SEPARATOR).filter { it.isNotEmpty() }.map { it.toLong() }
//        }
//        ?: emptyList()

//    private fun appendIdsToParentPath(parentPath: String?, ids: List<Long>): String =
//        if (ids.isEmpty()) (parentPath ?: "")
//        else (
//            parentPath
//                ?.takeIf { it.isNotEmpty() }
//                ?.let { it + ids.joinToString(separator = PARENT_PATH_IDS_SEPARATOR) }
//                ?: (PARENT_PATH_IDS_SEPARATOR + ids.joinToString(separator = PARENT_PATH_IDS_SEPARATOR))
//            ) + PARENT_PATH_IDS_SEPARATOR

//    private fun appendIdToParentPath(parentPath: String?, id: Long): String = (
//        parentPath
//            ?.takeIf { it.isNotEmpty() }
//            ?.let { it + id }
//            ?: (PARENT_PATH_IDS_SEPARATOR + id.toString())
//        ) + PARENT_PATH_IDS_SEPARATOR

    private fun TodoParentPath.appendSegments(segments: List<TodoParentPathSegment>) = segments.forEach {
        this.segments.add(
            TodoParentPathSegment(
                parentPath = this,
                parentId = it.parentId,
                orderIndex = this.segments.size,
            )
        )
    }

    private fun TodoParentPath.appendIdsToParentPath(ids: List<Long>) = ids.forEach {
        this.segments.add(
            TodoParentPathSegment(
                parentPath = this,
                parentId = it,
                orderIndex = segments.size,
            )
        )
    }

    private fun TodoParentPath.appendIdToSegments(id: Long) = this.segments.add(
        TodoParentPathSegment(
            parentPath = this,
            parentId = id,
            orderIndex = segments.size,
        )
    )

//    private fun parentAndChildrenIdsInPath(parentPath: String, parentId: Long): List<Long> {
//        val allIds = extractIdsFromParentPath(parentPath)
//        val parentIndex = allIds.indexOf(parentId)
//        if (parentIndex in 0..allIds.lastIndex) {
//            return allIds.subList(parentIndex, allIds.size)
//        }
//        return emptyList()
//    }

    private fun parentWithChildrenIdsInPath(parentPath: TodoParentPath, parentId: Long): List<Long> {
        val allIds = parentPath.segments.map { it.parentId }
        val parentIndex = allIds.indexOf(parentId)
        if (parentIndex in 0..allIds.lastIndex) {
            return allIds.subList(parentIndex, allIds.size)
        }
        return emptyList()
    }

    private fun toParentPathPattern(id: Long) = "%${PARENT_PATH_IDS_SEPARATOR}$id${PARENT_PATH_IDS_SEPARATOR}%"

    private fun buildTodoHierarchy(todo: Todo?): TodoHierarchyDto? {
        if (todo == null) {
            return null
        }
        // initialize main node
        val result = todo.toShallowHierarchyDto()
        if (todo.id != TODO_ROOT.id) {
            // initialize parents chain
            val parentsPathSegments = todoParentPathRepository.getParentPathSegments(todo.id!!)
//            val parentTodos = todoRepository.findAllById(extractIdsFromParentPath(parentsPath))
            val parentTodos = todoRepository.findAllById(parentsPathSegments.map { it.parentId })
            var curHierarchy = result
            parentTodos.reversed().forEach {
                curHierarchy.parent = it.toShallowHierarchyDto()
                curHierarchy = curHierarchy.parent!!
            }
        }
        // initialize children
        result.children = (
            if (todo.id == TODO_ROOT.id) todoRepository.getAllTopTodos()
            else todoRepository.getAllChildrenOf(result.id)
            ).map {
                it.toShallowHierarchyDto()
            }.toTypedArray()
        return result
    }

    private fun buildTodoHierarchy(todo: Todo, orderedParentTodos: Iterable<Todo>): TodoHierarchyDto {
        val result = todo.toShallowHierarchyDto()
        var curChild = result
        for (parentTodo in orderedParentTodos.reversed()) {
            curChild.parent = parentTodo.toShallowHierarchyDto()
            curChild = curChild.parent!!
        }
        return result
    }
}