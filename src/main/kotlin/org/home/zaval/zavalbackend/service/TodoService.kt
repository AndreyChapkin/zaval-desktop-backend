package org.home.zaval.zavalbackend.service

import org.home.zaval.zavalbackend.dto.MoveTodoDto
import org.home.zaval.zavalbackend.model.Todo
import org.home.zaval.zavalbackend.model.value.TodoStatus
import org.home.zaval.zavalbackend.model.view.TodoHierarchy
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
    val todoRepository: TodoRepository
) {

    companion object {
        val todoRoot: Todo = Todo(id = -1000, name = "Root", status = TodoStatus.BACKLOG)
    }

    fun createTodo(todo: Todo): Todo {
        return todoRepository.save(todo)
    }

    fun getTodo(todoId: Long?): Todo? {
        return todoId?.let { todoRepository.findById(it).orElse(null) }
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
        return todoRepository.findAll().toList()
    }

    fun moveToParent(moveTodoDto: MoveTodoDto) {
        val movingTodo = getTodo(moveTodoDto.todoId)
        val finalParentTodo = getTodo(moveTodoDto.parentId)
        if (movingTodo != null &&  finalParentTodo != null) {
            movingTodo.parent = finalParentTodo
            todoRepository.save(movingTodo)
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
        var curChildTodo = todo
        var curHierarchy = result
        while (curChildTodo.parent != null) {
            curHierarchy.parent = curChildTodo.parent!!.toShallowHierarchy()
            curHierarchy = curHierarchy.parent!!
            curChildTodo = curChildTodo.parent!!
        }
        curHierarchy.parent = todoRoot.toShallowHierarchy()
        // initialize children
        result.children = todoRepository.getAllChildrenOf(result.id).map {
            it.toShallowHierarchy()
        }?.toTypedArray()
        return result
    }
}