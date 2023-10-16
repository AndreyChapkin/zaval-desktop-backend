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
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

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

    fun getTheMostPopularArticleLights(number: Int?): List<ArticleLightDto> {
        return articleRepository.getTheMostPopularArticles(number ?: 10).map { it.toLightDto() }
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

    fun createArticle(articleDto: ArticleLightDto): ArticleLightDto {
        val newArticle = articleDto.toEntity().apply {
            id = ArticleStore.getId()
            interactedOn = OffsetDateTime.now()
        }
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

    fun findAllArticleLightsByTitleFragment(titleFragment: String): List<ArticleLightDto> {
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
        return articleLabelRepository.findAll().map { it.toDto() }
    }

    fun getArticleLabel(articleLabelId: Long?): ArticleLabelDto? {
        return loadArticleLabel(articleLabelId)?.toDto()
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

    fun deleteArticleLabel(articleLabelId: Long) {
        articleRepository.deleteById(articleLabelId)
        labelToArticleConnectionRepository.deleteAllConnectionsWithLabelId(articleLabelId)
        ArticleStore.apply {
            removeArticleLabel(articleLabelId)
            removeLabelToArticleConnection(articleLabelId)
        }
    }

    fun bindLabelToArticle(labelId: Long, articleId: Long) {
        val newLabelToArticleConnection = LabelToArticleConnection(
            id = ArticleStore.getId(),
            articleId = articleId,
            labelId = labelId
        )
        labelToArticleConnectionRepository.save(newLabelToArticleConnection)
        ArticleStore.saveLabelToArticleConnection(newLabelToArticleConnection.toDto())
    }

    fun unbindLabelFromArticle(labelId: Long, articleId: Long) {
        val connection = labelToArticleConnectionRepository.findConnectionWithLabelAndArticleIds(
            labelId = labelId,
            articleId = articleId
        )
        if (connection != null) {
            labelToArticleConnectionRepository.deleteById(connection.id)
            ArticleStore.removeLabelToArticleConnection(connection.id)
        }
    }

    fun createLabelsCombination(labelIds: List<Long>): LabelsCombinationDto {
        val newLabelsCombinationDto = LabelsCombinationDto(
            id = ArticleStore.getId(),
            labelIds = labelIds,
            popularity = 0,
        )
        ArticleStore.saveLabelsCombination(newLabelsCombinationDto)
        return newLabelsCombinationDto
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

    private fun loadArticleLabel(articleLabelId: Long?): ArticleLabel? =
        articleLabelId?.let { articleLabelRepository.findById(it).orElse(null) }
}