package org.home.zaval.zavalbackend.dto

import org.home.zaval.zavalbackend.model.Todo
import org.home.zaval.zavalbackend.model.value.TodoStatus

data class MoveTodoDto(
    val todoId: Long,
    val parentId: Long?,
)

data class TodoDto(
    val id: Long,
    val name: String,
    val status: TodoStatus,
    val parentId: Long?,
)

data class CreateTodoDto(
    val name: String,
    val status: TodoStatus,
    val parentId: Long?,
)

data class UpdateTodoDto(
    val name: String,
    val status: TodoStatus,
)

data class TodoHierarchyDto(
    val id: Long,
    val name: String,
    val status: TodoStatus,
    var parent: TodoHierarchyDto? = null,
    var children: Array<TodoHierarchyDto>? = null
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