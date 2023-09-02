package org.home.zaval.zavalbackend.service

import org.home.zaval.zavalbackend.dto.*
import org.home.zaval.zavalbackend.entity.*
import org.home.zaval.zavalbackend.entity.value.TodoStatus
import org.home.zaval.zavalbackend.repository.TodoParentPathRepository
import org.home.zaval.zavalbackend.repository.TodoHistoryRepository
import org.home.zaval.zavalbackend.repository.TodoRepository
import org.home.zaval.zavalbackend.util.*
import org.home.zaval.zavalbackend.store.TodoStore
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
    fun createTodo(todoDto: CreateTodoDto): LightTodoDto {
        val newTodo = todoDto.toEntity().apply {
            id = TodoStore.getId()
        }
        val newTodoParentPath = TodoParentPath(id = null, segments = mutableListOf(), isLeave = true)
        val parentTodoParentPath = todoDto.parentId?.let {
            todoParentPathRepository.findById(it).orElse(null)
        }
        val savedTodo = todoRepository.save(newTodo).toLightDto()
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

    fun getLightTodo(todoId: Long?): LightTodoDto? {
        return loadTodo(todoId)?.toLightDto()
    }

    // TODO downgrade status of parent task to all child tasks statuses
    fun updateTodo(todoId: Long, updateTodoDto: UpdateTodoDto): LightTodoDto? {
        val updatingTodo = loadTodo(todoId)
        if (updatingTodo != null) {
            // update general information
            if (updateTodoDto.general != null) {
                updatingTodo.name = updateTodoDto.general.name
                updatingTodo.status = updateTodoDto.general.status
                updatingTodo.priority = updateTodoDto.general.priority
            }
            if (updateTodoDto.description != null) {
                updatingTodo.description = updateTodoDto.description
            }
            // also upgrade statuses of the parents
            // TODO optimize
            val updatedParentTodos = upgradeAllParentStatuses(updatingTodo)
            todoRepository.saveAll(mutableListOf(updatingTodo).apply {
                addAll(updatedParentTodos)
            })
            return updatingTodo.toLightDto()
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
    fun getDetailedTodo(todoId: Long?): DetailedTodoDto? {
        val todo: Todo = todoId?.let {
            todoRepository.findById(todoId).orElse(null)
        } ?: TODO_ROOT
        if (todo.id == TODO_ROOT.id) {
            val childrenTodos = todoRepository.getAllTopTodos()
            return todo.toDetailedDto(
                parents = emptyList(),
                childrenTodos.map { it.toLightDto() }
            )
        }
        val parentTodos = todoRepository.getAllParentsOf(todo.id!!)
        val orderedSegments = todoParentPathRepository.getOrderedParentPathSegments(todo.id!!)
        val orderedParentTodos = orderedSegments.map { segment ->
            parentTodos.find { it.id == segment.parentId }!!
        }
        val childrenTodos = todoRepository.getAllChildrenOf(todo.id!!)
        return todo.toDetailedDto(
            orderedParentTodos.map { it.toLightDto() },
            childrenTodos.map { it.toLightDto() }
        )
    }

    fun getAllTodos(status: TodoStatus?): List<LightTodoDto> {
        return status
            ?.let { todoRepository.getAllTodosWithStatus(it.name.uppercase()).map { it.toLightDto() } }
            ?: todoRepository.getAllTodos().map { it.toLightDto() }
    }

    fun findAllShallowTodosByNameFragment(nameFragment: String): List<LightTodoDto> {
        return todoRepository.findAllShallowTodosByNameFragment("%$nameFragment%")
            .map { it.toLightDto() }
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
        return extractPrioritizedTodosList(todos)
    }

    @Transactional
    fun moveTodo(moveTodoDto: MoveTodoDto) {
        if (moveTodoDto.todoId == moveTodoDto.parentId) {
            return
        }
        val movingTodo = loadTodo(moveTodoDto.todoId)
        val finalParentTodo = if (moveTodoDto.parentId != null) loadTodo(moveTodoDto.parentId) else TODO_ROOT
        if (movingTodo != null && finalParentTodo != null) {
            if (finalParentTodo.id == TODO_ROOT.id) {
                movingTodo.parent = null
                todoRepository.save(movingTodo)
                // update todoBranches
                val movingTodoParentPath = todoParentPathRepository.findById(movingTodo.id!!).get()
                // build new parent branch path for moving task
                movingTodoParentPath.apply {
                    if (segments.isNotEmpty()) {
                        val oldSegmentsIds = segments.map { it.id!! }
                        // NOTE: orphanRemoval delete each item with separate query
                        todoParentPathRepository.removeAllSegmentsByIds(oldSegmentsIds)
                        segments.clear()
                    }
                }
                // build new parent branch paths for all children
                val childrenTodoParentPaths = todoParentPathRepository.findAllLevelChildren(movingTodo.id!!)
                childrenTodoParentPaths.forEach { childTodoParentPath ->
                    val parentWithChildrenIds = parentWithChildrenIdsInPath(childTodoParentPath, movingTodo.id!!)
                    if (childTodoParentPath.segments.isNotEmpty()) {
                        val oldSegmentsIds = childTodoParentPath.segments.map { it.id!! }
                        // NOTE: orphanRemoval delete each item with separate query
                        todoParentPathRepository.removeAllSegmentsByIds(oldSegmentsIds)
                        childTodoParentPath.segments.clear()
                    }
                    childTodoParentPath.appendIdsToParentPath(parentWithChildrenIds)
                }
                todoParentPathRepository.saveAll(
                    mutableListOf(movingTodoParentPath)
                        .apply { addAll(childrenTodoParentPaths) }
                )
            } else {
                val finalParentTodoParentPath = todoParentPathRepository.findById(finalParentTodo.id!!).get()
                if (finalParentTodoParentPath.segments.any { it.parentId == movingTodo.id }) {
                    return
                }
                movingTodo.parent = finalParentTodo
                todoRepository.save(movingTodo)
                // update todoBranches
                val movingTodoParentPath = todoParentPathRepository.findById(movingTodo.id!!).get()
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
    }

    fun getTodoHistory(todoId: Long?): TodoHistoryDto? {
        return if (todoId != null) {
            todoHistoryRepository.findById(todoId).map { it.toLightDto() }.orElse(null)
        } else null
    }

    fun updateTodoHistory(todoId: Long, todoHistoryDto: TodoHistoryDto): TodoHistoryDto? {
        // try to update
        val updatingTodoHistory = todoHistoryRepository.findById(todoId).orElse(null)
        if (todoHistoryDto.records.isNotEmpty()) {
            if (updatingTodoHistory != null) {
                updatingTodoHistory.records = mergeHistoryRecordsToPersist(todoHistoryDto.records)
                todoHistoryRepository.save(updatingTodoHistory)
                return updatingTodoHistory.toLightDto()
            } else {
                // try to create
                val owningTodoExists = todoRepository.existsById(todoId)
                if (owningTodoExists) {
                    val newTodoHistory = TodoHistory(
                        id = todoId,
                        records = mergeHistoryRecordsToPersist(todoHistoryDto.records),
                    )
                    todoHistoryRepository.save(newTodoHistory)
                    return newTodoHistory.toLightDto()
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

    private fun parentWithChildrenIdsInPath(parentPath: TodoParentPath, parentId: Long): List<Long> {
        val allIds = parentPath.segments.map { it.parentId }
        val parentIndex = allIds.indexOf(parentId)
        if (parentIndex in 0..allIds.lastIndex) {
            return allIds.subList(parentIndex, allIds.size)
        }
        return emptyList()
    }
}