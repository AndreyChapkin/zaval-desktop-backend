package org.home.zaval.zavalbackend.listener

import org.home.zaval.zavalbackend.entity.Todo
import org.home.zaval.zavalbackend.entity.TodoHistory
import org.home.zaval.zavalbackend.store.TodoStore
import org.home.zaval.zavalbackend.util.toFullDto
import org.home.zaval.zavalbackend.util.toLightDto
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
        TodoStore.saveOrUpdateTodo(todo.toFullDto())
    }

    @PostRemove
    fun afterRemove(todo: Todo) {
        TodoStore.removeTodo(todo.toFullDto())
    }
}

class TodoHistoryListener {

    @PostPersist
    fun afterPersist(history: TodoHistory) {
        TodoStore.saveOrUpdateHistory(history.toLightDto())
    }

    @PostUpdate
    fun afterUpdate(history: TodoHistory) {
        TodoStore.saveOrUpdateHistory(history.toLightDto())
    }

    @PostRemove
    fun afterRemove(history: TodoHistory) {
        TodoStore.removeHistory(history.toLightDto())
    }
}