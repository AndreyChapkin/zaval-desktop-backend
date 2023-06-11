package org.home.zaval.zavalbackend.service

import org.home.zaval.zavalbackend.dto.*
import org.home.zaval.zavalbackend.model.Todo
import org.home.zaval.zavalbackend.model.TodoHistory
import org.home.zaval.zavalbackend.model.TodoParentPath
import org.home.zaval.zavalbackend.model.value.TodoStatus
import org.home.zaval.zavalbackend.repository.TodoBranchRepository
import org.home.zaval.zavalbackend.repository.TodoHistoryRepository
import org.home.zaval.zavalbackend.repository.TodoRepository
import org.home.zaval.zavalbackend.util.mergeHistoryRecordsToPersist
import org.home.zaval.zavalbackend.util.toDto
import org.home.zaval.zavalbackend.util.toShallowHierarchyDto
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class TodoService(
    val todoRepository: TodoRepository,
    val todoBranchRepository: TodoBranchRepository,
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

    fun createTodo(todoDto: CreateTodoDto): TodoDto {
        val newTodo = Todo(
            id = null,
            name = todoDto.name,
            status = todoDto.status,
            parent = todoDto.parentId?.takeIf { it != TODO_ROOT.id }?.let {
                Todo(id = todoDto.parentId, name = "", status = TodoStatus.BACKLOG)
            }
        )
        val newTodoBranch = TodoParentPath(id = null, parentPath = "", isLeave = true)
        val parentParentPath = todoDto.parentId?.let {
            todoBranchRepository.findById(todoDto.parentId).orElse(null)
        }?.let { parentBranch ->
            val parentTodoParentPath = appendIdToParentPath(parentBranch.parentPath, parentBranch.id!!)
            newTodoBranch.parentPath = parentTodoParentPath
            parentBranch.isLeave = false
            parentBranch
        }
        val savedTodo = todoRepository.save(newTodo).toDto()
        newTodoBranch.id = savedTodo.id
        val savingPathes = mutableListOf(newTodoBranch).apply {
            if (parentParentPath != null) {
                add(parentParentPath)
            }
        }
        todoBranchRepository.saveAll(savingPathes)
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

    // TODO optimize
    @Transactional
    fun deleteTodo(todoId: Long) {
        val resultDeleteIds = mutableListOf<Long>()
        // delete children recursively
        val searchingQueue = LinkedList<Long>().apply {
            add(todoId)
        }
        while (searchingQueue.isNotEmpty()) {
            val curParentId = searchingQueue.removeFirst()
            resultDeleteIds.add(curParentId)
            // plan children deletion
            val curChildrenIds = todoRepository.getAllChildrenOf(curParentId).map { it.id as Long }
            searchingQueue.addAll(curChildrenIds)
        }
        todoRepository.deleteAllById(resultDeleteIds)
        todoBranchRepository.deleteAllById(resultDeleteIds)
        todoHistoryRepository.deleteAllForIds(resultDeleteIds)
    }

    /**
     * @return root <- ... parent <- todoElement -> children[]
     */
    fun getTodoBranch(todoId: Long?): TodoHierarchyDto? {
        val todo: Todo? = if (todoId == null) TODO_ROOT else todoRepository.findById(todoId).orElse(null)
        return buildTodoBranch(todo)
    }

    fun getAllTodos(status: TodoStatus?): List<TodoDto> {
        return status
            ?.let { todoRepository.getAllTodosWithStatus(it.name.uppercase()).map { it.toDto() } }
            ?: todoRepository.getAllTodos().map { it.toDto() }
    }

    fun getAllUpBranches(status: TodoStatus?): List<TodoHierarchyDto> {
        val leavePaths = status
            ?.let {
                todoBranchRepository.getAllLeavesTodoParentPathsWithTodoStatus(it)
            }
            ?: todoBranchRepository.getAllLeavesTodoParentPaths()
        val leavesIdsSet = leavePaths.map { it.id }.toSet()
        val leaveBranchAndOrderedParentsIds = mutableMapOf<Long, List<Long>>()
        val parentAndChildrenIds = mutableMapOf<Long, MutableList<Long>>()
        val parentIdsSet = leavePaths.flatMap {
            // also construct parent -> children map
            val parentIds = extractIdsFromParentPath(it.parentPath)
            for (parentId in parentIds) {
                parentAndChildrenIds
                    .getOrPut(parentId) { mutableListOf() }
                    .apply { add(it.id!!) }
            }
            // also construct child -> parents map
            leaveBranchAndOrderedParentsIds[it.id!!] = parentIds
            parentIds
        }.toSet()
        val allIdsSet = leavesIdsSet.toMutableSet().apply {
            addAll(parentIdsSet)
        }
        val allTodos = todoRepository.findAllById(allIdsSet)
        val leaveTodos = mutableListOf<Todo>()
        val leaveIdAndParentsTodos = mutableMapOf<Long, MutableList<Todo>>()
        for (todo in allTodos) {
            if (leaveBranchAndOrderedParentsIds.containsKey(todo.id)) {
                // save leave todoHierarchy
                leaveTodos.add(todo)
            } else if (parentAndChildrenIds.containsKey(todo.id)) {
                // pre-save parent for all its children
                parentAndChildrenIds[todo.id]?.let { childrenIds ->
                    for (childId in childrenIds) {
                        leaveIdAndParentsTodos
                            .getOrPut(childId) { mutableListOf() }
                            .apply { add(todo) }
                    }
                }
            }
        }
        // construct hierarchies
        val result = leaveTodos.map { leaveTodo ->
            val orderedParentTodos = leaveIdAndParentsTodos[leaveTodo.id]?.let { parentTodos ->
                val orderedParentIds = leaveBranchAndOrderedParentsIds[leaveTodo.id] ?: emptyList()
                orderedParentIds.mapNotNull { id ->
                    parentTodos.find { it.id == id }
                }
            } ?: emptyList()
            buildTodoHierarchy(leaveTodo, orderedParentTodos)
        }
        return result
    }

    fun moveTodo(moveTodoDto: MoveTodoDto) {
        val movingTodo = loadTodo(moveTodoDto.todoId)
        val finalParentTodo = loadTodo(moveTodoDto.parentId)
        if (movingTodo != null && finalParentTodo != null) {
            movingTodo.parent = finalParentTodo
            todoRepository.save(movingTodo)
            // update todoBranches
            val movingTodoBranch = todoBranchRepository.findById(movingTodo.id!!).get()
            val finalParentBranch = todoBranchRepository.findById(finalParentTodo.id!!).get()
            // build new parent branch path for itself
            movingTodoBranch.parentPath = appendIdToParentPath(finalParentBranch.parentPath, finalParentTodo.id!!)
            finalParentBranch.isLeave = false
            // build new parent branch paths for all children
            val childrenBranches = todoBranchRepository.findAllLevelChildren(
                toParentPathPattern(movingTodo.id!!),
            )
            childrenBranches.forEach {
                it.parentPath = appendIdsToParentPath(
                    movingTodoBranch.parentPath,
                    parentAndChildrenIdsInPath(it.parentPath, movingTodo.id!!)
                )
            }
            todoBranchRepository.saveAll(
                childrenBranches.toMutableList().apply {
                    add(movingTodoBranch)
                    add(finalParentBranch)
                }
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

    private fun extractIdsFromParentPath(parentPath: String?): List<Long> = parentPath
        ?.takeIf { it.isNotEmpty() }
        ?.let { notEmptyPath ->
            notEmptyPath.split(PARENT_PATH_IDS_SEPARATOR).filter { it.isNotEmpty() }.map { it.toLong() }
        }
        ?: emptyList()

    private fun appendIdsToParentPath(parentPath: String?, ids: List<Long>): String =
        if (ids.isEmpty()) (parentPath ?: "")
        else (
            parentPath
                ?.takeIf { it.isNotEmpty() }
                ?.let { it + ids.joinToString(separator = PARENT_PATH_IDS_SEPARATOR) }
                ?: (PARENT_PATH_IDS_SEPARATOR + ids.joinToString(separator = PARENT_PATH_IDS_SEPARATOR))
            ) + PARENT_PATH_IDS_SEPARATOR

    private fun appendIdToParentPath(parentPath: String?, id: Long): String = (
        parentPath
            ?.takeIf { it.isNotEmpty() }
            ?.let { it + id }
            ?: (PARENT_PATH_IDS_SEPARATOR + id.toString())
        ) + PARENT_PATH_IDS_SEPARATOR

    private fun parentAndChildrenIdsInPath(parentPath: String, parentId: Long): List<Long> {
        val allIds = extractIdsFromParentPath(parentPath)
        val parentIndex = allIds.indexOf(parentId)
        if (parentIndex in 0..allIds.lastIndex) {
            return allIds.subList(parentIndex, allIds.size)
        }
        return emptyList()
    }

    private fun toParentPathPattern(id: Long) = "%${PARENT_PATH_IDS_SEPARATOR}$id${PARENT_PATH_IDS_SEPARATOR}%"

    private fun buildTodoBranch(todo: Todo?): TodoHierarchyDto? {
        if (todo == null) {
            return null
        }
        // initialize main node
        val result = todo.toShallowHierarchyDto()
        if (todo.id != TODO_ROOT.id) {
            // initialize parents chain
            val parentsPath = todoBranchRepository.getParentsPath(todo.id!!)
            val parentTodos = todoRepository.findAllById(extractIdsFromParentPath(parentsPath))
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