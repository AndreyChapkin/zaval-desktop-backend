package org.home.zaval.zavalbackend.service

import org.home.zaval.zavalbackend.dto.todo.*
import org.home.zaval.zavalbackend.entity.Todo
import org.home.zaval.zavalbackend.entity.TodoHistory
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
        val allParentIds = TodoStore.getAllParentsOf(todo.id!!)
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
            val curAllParentIds = TodoStore.getAllParentsOf(it.getId()!!)
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
                val finalParentParentIds = TodoStore.getAllParentsOf(finalParentTodo.id!!)
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
}