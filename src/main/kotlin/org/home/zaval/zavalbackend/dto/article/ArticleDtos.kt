package org.home.zaval.zavalbackend.dto.article

import org.home.zaval.zavalbackend.dto.IdentifiedDto
import org.home.zaval.zavalbackend.entity.Article
import org.home.zaval.zavalbackend.entity.projection.ArticleLightProjection
import org.home.zaval.zavalbackend.util.asStringFormattedWithISO8601withOffset
import org.home.zaval.zavalbackend.util.asUtc

class ArticleLightDto(
    id: Long,
    var title: String,
    var interactedOn: String,
) : IdentifiedDto(id)

fun ArticleLightProjection.toDto() = ArticleLightDto(
    id = this.getId(),
    title = this.getTitle(),
    interactedOn = this.getInteractedOn().asUtc.asStringFormattedWithISO8601withOffset(),
)

fun Article.toDto() = ArticleLightDto(
    id = this.id!!,
    title = this.title,
    interactedOn = this.interactedOn.asStringFormattedWithISO8601withOffset()
)

class ArticleContentDto(
    id: Long,
    var content: String,
) : IdentifiedDto(id)

class UpdateArticleDto(
    val title: String?,
    val content: String?
)

class ArticleLabelDto(
    var id: Long?,
    var name: String,
)
