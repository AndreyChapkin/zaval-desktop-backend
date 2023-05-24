package org.home.zaval.zavalbackend.model.view

import org.home.zaval.zavalbackend.model.Todo
import org.home.zaval.zavalbackend.model.value.TodoStatus

data class TodoHierarchy(
    val id: Long,
    val name: String,
    val status: TodoStatus,
    var parent: TodoHierarchy? = null,
    var children: Array<TodoHierarchy>? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Todo

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}