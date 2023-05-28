package org.home.zaval.zavalbackend.service

import org.home.zaval.zavalbackend.repository.TodoRepository

//@Service
//@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class ArticleService(
    val todoRepository: TodoRepository,
) {

//    fun createArticles(articles: List<Article>): List<Article> {
//        return articleRepository.createArticles(articles)
//    }
//
//    fun getArticle(articleId: Long?): Article? {
//        return articleId?.let { articleRepository.getArticle(it) }
//    }
//
//    fun updateArticle(articleId: Long, article: Article, command: ArticleChangeCommand): Article? {
//        val updatingArticle = getArticle(articleId)
//        if (updatingArticle != null) {
//            val resultArticle = updatingArticle.makeCopyWithOverriding {
//                when (command) {
//                    ArticleChangeCommand.NAME -> {
//                        fill(Article::name).withValue(article.name)
//                    }
//
//                    ArticleChangeCommand.TEXT -> {
//                        fill(Article::text).withValue(article.text)
//                    }
//
//                    else -> {
//                        fill(Article::name).withValue(article.name)
//                        fill(Article::text).withValue(article.text)
//                    }
//                }
//            }
//            articleRepository.updateArticles(listOf(resultArticle))
//            return resultArticle
//        }
//        return null
//    }
//
//    fun deleteArticles(articleIds: List<Long>) {
//        articleRepository.deleteArticles(articleIds)
//    }
//
//    fun createArticleToTodoConnections(connections: List<ArticleToTodoConnection>): List<ArticleToTodoConnection> {
//        val resultConnections = connections.filter {
//            todoRepository.isExist(it.todoId) && articleRepository.isExist(it.articleId)
//        }
//        return articleRepository.createArticleToTodoConnection(resultConnections)
//    }
//
//    fun getAllArticlesWithoutTexts(): List<Article> {
//        return articleRepository.getAllArticles().map {
//            it.makeCopyWithOverriding {
//                fill(Article::text).withValue("")
//            }
//        }
//    }
}