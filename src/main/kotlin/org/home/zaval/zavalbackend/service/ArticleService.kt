package org.home.zaval.zavalbackend.service

import org.home.zaval.zavalbackend.dto.article.*
import org.home.zaval.zavalbackend.entity.Article
import org.home.zaval.zavalbackend.entity.ArticleLabel
import org.home.zaval.zavalbackend.entity.LabelToArticleConnection
import org.home.zaval.zavalbackend.repository.ArticleLabelRepository
import org.home.zaval.zavalbackend.repository.ArticleRepository
import org.home.zaval.zavalbackend.repository.LabelToArticleConnectionRepository
import org.home.zaval.zavalbackend.store.ArticleStore
import org.home.zaval.zavalbackend.util.*
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
    val articleLabelRepository: ArticleLabelRepository,
    val labelToArticleConnectionRepository: LabelToArticleConnectionRepository,
) {

    fun getAllArticleLights(): List<ArticleLightDto> {
        return articleRepository.findAll().map { it.toLightDto() }
    }

    fun getArticleLight(articleId: Long?): ArticleLightDto? {
        val article = loadArticle(articleId)
        if (article != null) {
            return article.toLightDto()
        }
        return null
    }

    fun getTheMostRecentArticleLights(number: Int?): List<ArticleLightDto> {
        return articleRepository.findAll(
            PageRequest.of(0, number ?: 10, Sort.by(Sort.Order.desc(Article::interactedOn.name)))
        ).content.map { it.toLightDto() }
    }

    fun getArticleContent(articleId: Long?): ArticleContentDto? {
        val article = loadArticle(articleId)
        if (article != null) {
            article.interactedOn = OffsetDateTime.now()
            articleRepository.save(article)
            ArticleStore.updateArticleInteractedOn(
                articleId!!,
                article.interactedOn.asStringFormattedWithISO8601withOffset()
            )
            return ArticleStore.actualArticleContentsContent.readEntity(articleId!!)
        }
        return null
    }

    fun getArticleLabels(articleId: Long?): List<ArticleLabelDto> {
        val articleExists = if (articleId != null) articleRepository.existsById(articleId) else false
        if (articleExists) {
            val connections = labelToArticleConnectionRepository.findConnectionsWithArticleId(articleId!!)
            val labelIds = connections.map { it.labelId }
            val labels = articleLabelRepository.findAllById(labelIds)
            return labels.map { it.toDto() }
        }
        return emptyList()
    }

    fun createArticle(title: String): ArticleLightDto {
        val newArticle = Article(
            id = ArticleStore.getId(),
            interactedOn = OffsetDateTime.now(),
            title = title,
            contentTitles = "",
        )
        val savedArticleLightDto = articleRepository.save(newArticle).toLightDto()
        val contentDto = savedArticleLightDto.toContentDto("")
        ArticleStore.apply {
            saveArticleLight(savedArticleLightDto)
            saveArticleContent(contentDto)
        }
        return savedArticleLightDto
    }

    fun updateArticle(articleId: Long, updateArticleDto: UpdateArticleDto) {
        val updatingArticle = loadArticle(articleId)
        var updateLight = false
        if (updatingArticle != null) {
            if (updateArticleDto.title != null) {
                updatingArticle.title = updateArticleDto.title
                updateLight = true
            }
            if (updateArticleDto.content != null) {
                val articleContentDto = updatingArticle.toContentDto(updateArticleDto.content)
                updatingArticle.contentTitles = extractContentTitles(articleContentDto.content).asString()
                updateLight = true
                ArticleStore.updateArticleContent(articleContentDto)
            }
            if (updateLight) {
                ArticleStore.updateArticleLight(updatingArticle.toLightDto())
            }
            updatingArticle.interactedOn = OffsetDateTime.now()
            articleRepository.save(updatingArticle)
            ArticleStore.updateArticleInteractedOn(
                articleId,
                updatingArticle.interactedOn.asStringFormattedWithISO8601withOffset()
            )
        }
    }

    @Transactional
    fun deleteArticle(articleId: Long) {
        articleRepository.deleteById(articleId)
        val connectionIds = labelToArticleConnectionRepository.findConnectionsWithArticleId(articleId)
        labelToArticleConnectionRepository.deleteAllConnectionsWithArticleId(articleId)
        ArticleStore.apply {
            removeArticleLight(articleId)
            removeArticleContent(articleId)
            connectionIds.forEach { removeLabelToArticleConnection(it.id) }
        }
    }

    fun findAllArticleLightsWithTitleFragment(titleFragment: String): List<ArticleLightDto> {
        val articles = articleRepository.findAllArticlesWithTitleFragment("%$titleFragment%")
        return articles.map { it.toLightDto() }
    }

    fun findAllArticleLightsWithAllLabels(labelIds: List<Long>): List<ArticleLightDto> {
        val labelToArticleConnections = labelToArticleConnectionRepository.findConnectionsWithLabelIds(labelIds)
        val articleIdsToLabelIds = mutableMapOf<Long, MutableSet<Long>>()
        for (connection in labelToArticleConnections) {
            val labelIdsSet = articleIdsToLabelIds.computeIfAbsent(connection.articleId) { mutableSetOf() }
            labelIdsSet.add(connection.labelId)
        }
        val allLabelsSize = labelIds.size
        val allLabelArticleIds = articleIdsToLabelIds.entries
            .filter { it.value.size == allLabelsSize }
            .map { it.key }
        return articleRepository.findAllById(allLabelArticleIds)
            .map { it.toLightDto() }
    }

    fun findAllArticlesWithLabelNameFragment(nameFragment: String): List<ArticleLightWithLabelsDto> {
        val labels = articleLabelRepository.findLabelsWithNameFragment("%$nameFragment%")
        val labelConnections = labelToArticleConnectionRepository.findConnectionsWithLabelIds(
            labels.map { it.id!! }
        )
        val articles = articleRepository.findAllById(
            labelConnections.map { it.articleId }
        )
        val result = mutableListOf<ArticleLightWithLabelsDto>()
        articles.forEach {
            val articleConnections = labelToArticleConnectionRepository.findConnectionsWithArticleId(it.id!!)
            val articleConnectedLabels = articleLabelRepository.findAllById(
                articleConnections.map { it.labelId }
            )
            result.add(
                ArticleLightWithLabelsDto(
                    articleLight = it.toLightDto(),
                    labels = articleConnectedLabels.map { it.toDto() }
                )
            )
        }
        return result
    }

    private fun loadArticle(articleId: Long?): Article? = articleId?.let { articleRepository.findById(it).orElse(null) }

    fun createArticleLabel(articleLabelDto: ArticleLabelDto): ArticleLabelDto {
        val newArticleLabel = articleLabelDto.toEntity().apply {
            id = ArticleStore.getId()
        }
        val savedArticleLabelDto = articleLabelRepository.save(newArticleLabel).toDto()
        ArticleStore.apply {
            saveArticleLabel(savedArticleLabelDto)
        }
        return savedArticleLabelDto
    }

    fun getAllArticleLabels(): List<ArticleLabelDto> {
        return articleLabelRepository.findAll().map { it.toDto() }.sortedBy { it.name }
    }

    fun getArticleLabel(articleLabelId: Long?): ArticleLabelDto? {
        return loadArticleLabel(articleLabelId)?.toDto()
    }

    fun findAllArticleLabelsWithNameFragment(nameFragment: String): List<ArticleLabelDto> {
        val labels = articleLabelRepository.findLabelsWithNameFragment("%$nameFragment%")
        return labels.map { it.toDto() }
    }

    fun updateArticleLabel(articleLabelId: Long, updateArticleLabelDto: UpdateArticleLabelDto) {
        val updatingArticleLabel = loadArticleLabel(articleLabelId)
        if (updatingArticleLabel != null) {
            if (updateArticleLabelDto.name != null) {
                updatingArticleLabel.name = updateArticleLabelDto.name!!
            }
            ArticleStore.updateArticleLabel(updatingArticleLabel.toDto())
            articleLabelRepository.save(updatingArticleLabel)
        }
    }

    @Transactional
    fun deleteArticleLabel(articleLabelId: Long) {
        articleLabelRepository.deleteById(articleLabelId)
        labelToArticleConnectionRepository.deleteAllConnectionsWithLabelId(articleLabelId)
        ArticleStore.apply {
            removeArticleLabel(articleLabelId)
            removeLabelToArticleConnection(articleLabelId)
        }
        val labelsCombinations = ArticleStore.readAllLabelCombinationsWithLabelId(articleLabelId)
        labelsCombinations.forEach {
            ArticleStore.removeLabelsCombination(it.id)
        }
    }

    fun bindLabelsToArticle(labelIds: List<Long>, articleId: Long) {
        val newLabelToArticleConnections = labelIds.map {
            LabelToArticleConnection(
                id = ArticleStore.getId(),
                articleId = articleId,
                labelId = it
            )
        }
        labelToArticleConnectionRepository.saveAll(newLabelToArticleConnections)
        newLabelToArticleConnections.forEach {
            ArticleStore.saveLabelToArticleConnection(it.toDto())
        }
    }

    fun unbindLabelsFromArticle(labelIds: List<Long>, articleId: Long) {
        val connections = labelToArticleConnectionRepository.findConnectionsWithArticleId(articleId)
        val labelToConnectionIdIndex = connections.associate { it.labelId to it.id }
        val connectedLabelIdsSet = connections.map { it.labelId }.toSet()
        labelIds.forEach { labelId ->
            if (connectedLabelIdsSet.contains(labelId)) {
                val connectionId = labelToConnectionIdIndex[labelId]
                labelToArticleConnectionRepository.deleteById(connectionId!!)
                ArticleStore.removeLabelToArticleConnection(connectionId!!)
            }
        }
    }

    fun createLabelsCombination(labelIds: List<Long>): LabelsCombinationDto {
        val newLabelsCombinationDto = LabelsCombinationDto(
            id = ArticleStore.getId(),
            labelIds = labelIds,
            popularity = 1,
        )
        ArticleStore.saveLabelsCombination(newLabelsCombinationDto)
        return newLabelsCombinationDto
    }

    fun getTheMostPopularLabelsCombinations(number: Int?): List<FilledLabelsCombinationDto> {
        val combinations = ArticleStore.labelCombinationsInMemory.values
            .sortedByDescending { it.popularity }
            .take(number ?: 10)
        val allLabelIds = combinations.flatMap { it.labelIds }.toSet()
        val allLabels = articleLabelRepository.findAllById(allLabelIds)
        val labelsIndex = mutableMapOf<Long, ArticleLabel>().apply {
            allLabels.forEach { this[it.id!!] = it }
        }
        val resultCombinationDtos = combinations.map { combination ->
            val labelDtos = combination.labelIds.map { labelsIndex[it]!!.toDto() }
            combination.toFilledDto(labelDtos)
        }
        return resultCombinationDtos
    }

    fun updateLabelsCombinationPopularity(combinationId: Long, popularity: Long) {
        val combination = ArticleStore.labelCombinationsInMemory[combinationId]
        if (combination != null) {
            combination.popularity = popularity
            ArticleStore.saveLabelsCombination(combination)
        }
    }

    fun deleteLabelsCombination(combinationId: Long) {
        ArticleStore.removeLabelsCombination(combinationId)
    }

    fun createArticleSeries(articleSeriesDto: ArticleSeriesDto): ArticleSeriesDto {
        val newArticleSeriesDto = ArticleSeriesDto(
            id = ArticleStore.getId(),
            name = articleSeriesDto.name,
            articleIds = articleSeriesDto.articleIds,
            interactedOn = OffsetDateTime.now().asStringFormattedWithISO8601withOffset(),
        )
        ArticleStore.saveArticleSeries(newArticleSeriesDto)
        return newArticleSeriesDto
    }

    fun getArticleSeries(articleSeriesId: Long?): ArticleSeriesDto? {
        if (articleSeriesId == null) {
            return null
        }
        return ArticleStore.findArticleSeriesById(articleSeriesId)
    }

    fun getTheMostRecentArticleSeries(number: Int?): List<ArticleSeriesDto> {
        return ArticleStore.getAllArticleSeries().sortedByDescending {
            it.interactedOn.asOffsetDateTimeFromISO8601WithOffset()
        }.take(number ?: 10)
    }

    fun updateArticleSeries(articleSeriesId: Long, updateArticleSeriesDto: UpdateArticleSeriesDto) {
        ArticleStore.updateArticleSeries(articleSeriesId, updateArticleSeriesDto)
    }

    fun deleteArticleSeries(articleSeriesId: Long) {
        ArticleStore.removeArticleSeriesDto(articleSeriesId)
    }

    private fun loadArticleLabel(articleLabelId: Long?): ArticleLabel? =
        articleLabelId?.let { articleLabelRepository.findById(it).orElse(null) }
}