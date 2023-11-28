package org.home.zaval.zavalbackend.dto.article

import org.home.zaval.zavalbackend.dto.IdentifiedDto

class UpdateArticleDto(
    val title: String?,
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
    var interactedOn: String,
) : IdentifiedDto(id), ArticleSeriesContent

class ArticleLightStableDto(
    id: Long,
    var title: String,
    var contentTitles: List<ContentTitleDto>,
) : IdentifiedDto(id)

class ArticleVolatileDto(
    id: Long,
    val interactedOn: String
) : IdentifiedDto(id)

class ArticleContentDto(
    id: Long,
    val content: String,
) : IdentifiedDto(id)

class ArticleLabelDto(
    var id: Long?,
    var name: String,
)

class ArticleLightWithLabelsDto(
    var articleLight: ArticleLightDto,
    var labels: List<ArticleLabelDto>
)

class UpdateArticleLabelDto(
    var name: String?,
)

class LabelToArticleConnectionDto(
    id: Long,
    val articleId: Long,
    val labelId: Long,
) : IdentifiedDto(id)

class LabelsCombinationDto(
    id: Long,
    val labelIds: List<Long> = listOf(),
    var popularity: Long
) : IdentifiedDto(id)

interface ArticleSeriesContent

class ArticleSeriesDto(
    id: Long,
    var name: String,
    var articleIds: List<Long> = listOf(),
    var interactedOn: String,
) : IdentifiedDto(id), ArticleSeriesContent

class UpdateArticleSeriesDto(
    val name: String? = null,
    val articleIds: List<Long>? = null,
    val interactedOn: String? = null,
)

class FilledLabelsCombinationDto(
    id: Long,
    val labels: List<ArticleLabelDto> = listOf(),
    val popularity: Long
) : IdentifiedDto(id)
