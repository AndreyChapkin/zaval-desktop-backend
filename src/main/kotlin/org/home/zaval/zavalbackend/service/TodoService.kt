package org.home.zaval.zavalbackend.service

import org.home.zaval.zavalbackend.dto.todo.*
import org.home.zaval.zavalbackend.entity.Todo
import org.home.zaval.zavalbackend.entity.TodoHistory
import org.home.zaval.zavalbackend.entity.TodoLightView
import org.home.zaval.zavalbackend.entity.value.TodoStatus
import org.home.zaval.zavalbackend.exception.CircularTodoDependencyException
import org.home.zaval.zavalbackend.repository.TodoHistoryRepository
import org.home.zaval.zavalbackend.repository.TodoRepository
import org.home.zaval.zavalbackend.store.TodoStore
import org.home.zaval.zavalbackend.util.*
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.LinkedList

@Service
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class TodoService(
    val todoRepository: TodoRepository,
    val todoHistoryRepository: TodoHistoryRepository,
) {

    @Transactional
    fun createTodo(todoDto: CreateTodoDto): LightTodoDto {
        val newTodo = todoDto.toEntity().apply {
            id = TodoStore.getId()
        }
        val savedTodo = todoRepository.save(newTodo).toLightDto()
        return savedTodo
    }

    fun getLightTodo(todoId: Long?): LightTodoDto? {
        return loadTodo(todoId)?.toLightDto()
    }

    // TODO optimize data fetching
    fun updateTodo(todoId: Long, updateTodoDto: UpdateTodoDto): LightTodoDto? {
        val updatedTodo = loadTodo(todoId)
        var statusChanged = false
        if (updatedTodo != null) {
            // update general information
            if (updateTodoDto.general != null) {
                updatedTodo.name = updateTodoDto.general.name
                updatedTodo.priority = updateTodoDto.general.priority
                // Can not change status of the parent directly
                val directChildrenIds = TodoStore.getDirectChildrenOf(todoId)
                if (directChildrenIds.isEmpty()) {
                    statusChanged = updatedTodo.status != updateTodoDto.general.status
                    updatedTodo.status = updateTodoDto.general.status
                }
            }
            if (updateTodoDto.description != null) {
                updatedTodo.description = updateTodoDto.description
            }
            todoRepository.save(updatedTodo)
            val allOrderedParentIds = TodoStore.getAllOrderedParentIdsOf(updatedTodo.id!!)
            // also upgrade statuses of the parents
            if (statusChanged) {
                correctAllParentStatuses(updatedTodo, allOrderedParentIds)
            }
            return updatedTodo.toLightDto()
        }
        return null
    }

    // TODO optimize. Standard delete methods generate too many queries
    @Transactional
    fun deleteTodo(todoId: Long) {
        val allLevelChildrenIds = TodoStore.getAllLevelChildrenOf(todoId)
        val resultDeleteIds = mutableListOf<Long>()
            .apply { add(todoId) }
            .apply { addAll(allLevelChildrenIds) }
        todoRepository.deleteAllById(resultDeleteIds)
        todoHistoryRepository.deleteAllForIds(resultDeleteIds)
    }

    fun deleteAllOutdatedTodos() {
        TodoStore.deleteAllOutdatedTodos()
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
        val allParentIds = TodoStore.getAllOrderedParentIdsOf(todo.id!!)
        val parentTodos = todoRepository.findAllById(allParentIds)
        val orderedParentTodos = allParentIds.map { parentId ->
            parentTodos.find { it.id == parentId }!!
        }
        val childrenTodos = todoRepository.getAllChildrenOf(todo.id!!)
        return todo.toDetailedDto(
            orderedParentTodos.map { it.toLightDto() },
            childrenTodos.map { it.toLightDto() }
        )
    }

    fun getHeavyDetails(todoId: Long?): HeavyDetailsDto? {
        if (todoId == null) {
            return null
        }
        val description = todoRepository.getDescription(todoId)
        val history = todoHistoryRepository.findById(todoId).orElse(null)
        return HeavyDetailsDto(
            todoId = todoId,
            description = description,
            history = history?.toLightDto()
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
        val allTodosWithStatus = todoRepository.getAllTodosWithStatus(status.name.uppercase())
        val neededAllLevelParentTodoIds = mutableSetOf<Long>()
        allTodosWithStatus.forEach {
            val curAllParentIds = TodoStore.getAllOrderedParentIdsOf(it.getId()!!)
            neededAllLevelParentTodoIds.addAll(curAllParentIds)
        }
        val resultTodos = allTodosWithStatus.toMutableList()
        if (neededAllLevelParentTodoIds.isNotEmpty()) {
            val allLevelParentTodos = todoRepository.getAllShallowTodosByIds(neededAllLevelParentTodoIds)
            resultTodos.addAll(allLevelParentTodos)
        }
        return extractPrioritizedTodosList(resultTodos)
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
            } else {
                // Eliminate circular dependencies
                val finalParentParentIds = TodoStore.getAllOrderedParentIdsOf(finalParentTodo.id!!)
                if (finalParentParentIds.contains(movingTodo.id)) {
                    throw CircularTodoDependencyException(
                        finalParentId = finalParentTodo.id!!,
                        movingTodoId = movingTodo.id!!
                    )
                }
                movingTodo.parent = finalParentTodo
                todoRepository.save(movingTodo)
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

    // TODO: optimize data fetching
    private fun correctAllParentStatuses(todo: Todo, orderedParentIds: List<Long>) {
        val allOrderedParentLightViews = LinkedList<TodoLightView>()
        val allParentLightViews = todoRepository.getAllShallowTodosByIds(orderedParentIds)
        orderedParentIds.forEach { id ->
            val parentView = allParentLightViews.find { it.getId() == id }
            allOrderedParentLightViews.addLast(parentView)
        }
        var curAlreadyConsideredChildId = todo.id!!
        var theHighestStatus = todo.status
        while (allOrderedParentLightViews.isNotEmpty()) {
            // Get not considered children of the current parent
            val curParentLightView = allOrderedParentLightViews.pollLast()!!
            val directNotConsideredChildrenIds = TodoStore
                .getDirectChildrenOf(curParentLightView.getId()!!)
                .filter { it != curAlreadyConsideredChildId }
            val curNotConsideredChildrenLightViews = if (directNotConsideredChildrenIds.isNotEmpty())
                todoRepository.getAllShallowTodosByIds(directNotConsideredChildrenIds)
            else
                emptyList()
            // Find the highest status among the children
            for (child in curNotConsideredChildrenLightViews) {
                if (theHighestStatus.priority < child.getStatus().priority) {
                    theHighestStatus = child.getStatus()
                }
            }
            // Any parent must have the highest status from its children
            if (curParentLightView.getStatus() != theHighestStatus) {
                val curParentTodo = todoRepository.findById(curParentLightView.getId()!!).get()
                curParentTodo.status = theHighestStatus
                todoRepository.save(curParentTodo)
            }
            curAlreadyConsideredChildId = curParentLightView.getId()!!
        }
    }
}