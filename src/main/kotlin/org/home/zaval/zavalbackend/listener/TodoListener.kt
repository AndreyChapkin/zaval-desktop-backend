package org.home.zaval.zavalbackend.listener

import org.home.zaval.zavalbackend.entity.Todo
import org.home.zaval.zavalbackend.store.TodoStore
import org.home.zaval.zavalbackend.util.toFullDto
import javax.persistence.PostPersist
import javax.persistence.PostRemove
import javax.persistence.PostUpdate

class TodoListener {

    @PostPersist
    fun afterPersist(todo: Todo) {
        TodoStore.saveOrUpdateTodo(todo.toFullDto())
    }

    @PostUpdate
    fun afterUpdate(todo: Todo) {
        println("@@@ Updated Todo id = ${todo.id}, description = ${todo.description}")
        TodoStore.saveOrUpdateTodo(todo.toFullDto())
    }

    @PostRemove
    fun afterRemove(todo: Todo) {
        TodoStore.removeTodo(todo.toFullDto())
    }
}