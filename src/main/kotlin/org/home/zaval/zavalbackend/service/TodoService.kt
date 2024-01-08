package org.home.zaval.zavalbackend.service

import org.home.zaval.zavalbackend.dto.todo.*
import org.home.zaval.zavalbackend.entity.Todo
import org.home.zaval.zavalbackend.entity.TodoHistory
import org.home.zaval.zavalbackend.entity.projection.TodoLightProjection
import org.home.zaval.zavalbackend.entity.value.TodoStatus
import org.home.zaval.zavalbackend.exception.CircularTodoDependencyException
import org.home.zaval.zavalbackend.repository.TodoComplexRepository
import org.home.zaval.zavalbackend.repository.TodoHistoryRepository
import org.home.zaval.zavalbackend.util.*
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.*

@Service
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class TodoService(
    val complexRepo: TodoComplexRepository,
    val todoHistoryRepository: TodoHistoryRepository,
) {

    @Transactional
    fun createTodo(todoDto: TodoCreateDto): TodoLightDto {
        val newTodo = todoDto.toEntity()
        return complexRepo.save(newTodo).toDto()
    }

    fun getLightTodo(id: Long?): TodoLightDto? {
        return id?.let {
            complexRepo
                .repository
                .getAllTodoLightsByIds(listOf(id))
                .takeIf { it.isNotEmpty() }
                ?.first()
                ?.toDto()
        }
    }

    fun getTodoDescription(id: Long?): String? {
        return id?.let {
            complexRepo
                .repository
                .getDescription(id)
        }
    }

    fun getTheMostDatedTodoLights(count: Int?, orderType: String?): List<TodoLightDto> {
        val sortType = if (orderType == "recent") {
            Sort.by(Sort.Order.desc(Todo::interactedOn.name))
        } else {
            Sort.by(Sort.Order.asc(Todo::interactedOn.name))
        }
        return complexRepo
            .repository
            .findAll(
                PageRequest.of(0, count ?: 10, sortType)
            ).content
            .map { it.toDto() }
    }

    fun getRootTodos(): List<TodoLightDto> {
        return complexRepo
            .repository
            .getAllRootTodos()
            .map { it.toDto() }
    }

    /**
     * @return todoElement with ordered parents: root, parent 1, parent 2, ...
     * and unordered children
     */
    @Transactional
    fun getTodoFamily(todoId: Long?): TodoFamilyDto? {
        if (todoId == null) {
            return null
        }
        val todo: Todo = complexRepo
            .repository.findById(todoId)
            .orElse(null)
            ?: return null
        todo.interactedOn = OffsetDateTime.now().asUtc
        complexRepo.save(todo)
        val orderedParentIds = complexRepo.getOrderedParentIdsOf(todo.id!!)
        val parentTodos = complexRepo.repository.findAllById(orderedParentIds)
        val orderedParentTodos = orderedParentIds.map { parentId ->
            parentTodos.find { it.id == parentId }!!
        }
        val childrenTodos = complexRepo.repository.getAllChildrenOf(todo.id!!)
        return todo.toFamilyDto(
            orderedParentTodos.map { it.toDto() },
            childrenTodos.map { it.toDto() }
        )
    }

    fun findAllTodoLightsWithNameFragment(nameFragment: String): List<TodoLightDto> {
        return complexRepo.repository.findAllTodoLightsWithNameFragment("%$nameFragment%")
            .map { it.toDto() }
    }

    fun getPrioritizedListOfTodosWithStatus(status: TodoStatus): TodoLeavesAndBranchesDto {
        val allTodosWithStatus = complexRepo.repository.getAllTodosWithStatus(status.name.uppercase())
        return transformToLists(allTodosWithStatus)
    }

    fun getPrioritizedListOfTodos(todoIds: List<Long>): TodoLeavesAndBranchesDto {
        val childrenLightViews = complexRepo.repository.getAllTodoLightsByIds(todoIds)
        return transformToLists(childrenLightViews)
    }

    private fun transformToLists(childrenLights: List<TodoLightProjection>): TodoLeavesAndBranchesDto {
        val neededAllLevelParentTodoIds = childrenLights.flatMap {
            complexRepo.getOrderedParentIdsOf(it.getId())
        }
        val resultTodos = if (neededAllLevelParentTodoIds.isNotEmpty()) {
            childrenLights + complexRepo
                .repository
                .getAllTodoLightsByIds(neededAllLevelParentTodoIds)
        } else {
            emptyList()
        }
        return extractPrioritizedTodosList(resultTodos)
    }

    @Transactional
    fun updateTodo(todoId: Long, updateTodoDto: TodoUpdateDto): TodoLightDto? {
        val updatedTodo = complexRepo.repository.findById(todoId).orElse(null)
            ?: return null
        var statusChanged = false
        // update general information
        if (updateTodoDto.general != null) {
            updatedTodo.name = updateTodoDto.general.name
            updatedTodo.priority = updateTodoDto.general.priority
            statusChanged = updatedTodo.status != updateTodoDto.general.status
            updatedTodo.status = updateTodoDto.general.status
        }
        if (updateTodoDto.description != null) {
            updatedTodo.description = updateTodoDto.description
        }
        updatedTodo.interactedOn = OffsetDateTime.now().asUtc
        complexRepo.save(updatedTodo)
        if (statusChanged) {
            correctAllParentStatuses(todoId, updatedTodo.status)
        }
        return updatedTodo.toDto()
    }

    @Transactional
    fun moveTodo(moveDto: TodoMoveDto) {
        if (
            moveDto.todoId == moveDto.parentId
            || !complexRepo.repository.existsById(moveDto.todoId)
        ) {
            return
        }
        if (moveDto.parentId != null) {
            if (!complexRepo.repository.existsById(moveDto.parentId)) {
                return
            }
            // don't allow circular dependencies
            val newParentsAllParentIds = complexRepo.getOrderedParentIdsOf(moveDto.parentId)
            if (newParentsAllParentIds.contains(moveDto.todoId)) {
                throw CircularTodoDependencyException(
                    finalParentId = moveDto.parentId,
                    movingTodoId = moveDto.todoId
                )
            }
        }
        complexRepo.updateParent(moveDto.todoId, moveDto.parentId)
    }

    @Transactional
    fun deleteTodo(todoId: Long) {
        val allDeletedIds = complexRepo.deleteById(todoId)
        todoHistoryRepository.deleteAllForIds(allDeletedIds)
    }

    fun getTodoHistory(todoId: Long?): TodoHistoryDto? {
        return todoId?.let { id ->
            todoHistoryRepository.findById(id).map { it.toDto() }.orElse(null)
        }
    }

    fun updateTodoHistory(todoId: Long, todoHistoryDto: TodoHistoryDto) {
        // try to update
        if (todoHistoryRepository.existsById(todoId)) {
            // try to update or delete
            if (todoHistoryDto.records.isNotEmpty()) {
                todoHistoryRepository.updateRecords(todoId, mergeHistoryRecords(todoHistoryDto.records))
            } else {
                todoHistoryRepository.deleteById(todoId)
            }
        } else {
            // try to create
            val owningTodoExists = complexRepo.repository.existsById(todoId)
            if (owningTodoExists) {
                val newTodoHistory = TodoHistory(
                    id = todoId,
                    records = mergeHistoryRecords(todoHistoryDto.records),
                )
                todoHistoryRepository.save(newTodoHistory)
            }
        }
    }

    private fun updateInteractedOn(id: Long): OffsetDateTime {
        val currentDate = OffsetDateTime.now().asUtc
        complexRepo.repository.updateInteractedOn(id, currentDate)
        return currentDate
    }

    private fun correctAllParentStatuses(id: Long, newStatus: TodoStatus) {
        val reversedAllOrderedParentIds = complexRepo
            .getOrderedParentIdsOf(id)
            .asReversed()
        for (parentId in reversedAllOrderedParentIds) {
            val children = complexRepo.repository.getAllChildrenOf(parentId)
            val hasHigherPriority = children.find { it.status > newStatus } != null
            if (!hasHigherPriority) {
                complexRepo.repository.updateStatus(parentId, newStatus)
            }
        }
    }
}