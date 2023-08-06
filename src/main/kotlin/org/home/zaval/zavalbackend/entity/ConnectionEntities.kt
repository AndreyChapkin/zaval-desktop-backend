package org.home.zaval.zavalbackend.entity

class ArticleToTodoConnection(
    id: Long,
    val articleId: Long,
    val todoId: Long,
) : BaseEntity(id)