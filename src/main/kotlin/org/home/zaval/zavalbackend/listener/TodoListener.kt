package org.home.zaval.zavalbackend.listener

import org.home.zaval.zavalbackend.entity.Todo
import org.home.zaval.zavalbackend.util.singleton.todo.TodoStore
import org.home.zaval.zavalbackend.util.toLightDto
import javax.persistence.PostPersist
import javax.persistence.PostRemove
import javax.persistence.PostUpdate

class TodoListener {

    @PostPersist
    fun afterPersist(todo: Todo) {
        TodoStore.saveOrUpdateTodo(todo.toLightDto())
    }

    @PostUpdate
    fun afterUpdate(todo: Todo) {
        TodoStore.saveOrUpdateTodo(todo.toLightDto())
    }

    @PostRemove
    fun afterRemove(todo: Todo) {
        TodoStore.removeTodo(todo.toLightDto())
    }
}