package org.home.zaval.zavalbackend.model

import org.home.zaval.zavalbackend.model.value.TodoStatus

class Todo(
    id: Long,
    val name: String,
    val status: TodoStatus,
    val parentId: Long? = null,
) : BaseEntity(id)

class TodoHistory(
    id: Long,
    val actions: List<String>,
    val todoId: Long,
) : BaseEntity(id)