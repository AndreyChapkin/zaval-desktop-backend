package org.home.zaval.zavalbackend.service

import org.home.zaval.zavalbackend.dto.article.ArticleContentDto
import org.home.zaval.zavalbackend.dto.article.ArticleLightDto
import org.home.zaval.zavalbackend.dto.article.UpdateArticleDto
import org.home.zaval.zavalbackend.entity.Article
import org.home.zaval.zavalbackend.repository.ArticleLabelRepository
import org.home.zaval.zavalbackend.repository.ArticleRepository
import org.home.zaval.zavalbackend.repository.LabelToArticleConnectionRepository
import org.home.zaval.zavalbackend.store.ArticleStore
import org.home.zaval.zavalbackend.util.*
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Service

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
        return loadArticle(articleId)?.toLightDto()
    }

    fun getArticleContent(articleId: Long?): ArticleContentDto? {
        if (articleId != null && articleRepository.existsById(articleId)) {
            return ArticleStore.actualArticleContentsContent.readEntity(articleId)
        }
        return null
    }

    fun createArticle(articleDto: ArticleLightDto): ArticleLightDto {
        val newArticle = articleDto.toEntity().apply {
            id = ArticleStore.getId()
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
        if (updatingArticle != null) {
            if (updateArticleDto.title != null) {
                updatingArticle.title = updateArticleDto.title
            }
            if (updateArticleDto.popularity != null) {
                updatingArticle.popularity = updateArticleDto.popularity
            }
            if (updateArticleDto.content != null) {
                val articleContentDto = updatingArticle.toContentDto(updateArticleDto.content)
                updatingArticle.contentTitles = extractContentTitles(articleContentDto.content).asString()
                ArticleStore.updateArticleContent(articleContentDto)
            }
            ArticleStore.updateArticleLight(updatingArticle.toLightDto())
            articleRepository.save(updatingArticle)
        }
    }

    fun updateArticlePopularity(articleId: Long, popularity: Long) {
        if (articleRepository.existsById(articleId)) {
            articleRepository.updatePopularity(articleId, popularity)
            ArticleStore.updateArticlePopularity(articleId, popularity)
        }
    }

    fun deleteArticle(articleId: Long) {
        articleRepository.deleteById(articleId)
        ArticleStore.apply {
            removeArticleLight(articleId)
            removeArticleContent(articleId)
        }
    }

    fun findAllArticleLightsByTitleFragment(titleFragment: String): List<ArticleLightDto> {
        val articles = articleRepository.findAllLightArticlesWithTitleFragment("%$titleFragment%")
        return articles.map { it.toLightDto() }
    }

    private fun loadArticle(articleId: Long?): Article? = articleId?.let { articleRepository.findById(it).orElse(null) }
}