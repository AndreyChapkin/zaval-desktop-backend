package org.home.zaval.zavalbackend.service

import org.home.zaval.zavalbackend.dto.*
import org.home.zaval.zavalbackend.entity.*
import org.home.zaval.zavalbackend.entity.value.TodoStatus
import org.home.zaval.zavalbackend.repository.TodoParentPathRepository
import org.home.zaval.zavalbackend.repository.TodoHistoryRepository
import org.home.zaval.zavalbackend.repository.TodoRepository
import org.home.zaval.zavalbackend.util.*
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

    @Transactional
    fun createTodo(todoDto: CreateTodoDto): TodoDto {
        val newTodo = todoDto.toEntity()
        val newTodoParentPath = TodoParentPath(id = null, segments = mutableListOf(), isLeave = true)
        val parentTodoParentPath = todoDto.parentId?.let {
            todoParentPathRepository.findById(it).orElse(null)
        }
        val savedTodo = todoRepository.save(newTodo).toDto()
        newTodoParentPath.apply {
            id = savedTodo.id
            if (parentTodoParentPath != null) {
                parentTodoParentPath.isLeave = false
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
            updatingTodo.priority = todoDto.priority
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

    fun findAllShallowTodosByNameFragment(nameFragment: String): List<TodoDto> {
        return todoRepository.findAllShallowTodosByNameFragment("%$nameFragment%")
            .map { it.toDto() }
    }

    fun getPrioritizedListOfTodosWithStatus(status: TodoStatus): TodosListDto {
        val todoParentPaths = todoParentPathRepository.getAllTodoParentPathsWithTodoStatus(status)
        if (todoParentPaths.isEmpty()) {
            return TodosListDto(todos = emptyList(), parentBranchesMap = emptyMap())
        }
        val neededTodoIds = mutableSetOf<Long>()
        todoParentPaths.forEach { path ->
            neededTodoIds.add(path.id!!)
            path.segments.forEach { neededTodoIds.add(it.parentId) }
        }
        val todos = todoRepository.getAllShallowTodosByIds(neededTodoIds)
        val rootHierarchyDtos = buildFullHierarchy(todos)
        return extractPrioritizedTodosList(rootHierarchyDtos)
    }

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
                if (segments.isNotEmpty()) {
                    val oldSegmentsIds = segments.map { it.id!! }
                    // NOTE: orphanRemoval delete each item with separate query
                    todoParentPathRepository.removeAllSegmentsByIds(oldSegmentsIds)
                    segments.clear()
                }
                appendSegments(finalParentTodoParentPath.segments)
                appendIdToSegments(finalParentTodo.id!!)
            }
            finalParentTodoParentPath.isLeave = false
            // build new parent branch paths for all children
            val childrenTodoParentPaths = todoParentPathRepository.findAllLevelChildren(movingTodo.id!!)
            childrenTodoParentPaths.forEach { todoParentPath ->
                val parentWithChildrenIds = parentWithChildrenIdsInPath(todoParentPath, movingTodo.id!!)
                if (todoParentPath.segments.isNotEmpty()) {
                    val oldSegmentsIds = todoParentPath.segments.map { it.id!! }
                    // NOTE: orphanRemoval delete each item with separate query
                    todoParentPathRepository.removeAllSegmentsByIds(oldSegmentsIds)
                    todoParentPath.segments.clear()
                }
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

    private fun buildTodoHierarchy(todo: Todo?): TodoHierarchyDto? {
        if (todo == null) {
            return null
        }
        // initialize main node
        val result = todo.toShallowHierarchyDto()
        if (todo.id != TODO_ROOT.id) {
            // initialize parents chain
//            val parentsPathSegments = todoParentPathRepository.getParentPathSegments(todo.id!!)
            // must be found
            val parentsPath = todoParentPathRepository.findById(todo.id!!).get()
            val orderedParentIds = parentsPath.segments.map { it.parentId }
            val parentTodos = todoRepository.findAllById(orderedParentIds)
            val orderedParentsHierarchiesList = orderedParentIds
                .mapNotNull { id -> parentTodos.find { id == it.id } }
                .map { it.toShallowHierarchyDto() }
            result.parents = orderedParentsHierarchiesList
        }
        // initialize children
        result.children = (
            if (todo.id == TODO_ROOT.id) todoRepository.getAllTopTodos()
            else todoRepository.getAllChildrenOf(result.id)
            ).map {
                it.toShallowHierarchyDto()
            }
        return result
    }

    private fun buildTodoHierarchy(todo: Todo, orderedParentTodos: Iterable<Todo>): TodoHierarchyDto {
        val result = todo.toShallowHierarchyDto()
        val orderedParentsHierarchiesList = orderedParentTodos
            .map { it.toShallowHierarchyDto() }
        result.parents = orderedParentsHierarchiesList
        return result
    }
}