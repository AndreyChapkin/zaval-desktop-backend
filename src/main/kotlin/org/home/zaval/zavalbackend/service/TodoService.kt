package org.home.zaval.zavalbackend.service

import org.home.zaval.zavalbackend.dto.todo.*
import org.home.zaval.zavalbackend.entity.Todo
import org.home.zaval.zavalbackend.entity.projection.TodoLightProjection
import org.home.zaval.zavalbackend.entity.value.TodoStatus
import org.home.zaval.zavalbackend.exception.CircularTodoDependencyException
import org.home.zaval.zavalbackend.persistence.ObsidianVaultHelper
import org.home.zaval.zavalbackend.repository.TodoComplexRepository
import org.home.zaval.zavalbackend.util.asUtc
import org.home.zaval.zavalbackend.util.extractPrioritizedTodosList
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.file.Path
import java.nio.file.Paths
import java.time.OffsetDateTime
import javax.servlet.http.HttpServletRequest
import kotlin.io.path.name

@Service
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class TodoService(
    private val complexRepo: TodoComplexRepository,
    private final val obsidianVaultHelper: ObsidianVaultHelper,
    private val request: HttpServletRequest
) {

    val OBSIDIAN_TODOS_DIRECTORY = "zaval-todos-info"
    val obsidianVaultName = obsidianVaultHelper.obsidianVaultPath?.fileName?.name

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
        val resultTodos = childrenLights + (neededAllLevelParentTodoIds
            .takeIf { it.isNotEmpty() }
            ?.let {
                complexRepo
                    .repository
                    .getAllTodoLightsByIds(neededAllLevelParentTodoIds)
            }
            ?: emptyList())
        return extractPrioritizedTodosList(resultTodos)
    }

    @Transactional
    fun updateTodo(todoId: Long, updateTodoDto: TodoUpdateDto): TodoLightDto? {
        val updatedTodo = complexRepo.repository.findById(todoId).orElse(null)
            ?: return null
        var statusChanged = false
        var needToRenameObsidianNote = false
        // update general information
        if (updateTodoDto.general != null) {
            needToRenameObsidianNote = updatedTodo.name != updateTodoDto.general.name
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
        // rename obsidian note if needed
        if (needToRenameObsidianNote) {
            renameObsidianNoteFor(todoId, updateTodoDto.general!!.name)
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
        complexRepo.deleteById(todoId)
        val notePath = findNoteFor(todoId)
        if (notePath != null) {
            obsidianVaultHelper.removeFile(notePath)
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
            val parent = complexRepo.repository.findById(parentId).orElse(null)
                ?: continue
            val children = complexRepo.repository.getAllChildrenOf(parentId)
            val theHighestStatus = children.find { it.status > newStatus }?.status
                ?: newStatus
            if (parent.status != theHighestStatus) {
                complexRepo.repository.updateStatus(parentId, theHighestStatus.name)
            }
        }
    }

    // open obsidian note for the task (create if it doesn't exist)
    fun openObsidianNoteForTodo(todoId: Long, uiPageUrl: String) {
        if (obsidianVaultName == null) {
            return
        }
        val todoName = getLightTodo(todoId)?.name ?: return
        val notePath = makeNotePath(todoId, todoName)
        if (!obsidianVaultHelper.fileExists(notePath)) {
            val initialContent = """
                Link: [$uiPageUrl]($uiPageUrl)
                
                
            """.trimIndent()
            obsidianVaultHelper.writeToFile(initialContent, notePath)
        }
        // open note via system command
        val process = ProcessBuilder("cmd.exe", "/c", "start", makeObsidianLinkFor(todoId))
            .redirectErrorStream(true)
            .start()
        val exitCode = process.waitFor()
        println("Obsidian process exited with code $exitCode")
    }

    private fun renameObsidianNoteFor(todoId: Long, newName: String) {
        if (obsidianVaultName == null) {
            return
        }
        val curNotePath = findNoteFor(todoId) ?: return
        val newNotePath = makeNotePath(todoId, newName)
        obsidianVaultHelper.renameFile(curNotePath, newNotePath)
    }

    private fun findNoteFor(todoId: Long): Path? {
        return obsidianVaultHelper.allFilenamesInDir(Paths.get(OBSIDIAN_TODOS_DIRECTORY))
            .find { it.fileName.name.startsWith("$todoId - ") }
    }

    private fun makeNotePath(todoId: Long, todoName: String): Path {
        return Paths.get(OBSIDIAN_TODOS_DIRECTORY, "$todoId - $todoName.md")
    }

    private fun makeObsidianLinkFor(todoId: Long): String {
        if (obsidianVaultName == null) {
            return ""
        }
        // return obsidian://open?vault=Test%20Vault"&"file=zaval-todos-info/6
        val encodedVaultName = obsidianVaultName!!.replace(" ", "%20")
        val encodedFilename = "$OBSIDIAN_TODOS_DIRECTORY/$todoId".replace(" ", "%20")
        return "obsidian://open?vault=$encodedVaultName\"&\"file=$encodedFilename"
    }
}