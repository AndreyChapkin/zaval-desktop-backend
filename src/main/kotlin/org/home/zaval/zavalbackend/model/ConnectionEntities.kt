package org.home.zaval.zavalbackend.model

class ArticleToTodoConnection(
    id: Long,
    val articleId: Long,
    val todoId: Long,
) : BaseEntity(id)