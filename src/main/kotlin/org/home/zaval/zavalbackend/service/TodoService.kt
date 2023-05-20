package org.home.zaval.zavalbackend.service

import org.home.zaval.zavalbackend.dto.MoveTodoDto
import org.home.zaval.zavalbackend.model.Todo
import org.home.zaval.zavalbackend.model.TodoHierarchy
import org.home.zaval.zavalbackend.store.TodoStore
import org.home.zaval.zavalbackend.util.parentToChildrenMap
import org.home.zaval.zavalbackend.util.toShallowHierarchy
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Service

@Service
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class TodoService(
    val todoStore: TodoStore
) {

    val todoAndChildren: Map<Long, MutableList<Todo>> by lazy {
        parentToChildrenMap(todoStore.getAllTodo())
    }

    /**
     * @return root <- ... parent <- todoElement -> children[]
     */
    fun getTodoBranch(todoId: Long): TodoHierarchy? {
        val todo = todoStore.getTodo(todoId)
        return todo?.let { buildTodoBranch(it) }
    }

    fun getAllTodos(): List<Todo> {
        return todoStore.getAllTodo()
    }

    fun getTodo(todoId: Long?): Todo? {
        return todoId?.let { todoStore.getTodo(it) }
    }

    fun createTodo(todo: Todo): Todo {
        return todoStore.createTodo(todo)
    }

    fun moveToParent(moveTodoDto: MoveTodoDto) {
        val updatedTodo = getTodo(moveTodoDto.todoId)
        if (updatedTodo != null && getTodo(moveTodoDto.parentId) != null) {
            val resultTodo = updatedTodo.copy(parentId = moveTodoDto.parentId)
            todoStore.updateTodo(resultTodo)
        }
    }

    fun updateTodo(todoId: Long, todo: Todo): Todo? {
        val updatedTodo = getTodo(todoId)
        if (updatedTodo != null) {
            val result = updatedTodo.copy(name = todo.name, status = todo.status)
            todoStore.updateTodo(result)
            return result
        }
        return null
    }

    fun deleteTodo(todoId: Long) {
        todoStore.deleteTodo(todoId)
    }

    fun buildTodoBranch(todo: Todo): TodoHierarchy {
        // initialize main node
        val result = TodoHierarchy(id = todo.id, name = todo.name, status = todo.status)
        // initialize parents chain
        var curParentTodo = getTodo(todo.parentId)
        var curChild = result
        while (curParentTodo != null) {
            curChild.parent = curParentTodo.toShallowHierarchy()
            curChild = curChild.parent!!
            curParentTodo = getTodo(curParentTodo.parentId)
        }
        curChild.parent = TodoStore.todoRoot.toShallowHierarchy()
        // initialize children
        result.children = todoAndChildren[result.id]?.map {
            it.toShallowHierarchy()
        }?.toTypedArray()
        return result
    }
}