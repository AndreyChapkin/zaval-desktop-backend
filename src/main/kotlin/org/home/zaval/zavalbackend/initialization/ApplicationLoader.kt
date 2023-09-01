package org.home.zaval.zavalbackend.initialization

import org.home.zaval.zavalbackend.dto.CreateTodoDto
import org.home.zaval.zavalbackend.entity.Todo
import org.home.zaval.zavalbackend.entity.value.TodoStatus
import org.home.zaval.zavalbackend.repository.TodoRepository
import org.home.zaval.zavalbackend.service.TodoService
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class ApplicationLoader(
    val todoRepository: TodoRepository,
    val todoService: TodoService,
) : CommandLineRunner {
    override fun run(vararg args: String?) {
        loadConfig()
        loadTodoPersistedValuesAndOptimizationFiles()
        val persistedTodos = loadTodos()
        if (persistedTodos.isNotEmpty()) {
            placeTodosInMainMemory(persistedTodos, todoRepository)
        } else {
            val createdIds = mutableListOf<Long>()
            val maxNumber = 10
            for (i in 0..maxNumber) {
                val parent = when {
                    i == 0 || i == maxNumber / 2 -> null
                    i == 1 || i == (maxNumber / 2) + 1 -> Todo(
                        id = createdIds[i - 1],
                        name = "",
                        status = TodoStatus.BACKLOG,
                    )

                    else -> Todo(
                        id = createdIds[i - 2],
                        name = "",
                        status = TodoStatus.BACKLOG,
                    )
                }
                todoService.createTodo(
                    CreateTodoDto(
                        name = i.toString(),
                        status = TodoStatus.BACKLOG,
                        parentId = parent?.id,
                    )
                ).also { createdIds.add(it.id) }
            }
        }
    }
}