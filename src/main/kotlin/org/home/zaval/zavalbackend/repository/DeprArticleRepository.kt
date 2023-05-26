package org.home.zaval.zavalbackend.repository

import org.home.zaval.zavalbackend.model.Article
import org.home.zaval.zavalbackend.model.ArticleToTodoConnection

interface DeprArticleRepository {
    fun getArticle(articleId: Long): Article?

    fun createArticles(articles: List<Article>): List<Article>

    fun updateArticles(article: List<Article>)

    fun deleteArticles(articleId: List<Long>)

    fun getAllArticles(): List<Article>

    fun isExist(articleId: Long): Boolean

    fun createArticleToTodoConnection(
        articleToTodoConnections: List<ArticleToTodoConnection>
    ): List<ArticleToTodoConnection>

    // TODO bad decision for batching
    fun batched(modifier: () -> Unit)
}