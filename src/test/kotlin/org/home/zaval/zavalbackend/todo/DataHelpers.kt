package org.home.zaval.zavalbackend.todo

import org.home.zaval.zavalbackend.dto.todo.TodoCreateDto
import org.home.zaval.zavalbackend.entity.value.TodoStatus

fun createInitialTestTodos(): List<TodoCreateDto> {
    return listOf(
        TodoCreateDto(
            name = "First",
            status = TodoStatus.BACKLOG,
        ),
        TodoCreateDto(
            name = "Second",
            status = TodoStatus.BACKLOG,
        ),
        TodoCreateDto(
            name = "Third",
            status = TodoStatus.BACKLOG,
        ),
        TodoCreateDto(
            name = "Fourth",
            status = TodoStatus.BACKLOG,
        ),
        TodoCreateDto(
            name = "Fifth",
            status = TodoStatus.BACKLOG,
        )
    )
}