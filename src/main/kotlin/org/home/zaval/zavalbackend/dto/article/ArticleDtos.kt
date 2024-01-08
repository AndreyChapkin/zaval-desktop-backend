package org.home.zaval.zavalbackend.dto.article

import org.home.zaval.zavalbackend.dto.IdentifiedDto
import org.home.zaval.zavalbackend.entity.Article
import org.home.zaval.zavalbackend.entity.ArticleLabel
import org.home.zaval.zavalbackend.entity.ArticleSeries
import org.home.zaval.zavalbackend.entity.projection.ArticleLightProjection
import org.home.zaval.zavalbackend.util.asStringFormattedWithISO8601withOffset
import org.home.zaval.zavalbackend.util.asUtc
import java.time.OffsetDateTime


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

fun ArticleLabelDto.toEntity() = ArticleLabel(
    id = this.id,
    name = this.name
)

fun ArticleLabel.toDto() = ArticleLabelDto(
    id = this.id,
    name = this.name
)

class ArticleWithLabelsDto(
    val article: ArticleLightDto,
    val labels: List<ArticleLabelDto>,
)

class LabelArticleConnectionDto(
    id: Long,
    val articleId: Long,
    val labelId: Long,
) : IdentifiedDto(id)

class ArticleSeriesDto(
    id: Long,
    var title: String,
    var articleIds: List<Long> = listOf(),
    var interactedOn: String,
) : IdentifiedDto(id)

fun ArticleSeries.toDto(articleIds: List<Long>) = ArticleSeriesDto(
    id = this.id!!,
    title = this.title,
    articleIds = articleIds,
    interactedOn = this.interactedOn.asStringFormattedWithISO8601withOffset()
)

class SeriesWithLabelsDto(
    id: Long,
    var title: String,
    var articleIds: List<Long> = listOf(),
    var interactedOn: String,
    val labels: List<ArticleLabelDto>,
) : IdentifiedDto(id)

fun ArticleSeries.toDtoWithLabels(articleIds: List<Long>, labels: List<ArticleLabelDto>) = SeriesWithLabelsDto(
    id = this.id!!,
    title = this.title,
    articleIds = articleIds,
    interactedOn = this.interactedOn.asStringFormattedWithISO8601withOffset(),
    labels = labels
)

class CreateArticleSeriesDto(
    val title: String,
    val articleIds: List<Long> = listOf(),
)

fun CreateArticleSeriesDto.toEntity() = ArticleSeries(
    id = null,
    title = this.title,
    interactedOn = OffsetDateTime.now().asUtc,
)

class UpdateArticleSeriesDto(
    val title: String? = null,
    val articleIds: List<Long>? = null,
    val interactedOn: String? = null,
)

class FilledLabelsCombinationDto(
    id: Long,
    val labels: List<ArticleLabelDto> = listOf(),
    val popularity: Long
) : IdentifiedDto(id)
