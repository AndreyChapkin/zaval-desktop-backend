package org.home.zaval.zavalbackend.util

import org.home.zaval.zavalbackend.dto.article.ArticleDto
import org.home.zaval.zavalbackend.dto.article.ArticleLightDto
import org.home.zaval.zavalbackend.dto.todo.LightTodoDto
import org.home.zaval.zavalbackend.entity.Article
import org.home.zaval.zavalbackend.entity.ArticleDirectory
import org.home.zaval.zavalbackend.entity.ArticleLightView
import org.home.zaval.zavalbackend.entity.Todo
import org.home.zaval.zavalbackend.entity.value.TodoStatus

val ARTICLE_DIRECTORY_ROOT: ArticleDirectory = ArticleDirectory(
    id = -1000,
    name = "Root",
)

// TODO get rid of
fun ArticleLightView.toLightDto() = ArticleLightDto(
    id = this.getId() ?: -100,
    title = this.getTitle(),
    directoryId = this.getDirectoryId(),
)

fun Article.toDto() = ArticleDto(
    id = this.id!!,
    title = this.title,
    content = this.content,
    directoryId = this.directory?.id,
)

fun Article.toLightDto() = ArticleLightDto(
    id = this.id!!,
    title = this.title,
    directoryId = this.directory?.id,
)

fun ArticleDto.toEntity() = Article(
    id = this.id,
    title = this.title,
    content = this.content,
    directory = ArticleDirectory(
        id = this.directoryId,
        name = "",
    )
)
