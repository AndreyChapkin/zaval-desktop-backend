package org.home.zaval.zavalbackend.initialization

import org.home.zaval.zavalbackend.repository.ArticleLabelRepository
import org.home.zaval.zavalbackend.repository.ArticleRepository
import org.home.zaval.zavalbackend.repository.LabelArticleConnectionRepository
import org.home.zaval.zavalbackend.repository.TodoRepository
import org.home.zaval.zavalbackend.service.ArticleService
import org.home.zaval.zavalbackend.service.TodoService
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class ApplicationLoader(
    val todoRepository: TodoRepository,
    val todoService: TodoService,
    val articleService: ArticleService,
    val articleRepository: ArticleRepository,
    val articleLabelRepository: ArticleLabelRepository,
    val labelToArticleConnectionRepository: LabelArticleConnectionRepository,
) : CommandLineRunner {
    override fun run(vararg args: String?) {
//        loadConfig()
//        reserveCurrentData()
//        loadTodoTechnicalFiles()
//        // Todos
//        val persistedTodos = loadTodos()
//        if (persistedTodos.isNotEmpty()) {
//            TodoStore.active = false
//            placeTodosInMainMemory(persistedTodos, todoRepository)
//            TodoStore.active = true
//        }
//        // Histories
//        loadTodoHistoryTechnicalFiles()
//        val persistedHistories = loadTodoHistories()
//        if (persistedTodos.isNotEmpty()) {
//            TodoStore.active = false
//            placeTodosHistoriesInMemory(persistedHistories, todoService)
//            TodoStore.active = true
//        }
//        // Articles
//        val persistedArticleLights = loadArticlesData()
//        if (persistedArticleLights.isNotEmpty()) {
//            persistedArticleLights.forEach {articleLight ->
//                articleRepository.save(articleLight.toEntity())
//            }
//        }
//        // Labels
//        val persistedArticleLabels = loadArticleLabels()
//        persistedArticleLabels.forEach {
//            articleLabelRepository.save(it.toEntity())
//        }
//        // Label to article connections
//        val persistedConnections = loadLabelToArticleConnections()
//        persistedConnections.forEach {
//            labelToArticleConnectionRepository.save(it.toEntity())
//        }
//        // Label combinations
//        loadArticleLabelsCombinations()
//        // Article series
//        loadArticleSeriesData()
    }
}