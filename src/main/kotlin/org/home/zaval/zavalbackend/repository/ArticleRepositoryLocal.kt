package org.home.zaval.zavalbackend.repository

import org.home.zaval.zavalbackend.model.Article
import org.home.zaval.zavalbackend.model.ArticleToTodoConnection
import org.home.zaval.zavalbackend.model.Todo
import org.home.zaval.zavalbackend.model.value.TodoStatus
import org.home.zaval.zavalbackend.store.GlobalIdSequence
import org.home.zaval.zavalbackend.store.IdGenerator
import org.home.zaval.zavalbackend.store.silentlyModify
import org.home.zaval.zavalbackend.util.copyAndAppendAll
import org.home.zaval.zavalbackend.util.copyAndRemoveAll
import org.home.zaval.zavalbackend.util.makeCopyWithOverriding
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class ArticleRepositoryLocal : RepositoryLocal(), ArticleRepository, IdGenerator by GlobalIdSequence {

    override val changeListener: (Map<String, Any?>) -> Unit = {
        if (listenToChanges) {
            println("@@@ ${ArticleRepositoryLocal::class.simpleName}: detected changes in store.")
        }
    }

    init {
        localStore.silentlyModify {
            todos = kotlin.run {
                val taskA = Todo(generateId(), name = "Task A", status = TodoStatus.BACKLOG)
                val taskAA = Todo(generateId(), name = "Task A-A", status = TodoStatus.BACKLOG, parentId = taskA.id)
                val taskB = Todo(generateId(), name = "Task B", status = TodoStatus.BACKLOG)
                val taskBA = Todo(generateId(), name = "Task B-A", status = TodoStatus.BACKLOG, parentId = taskB.id)
                mutableListOf(
                    taskA,
                    taskAA,
                    Todo(generateId(), name = "Task A-A-A", status = TodoStatus.BACKLOG, parentId = taskAA.id),
                    Todo(generateId(), name = "Task A-A-B", status = TodoStatus.BACKLOG, parentId = taskAA.id),
                    Todo(generateId(), name = "Task A-B", status = TodoStatus.BACKLOG, parentId = taskA.id),
                    taskB,
                    taskBA,
                    Todo(generateId(), name = "Task B-A-A", status = TodoStatus.BACKLOG, parentId = taskBA.id),
                    Todo(generateId(), name = "Task B-A-B", status = TodoStatus.BACKLOG, parentId = taskBA.id),
                    Todo(generateId(), name = "Task B-B", status = TodoStatus.BACKLOG, parentId = taskB.id),
                )
            }
        }
    }

    override fun getArticle(articleId: Long): Article? {
        return localStore.articles.find { it.id == articleId }
    }

    override fun createArticles(articles: List<Article>): List<Article> {
        val resultArticles = mutableListOf<Article>()
        for (article in articles) {
            val result = article.makeCopyWithOverriding {
                fill(Article::id).withValue(generateId())
            }
            resultArticles.add(result)
        }
        localStore.articles = localStore.articles.copyAndAppendAll(resultArticles)
        return resultArticles
    }

    override fun updateArticles(articles: List<Article>) {
        val resultArticles = mutableListOf<Article>()
        for (article in localStore.articles) {
            var resultArticle = article
            val indexOfUpdatedArticle = articles.indexOf(article)
            if (indexOfUpdatedArticle > -1) {
                resultArticle = articles[indexOfUpdatedArticle]
            }
            resultArticles.add(resultArticle)
        }
        localStore.articles = resultArticles
    }

    override fun deleteArticles(articleIds: List<Long>) {
        localStore.articles = localStore.articles.copyAndRemoveAll { articleIds.contains(it.id) }
    }

    override fun getAllArticles(): List<Article> {
        return localStore.articles
    }

    override fun createArticleToTodoConnection(
        articleToTodoConnections: List<ArticleToTodoConnection>
    ): List<ArticleToTodoConnection> {
        val resultConnections = mutableListOf<ArticleToTodoConnection>()
        for (connection in articleToTodoConnections) {
            val resultConnection = connection.makeCopyWithOverriding {
                fill(ArticleToTodoConnection::id).withValue(generateId())
            }
            resultConnections.add(resultConnection)
        }
        localStore.articlesToTodosConnections = localStore.articlesToTodosConnections
            .copyAndAppendAll(resultConnections)
        return resultConnections
    }

    override fun isExist(articleId: Long): Boolean {
        return localStore.articles.indexOfFirst { it.id == articleId } > 0
    }
}
