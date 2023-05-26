package org.home.zaval.zavalbackend.repository

import org.home.zaval.zavalbackend.model.Todo

interface DeprTodoRepository {
    fun getTodo(todoId: Long): Todo?

    fun createTodos(todos: List<Todo>): List<Todo>

    fun updateTodos(todo: List<Todo>)

    fun deleteTodos(todoId: List<Long>)

    fun getAllTodo(): List<Todo>

    fun getAllChildrenOf(todoId: Long): List<Todo>

    fun getAllParentsOf(todoId: Long): List<Todo>

    fun isExist(todoId: Long): Boolean

    // TODO bad decision for batching
    fun batched(modifier: () -> Unit)
}