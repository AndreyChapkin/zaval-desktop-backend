package org.home.zaval.zavalbackend.initialization

import org.home.zaval.zavalbackend.dto.article.ArticleLightDto
import org.home.zaval.zavalbackend.dto.todo.CreateTodoDto
import org.home.zaval.zavalbackend.entity.Todo
import org.home.zaval.zavalbackend.entity.value.TodoStatus
import org.home.zaval.zavalbackend.repository.ArticleRepository
import org.home.zaval.zavalbackend.repository.TodoRepository
import org.home.zaval.zavalbackend.service.ArticleService
import org.home.zaval.zavalbackend.service.TodoService
import org.home.zaval.zavalbackend.store.TodoStore
import org.home.zaval.zavalbackend.util.toEntity
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class ApplicationLoader(
    val todoRepository: TodoRepository,
    val todoService: TodoService,
    val articleService: ArticleService,
    val articleRepository: ArticleRepository,
) : CommandLineRunner {
    override fun run(vararg args: String?) {
        loadConfig()
        reserveCurrentData()
        loadTodoTechnicalFiles()
        // Todos
        val persistedTodos = loadTodos()
        if (persistedTodos.isNotEmpty()) {
            TodoStore.active = false
            placeTodosInMainMemory(persistedTodos, todoRepository)
            TodoStore.active = true
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
        // Histories
        loadTodoHistoryTechnicalFiles()
        val persistedHistories = loadTodoHistories()
        if (persistedTodos.isNotEmpty()) {
            TodoStore.active = false
            placeTodosHistoriesInMemory(persistedHistories, todoService)
            TodoStore.active = true
        }
        // Articles
        val persistedArticleLights = loadArticleLights()
        if (persistedArticleLights.isNotEmpty()) {
            persistedArticleLights.forEach {articleLight ->
                articleRepository.save(articleLight.toEntity())
            }
        } else {
            articleService.createArticle(ArticleLightDto(
                id = -100,
                title = "First example",
                contentTitles = emptyList(),
                popularity = 0L,
            ))
            articleService.createArticle(ArticleLightDto(
                id = -100,
                title = "Second example",
                contentTitles = emptyList(),
                popularity = 0L,
            ))
        }
    }
}