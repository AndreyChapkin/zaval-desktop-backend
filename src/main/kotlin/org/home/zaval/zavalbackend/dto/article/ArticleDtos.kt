package org.home.zaval.zavalbackend.dto.article

import org.home.zaval.zavalbackend.dto.IdentifiedDto

class UpdateArticleDto(
    val title: String?,
    val popularity: Long?,
    val content: String?
)

class ContentTitleDto(
    val level: Int,
    val title: String,
    val id: String,
)

class ArticleLightDto(
    id: Long,
    var title: String,
    var contentTitles: List<ContentTitleDto>,
    var popularity: Long,
) : IdentifiedDto(id)

class ArticleLightStableDto(
    id: Long,
    var title: String,
    var contentTitles: List<ContentTitleDto>,
) : IdentifiedDto(id)

class ArticleContentDto(
    id: Long,
    val content: String,
) : IdentifiedDto(id)

class ArticleLabelDto(
    var id: Long?,
    var name: String,
    var popularity: Long,
)

class LabelToArticleConnectionDto(
    id: Long,
    val articleId: Long,
    val labelId: Long,
) : IdentifiedDto(id)
