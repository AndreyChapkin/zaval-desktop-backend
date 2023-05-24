package org.home.zaval.zavalbackend.service

import org.home.zaval.zavalbackend.dto.MoveTodoDto
import org.home.zaval.zavalbackend.model.Todo
import org.home.zaval.zavalbackend.model.value.TodoStatus
import org.home.zaval.zavalbackend.model.view.TodoHierarchy
import org.home.zaval.zavalbackend.repository.TodoRepository
import org.home.zaval.zavalbackend.util.Copier
import org.home.zaval.zavalbackend.util.toShallowHierarchy
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Service
import java.util.LinkedList

@Service
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class TodoService(
    val todoRepository: TodoRepository
) {

    companion object {
        val todoRoot: Todo = Todo(id = -1000, name = "Root", status = TodoStatus.BACKLOG)
    }

    val todoAndChildren: Map<Long, MutableList<Todo>>
        get() = parentToChildrenMap(todoRepository.getAllTodo())    // TODO optimize

    fun createTodos(todos: List<Todo>): List<Todo> {
        var resultTodos = mutableListOf<Todo>()
        // only existing parents
        for (todo in todos) {
            var resultTodo = todo
            if (todo.parentId != null) {
                val parentExists = todoRepository.isExist(todo.parentId)
                if (!parentExists) {
                    resultTodo = Copier(todo).copyWithOverriding {
                        todo::parentId overrideWith null
                    }
                }
            }
            resultTodos.add(resultTodo)
        }
        return todoRepository.createTodos(resultTodos)
    }

    fun getTodo(todoId: Long?): Todo? {
        return todoId?.let { todoRepository.getTodo(it) }
    }

    // TODO downgrade status of parent task to all child tasks statuses
    fun updateTodo(todoId: Long, todo: Todo): Todo? {
        val updatingTodo = getTodo(todoId)
        if (updatingTodo != null) {
            val resultChildTodo = Copier(updatingTodo).copyWithOverriding {
                updatingTodo::name overrideWith todo.name
                updatingTodo::status overrideWith todo.status
            }
            todoRepository.batched {
                // also upgrade parents' statuses
                // TODO optimize
                val parentTodos = todoRepository.getAllParentsOf(resultChildTodo.id)
                val updatedParents = upgradeAllParentStatuses(resultChildTodo, parentTodos)
                val allUpdatedTodos = updatedParents.toMutableList().apply {
                    add(resultChildTodo)
                }
                todoRepository.updateTodos(allUpdatedTodos)
            }
            return resultChildTodo
        }
        return null
    }

    fun deleteTodos(todoIds: List<Long>) {
        todoRepository.batched {
            val resultDeleteIds = mutableListOf<Long>()
            // delete children recursively
            val searchingQueue = LinkedList(todoIds)
            while (searchingQueue.isNotEmpty()) {
                val curParentId = searchingQueue.removeFirst()
                resultDeleteIds.add(curParentId)
                // plan children deletion
                val curChildrenIds = todoRepository.getAllChildrenOf(curParentId).map { it.id }
                searchingQueue.addAll(curChildrenIds)
            }
            todoRepository.deleteTodos(resultDeleteIds)
        }
    }

    /**
     * @return root <- ... parent <- todoElement -> children[]
     */
    fun getTodoBranch(todoId: Long): TodoHierarchy? {
        val todo = todoRepository.getTodo(todoId)
        return todo?.let { buildTodoBranch(it) }
    }

    fun getAllTodos(): List<Todo> {
        return todoRepository.getAllTodo()
    }

    fun moveToParent(moveTodoDto: MoveTodoDto) {
        val movingTodo = getTodo(moveTodoDto.todoId)
        if (movingTodo != null && getTodo(moveTodoDto.parentId) != null) {
            val resultTodo = Copier(movingTodo).copyWithOverriding {
                movingTodo::parentId overrideWith moveTodoDto.parentId
            }
            todoRepository.updateTodos(listOf(resultTodo))
        }
    }

    private fun upgradeAllParentStatuses(todo: Todo, parents: List<Todo>): List<Todo> {
        val updatedParents = mutableListOf<Todo>()
        for (parent in parents) {
            if (parent.status.priority < todo.status.priority) {
                val updatedParent = Copier(parent).copyWithOverriding {
                    parent::status overrideWith todo.status
                }
                updatedParents.add(updatedParent)
            }
        }
        return updatedParents
    }

    private fun buildTodoBranch(todo: Todo): TodoHierarchy {
        // initialize main node
        val result = todo.toShallowHierarchy()
        // initialize parents chain
        var curParentTodo = getTodo(todo.parentId)
        var curChild = result
        while (curParentTodo != null) {
            curChild.parent = curParentTodo.toShallowHierarchy()
            curChild = curChild.parent!!
            curParentTodo = getTodo(curParentTodo.parentId)
        }
        curChild.parent = todoRoot.toShallowHierarchy()
        // initialize children
        result.children = todoAndChildren[result.id]?.map {
            it.toShallowHierarchy()
        }?.toTypedArray()
        return result
    }

    private fun parentToChildrenMap(todos: List<Todo>): MutableMap<Long, MutableList<Todo>> {
        val result = mutableMapOf<Long, MutableList<Todo>>()
        todos.forEach {
            val children = (it.parentId ?: todoRoot.id).let { parentId ->
                result.getOrPut(parentId) { mutableListOf() }
            }
            children.add(it)
        }
        return result
    }
}