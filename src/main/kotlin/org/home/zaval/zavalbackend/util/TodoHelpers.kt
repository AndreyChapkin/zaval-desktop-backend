package org.home.zaval.zavalbackend.util

import org.home.zaval.zavalbackend.model.Todo
import org.home.zaval.zavalbackend.model.TodoHierarchy
import org.home.zaval.zavalbackend.store.TodoStore

fun Todo.toShallowHierarchy() = TodoHierarchy(id = this.id, name = this.name, status = this.status)

fun parentToChildrenMap(todos: List<Todo>): MutableMap<Long, MutableList<Todo>> {
    val result = mutableMapOf<Long, MutableList<Todo>>()
    todos.forEach {
        val children = (it.parentId ?: TodoStore.todoRoot.id).let { parentId ->
            result.getOrPut(parentId) { mutableListOf() }
        }
        children.add(it)
    }
    return result
}