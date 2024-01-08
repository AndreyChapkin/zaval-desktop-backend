package org.home.zaval.zavalbackend.service

import org.home.zaval.zavalbackend.dto.article.*
import org.home.zaval.zavalbackend.entity.*
import org.home.zaval.zavalbackend.repository.*
import org.home.zaval.zavalbackend.util.asStringFormattedWithISO8601withOffset
import org.home.zaval.zavalbackend.util.asUtc
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import javax.transaction.Transactional

@Service
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class ArticleService(
    val articleRepository: ArticleRepository,
    val seriesRepository: ArticleSeriesRepository,
    val seriesArticleConnectionRepository: SeriesArticleConnectionRepository,
    val labelRepository: ArticleLabelRepository,
    val labelArticleConnectionRepository: LabelArticleConnectionRepository,
    val labelSeriesConnectionRepository: LabelSeriesConnectionRepository,
) {

    fun createArticle(title: String): ArticleLightDto {
        val newArticle = Article(
            id = null,
            title = title,
            content = "",
            interactedOn = OffsetDateTime.now(),
        )
        return articleRepository.save(newArticle).toDto()
    }

    fun getArticleLightsByIds(articleIds: List<Long>): List<ArticleLightDto> {
        return articleRepository.findArticleLightsByIds(articleIds).map { it.toDto() }
    }

    fun getArticleLightById(articleId: Long): ArticleLightDto? {
        return getArticleLightsByIds(listOf(articleId))
            .takeIf { it.isNotEmpty() }
            ?.first()
    }

    fun getTheMostRecentArticleLights(number: Int?): List<ArticleLightDto> {
        return articleRepository.findArticleLightsByPage(
            PageRequest.of(0, number ?: 10, Sort.by(Sort.Order.desc(Article::interactedOn.name)))
        ).content.map { it.toDto() }
    }

    fun findAllArticleLightsWithTitleFragment(titleFragment: String): List<ArticleLightDto> {
        val articleLights = articleRepository.findAllArticleLightsWithTitleFragment("%$titleFragment%")
        return articleLights.map { it.toDto() }
    }

    fun findAllArticleLightsWithAllLabels(labelIds: List<Long>): List<ArticleLightDto> {
        val allLabelsSize = labelIds.size
        val articleIdToLabelIdsMap = labelArticleConnectionRepository.findConnectionsWithLabelIds(labelIds)
            .groupBy({ it.articleId }, { it.labelId })
            .filter { it.value.size >= allLabelsSize }
        return articleRepository.findArticleLightsByIds(articleIdToLabelIdsMap.keys)
            .map { it.toDto() }
    }

    fun findAllArticlesWithLabelNameFragment(nameFragment: String): List<ArticleWithLabelsDto> {
        val articleIdToLabelIds = labelRepository.findLabelsWithNameFragment("%$nameFragment%")
            .mapNotNull { it.id }
            .let { labelArticleConnectionRepository.findConnectionsWithLabelIds(it) }
            .groupBy({ it.articleId }, { it.labelId })

        return articleRepository.findArticleLightsByIds(articleIdToLabelIds.keys)
            .map { light ->
                val labels = articleIdToLabelIds[light.getId()]!!
                    .let { labelRepository.findAllById(it) }
                ArticleWithLabelsDto(
                    article = light.toDto(),
                    labels = labels.map { it.toDto() }
                )
            }
    }

    fun getArticleContent(id: Long?): ArticleContentDto? {
        val content = id?.let { articleRepository.getArticleContentById(it) }
            ?: return null
        articleRepository.updateInteractedOn(id, OffsetDateTime.now())
        return ArticleContentDto(id = id, content = content)
    }

    fun getArticleLabels(id: Long?): List<ArticleLabelDto> {
        return id
            ?.let {
                labelArticleConnectionRepository.findConnectionsWithArticleId(it)
                    .map { conn -> conn.labelId }
                    .let { labelIds -> labelRepository.findAllById(labelIds) }
                    .map { label -> label.toDto() }
            }
            ?: emptyList()
    }

    fun updateArticle(articleId: Long, updateArticleDto: UpdateArticleDto) {
        val updatingArticle = articleRepository.findById(articleId).orElse(null)
            ?: return
        if (updateArticleDto.title != null) {
            updatingArticle.title = updateArticleDto.title
        }
        if (updateArticleDto.content != null) {
            updatingArticle.content = updateArticleDto.content
        }
        updatingArticle.interactedOn = OffsetDateTime.now()
        articleRepository.save(updatingArticle)
    }

    @Transactional
    fun deleteArticle(articleId: Long) {
        articleRepository.deleteById(articleId)
    }

    fun createArticleLabel(articleLabelDto: ArticleLabelDto): ArticleLabelDto {
        val newArticleLabel = articleLabelDto.toEntity()
            .apply { id = null }
        return labelRepository.save(newArticleLabel)
            .toDto()
    }

    fun getArticleLabel(articleLabelId: Long?): ArticleLabelDto? {
        return loadArticleLabel(articleLabelId)
            ?.toDto()
    }

    fun findAllArticleLabelsWithNameFragment(nameFragment: String): List<ArticleLabelDto> {
        return labelRepository.findLabelsWithNameFragment("%$nameFragment%")
            .map { it.toDto() }
    }

    fun updateArticleLabel(articleLabelId: Long, newName: String?) {
        val updatingArticleLabel = loadArticleLabel(articleLabelId)
            ?: return
        if (newName != null) {
            updatingArticleLabel.name = newName
        }
        labelRepository.save(updatingArticleLabel)
    }

    @Transactional
    fun deleteArticleLabel(articleLabelId: Long) {
        labelRepository.deleteById(articleLabelId)
        labelArticleConnectionRepository.deleteAllConnectionsWithLabelId(articleLabelId)
    }

    fun bindLabelsToArticle(labelIds: List<Long>, articleId: Long) {
        val newLabelToArticleConnections = labelIds.map {
            LabelArticleConnection(
                id = null,
                articleId = articleId,
                labelId = it
            )
        }
        labelArticleConnectionRepository.saveAll(newLabelToArticleConnections)
    }

    fun unbindLabelsFromArticle(labelIds: List<Long>, articleId: Long) {
        labelArticleConnectionRepository.deleteAllConnectionOfArticleWithLabels(articleId, labelIds)
    }

    @Transactional
    fun createArticleSeries(createArticleSeriesDto: CreateArticleSeriesDto): ArticleSeriesDto {
        val savedSeries = createArticleSeriesDto
            .toEntity()
            .let { seriesRepository.save(it) }
        val connections = createArticleSeriesDto.articleIds.map {
            SeriesArticleConnection(
                id = null,
                articleId = it,
                seriesId = savedSeries.id!!
            )
        }.let {
            seriesArticleConnectionRepository.saveAll(it)
        }
        return ArticleSeriesDto(
            id = savedSeries.id!!,
            title = savedSeries.title,
            articleIds = connections.map { it.articleId },
            interactedOn = savedSeries.interactedOn.asStringFormattedWithISO8601withOffset(),
        )
    }

    fun getArticleSeries(articleSeriesId: Long?): ArticleSeriesDto? {
        val series = articleSeriesId?.let { seriesRepository.findById(it).orElse(null) }
            ?: return null
        val connections = seriesArticleConnectionRepository.findConnectionsWithSeriesId(series.id!!)
        return ArticleSeriesDto(
            id = series.id!!,
            title = series.title,
            articleIds = connections.map { it.articleId },
            interactedOn = series.interactedOn.asStringFormattedWithISO8601withOffset()
        )
    }

    fun findAllArticleSeriesWithTitleFragment(titleFragment: String): List<ArticleSeriesDto> {
        val articleSeries = seriesRepository.findAllArticleSeriesWithTitleFragment("%$titleFragment%")
        val connections = articleSeries
            .map { it.id!! }
            .let { seriesArticleConnectionRepository.findAllConnectionsWithSeriesIds(it) }
        val seriesIdToConnections = connections.groupBy { it.seriesId }
        return articleSeries.map {
            it to seriesIdToConnections[it.id!!]!!
        }.map { (series, connections) ->
            series.toDto(connections.map { it.articleId })
        }
    }

    fun findAllSeriesWithAllLabels(labelIds: List<Long>): List<SeriesWithLabelsDto> {
        val allLabelsSize = labelIds.size
        val seriesIdToLabelIdsMap = labelSeriesConnectionRepository.findConnectionsWithLabelIds(labelIds)
            .groupBy({ it.seriesId }, { it.labelId })
            .filter { pair -> pair.value.size >= allLabelsSize }
            .toMap()
        val allSeries = seriesIdToLabelIdsMap.keys.let {
            seriesRepository.findAllById(it)
        }
        val seriesIdToArticleIdsMap = allSeries
            .map { it.id!! }
            .let {
                seriesArticleConnectionRepository.findAllConnectionsWithSeriesIds(it)
            }
            .groupBy({ it.seriesId }, { it.articleId })
        val allLabelsMap = seriesIdToLabelIdsMap.values.flatten()
            .let { labelRepository.findAllById(it) }
            .groupBy { it.id!! }
            .mapValues { it.value.first()!! }
        return allSeries.map { series ->
            val labelDtos = seriesIdToLabelIdsMap[series.id]!!
                .map { allLabelsMap[it]!!.toDto() }
            val articleIds = seriesIdToArticleIdsMap[series.id!!]!!
            series.toDtoWithLabels(
                articleIds = articleIds,
                labels = labelDtos
            )
        }
    }

    fun getTheMostRecentArticleSeries(number: Int?): List<ArticleSeriesDto> {
        val series = seriesRepository.findAll(
            PageRequest.of(0, number ?: 10, Sort.by(Sort.Order.desc(ArticleSeries::interactedOn.name)))
        ).content
        val seriesIdToArticleIdsMap = series.map { it.id!! }
            .let { seriesArticleConnectionRepository.findAllConnectionsWithSeriesIds(it) }
            .groupBy({ it.seriesId }, { it.articleId })
        return series.map {
            val articleIds = seriesIdToArticleIdsMap[it.id!!]!!
            it.toDto(articleIds)
        }
    }

    @Transactional
    fun updateArticleSeries(seriesId: Long, updateArticleSeriesDto: UpdateArticleSeriesDto) {
        val series = seriesRepository.findById(seriesId).orElse(null)
            ?: return
        if (updateArticleSeriesDto.title != null) {
            series.title = updateArticleSeriesDto.title
        }
        if (updateArticleSeriesDto.articleIds != null) {
            seriesArticleConnectionRepository.deleteAllConnectionsWithSeriesId(series.id!!)
            updateArticleSeriesDto.articleIds.map {
                SeriesArticleConnection(
                    id = null,
                    articleId = it,
                    seriesId = series.id!!
                )
            }.let { seriesArticleConnectionRepository.saveAll(it) }

        }
        series.interactedOn = OffsetDateTime.now().asUtc
        seriesRepository.save(series)
    }

    fun deleteArticleSeries(seriesId: Long) {
        seriesRepository.deleteById(seriesId)
    }

    private fun loadArticleLabel(articleLabelId: Long?): ArticleLabel? =
        articleLabelId?.let {
            labelRepository.findById(it)
                .orElse(null)
        }
}