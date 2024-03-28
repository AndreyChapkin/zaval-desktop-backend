package org.home.zaval.zavalbackend.service

import org.home.zaval.zavalbackend.dto.article.*
import org.home.zaval.zavalbackend.entity.*
import org.home.zaval.zavalbackend.repository.*
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

    @Transactional
    fun getArticleContent(id: Long?): ArticleContentDto? {
        val content = id?.let { articleRepository.getArticleContentById(it) }
            ?: return null
        articleRepository.updateInteractedOn(id, OffsetDateTime.now())
        return ArticleContentDto(id = id, content = content)
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
}