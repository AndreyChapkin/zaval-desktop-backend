package org.home.zaval.zavalbackend.service

import org.home.zaval.zavalbackend.dto.MoveTodoDto
import org.home.zaval.zavalbackend.model.Todo
import org.home.zaval.zavalbackend.model.TodoParentPath
import org.home.zaval.zavalbackend.model.value.TodoStatus
import org.home.zaval.zavalbackend.model.view.TodoHierarchy
import org.home.zaval.zavalbackend.repository.TodoBranchRepository
import org.home.zaval.zavalbackend.repository.TodoRepository
import org.home.zaval.zavalbackend.util.makeCopyWithOverriding
import org.home.zaval.zavalbackend.util.toShallowHierarchy
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Service
import java.util.*

@Service
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class TodoService(
    val todoRepository: TodoRepository,
    val todoBranchRepository: TodoBranchRepository,
) {

    companion object {
        val todoRoot: Todo = Todo(
            id = -1000,
            name = "Root",
            status = TodoStatus.BACKLOG,
        )

        val PARENT_PATH_IDS_SEPARATOR = "/"
    }

    fun extractIdsFromParentPath(parentPath: String?): List<Long> = parentPath
        ?.takeIf { it.isNotEmpty() }
        ?.let { notEmptyPath ->
            notEmptyPath.split(PARENT_PATH_IDS_SEPARATOR).filter { it.isNotEmpty() }.map { it.toLong() }
        }
        ?: emptyList()

    fun appendIdsToParentPath(parentPath: String?, ids: List<Long>): String =
        if (ids.isEmpty()) (parentPath ?: "")
        else (
            parentPath
                ?.takeIf { it.isNotEmpty() }
                ?.let { it + ids.joinToString(separator = PARENT_PATH_IDS_SEPARATOR) }
                ?: (PARENT_PATH_IDS_SEPARATOR + ids.joinToString(separator = PARENT_PATH_IDS_SEPARATOR))
            ) + PARENT_PATH_IDS_SEPARATOR

    fun appendIdToParentPath(parentPath: String?, id: Long): String = (
        parentPath
            ?.takeIf { it.isNotEmpty() }
            ?.let { it + id }
            ?: (PARENT_PATH_IDS_SEPARATOR + id.toString())
        ) + PARENT_PATH_IDS_SEPARATOR

    fun parentAndChildrenIdsInPath(parentPath: String, parentId: Long): List<Long> {
        val allIds = extractIdsFromParentPath(parentPath)
        val parentIndex = allIds.indexOf(parentId)
        if (parentIndex in 0 .. allIds.lastIndex) {
            return allIds.subList(parentIndex, allIds.size)
        }
        return emptyList()
    }

    fun toParentPathPattern(id: Long) = "%${PARENT_PATH_IDS_SEPARATOR}$id${PARENT_PATH_IDS_SEPARATOR}%"

    fun createTodo(todo: Todo): Todo {
        todo.todoBranch = TodoParentPath(id = null, todo = todo, parentPath = "")
        if (todo.parent?.id != null) {
            val parentBranch = todoBranchRepository.findById(todo.parent!!.id!!).orElse(null)
            if (parentBranch != null) {
                val todoParentsPath = appendIdToParentPath(parentBranch.parentPath, todo.parent!!.id!!)
                todo.todoBranch!!.parentPath = todoParentsPath
            } else {
                todo.parent = null
            }
        }
        return todoRepository.save(todo)
    }

    fun getTodo(todoId: Long?): Todo? {
        return todoId?.let {
            todoRepository.findById(it).orElse(null)?.let {
                it.makeCopyWithOverriding {
                    fill(Todo::parent).withValue(null)
                    fill(Todo::todoBranch).withValue(null)
                }
            }
        }
    }

    // TODO downgrade status of parent task to all child tasks statuses
    fun updateTodo(todoId: Long, todo: Todo): Todo? {
        val updatingTodo = getTodo(todoId)
        if (updatingTodo != null) {
            updatingTodo.name = todo.name
            updatingTodo.status = todo.status
            // also upgrade parents' statuses
            // TODO optimize
            val updatedParents = upgradeAllParentStatuses(updatingTodo)
            todoRepository.saveAll(mutableListOf(updatingTodo).apply {
                addAll(updatedParents)
            })
            return updatingTodo
        }
        return null
    }

    // TODO optimize
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
    }

    /**
     * @return root <- ... parent <- todoElement -> children[]
     */
    fun getTodoBranch(todoId: Long): TodoHierarchy? {
        val todo = todoRepository.findById(todoId).orElse(null)
        return todo?.let { buildTodoBranch(it) }
    }

    fun getAllTodos(): List<Todo> {
        // @@@ delete
        todoBranchRepository.findAll().forEach {
            println("@@@ ${it.id}: ${it.parentPath}")
        }
        // @@@ end
        return todoRepository.findAll().toList()
    }

    fun moveToParent(moveTodoDto: MoveTodoDto) {
        val movingTodo = getTodo(moveTodoDto.todoId)
        val finalParentTodo = getTodo(moveTodoDto.parentId)
        if (movingTodo != null && finalParentTodo != null) {
            movingTodo.parent = finalParentTodo
            todoRepository.save(movingTodo)
            // update todoBranches
            val movingTodoBranch = todoBranchRepository.findById(movingTodo.id!!).get()
            val finalParentBranch = todoBranchRepository.findById(finalParentTodo.id!!).get()
            // build new parent branch path for itself
            println("@@@ before: ")
            println("@@@ selfParents [${movingTodoBranch.id}] = ${movingTodoBranch.parentPath}")
            movingTodoBranch.parentPath = appendIdToParentPath(finalParentBranch.parentPath, finalParentTodo.id!!)
            // build new parent branch paths for all children
            val childrenBranches = todoBranchRepository.findAllLevelChildren(
                pathPattern = toParentPathPattern(movingTodo.id!!),
            )
            childrenBranches.forEach {
                println("@@@ childParents [${it.id}] = ${it.parentPath}")
            }
            childrenBranches.forEach {
                it.parentPath = appendIdsToParentPath(
                    movingTodoBranch.parentPath,
                    parentAndChildrenIdsInPath(it.parentPath, movingTodo.id!!)
                )
            }
            todoBranchRepository.saveAll(
                childrenBranches.toMutableList()
                    .apply { add(movingTodoBranch) }
            )
            println("@@@ after: ")
            println("@@@ selfParents [${movingTodoBranch.id}] = ${movingTodoBranch.parentPath}")
            childrenBranches.forEach {
                println("@@@ childParents [${it.id}] = ${it.parentPath}")
            }
        }
    }

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

    private fun buildTodoBranch(todo: Todo): TodoHierarchy {
        // initialize main node
        val result = todo.toShallowHierarchy()
        // initialize parents chain
        val parentsPath = todoBranchRepository.getParentsPath(todo.id!!)
        val parentTodos = todoRepository.findAllById(extractIdsFromParentPath(parentsPath))
        var curHierarchy = result
        parentTodos.reversed().forEach {
            curHierarchy.parent = it.toShallowHierarchy()
            curHierarchy = curHierarchy.parent!!
        }
        curHierarchy.parent = todoRoot.toShallowHierarchy()
        // initialize children
        result.children = todoRepository.getAllChildrenOf(result.id).map {
            it.toShallowHierarchy()
        }.toTypedArray()
        return result
    }
}