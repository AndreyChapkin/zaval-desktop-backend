package org.home.zaval.zavalbackend.todo

import org.home.zaval.zavalbackend.dto.CreateTodoDto
import org.home.zaval.zavalbackend.entity.value.TodoStatus

fun createInitialTestTodos(): List<CreateTodoDto> {
    return listOf(
        CreateTodoDto(
            name = "First",
            status = TodoStatus.BACKLOG,
        ),
        CreateTodoDto(
            name = "Second",
            status = TodoStatus.BACKLOG,
        ),
        CreateTodoDto(
            name = "Third",
            status = TodoStatus.BACKLOG,
        ),
        CreateTodoDto(
            name = "Fourth",
            status = TodoStatus.BACKLOG,
        ),
        CreateTodoDto(
            name = "Fifth",
            status = TodoStatus.BACKLOG,
        )
    )
}