package org.home.zaval.zavalbackend.service

import org.home.zaval.zavalbackend.dto.article.ArticleDto
import org.home.zaval.zavalbackend.dto.article.ArticleLightDto
import org.home.zaval.zavalbackend.repository.ArticleDirectoryRepository
import org.home.zaval.zavalbackend.repository.ArticleRepository
import org.home.zaval.zavalbackend.store.ArticleStore
import org.home.zaval.zavalbackend.util.toEntity
import org.home.zaval.zavalbackend.util.toLightDto
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class ArticleService(
    val articleRepository: ArticleRepository,
    val directoryRepository: ArticleDirectoryRepository,
) {
    @Transactional
    fun createArticle(articleDto: ArticleDto): ArticleLightDto {
        val newArticle = articleDto.toEntity().apply {
            id = ArticleStore.getId()
        }
        val savedArticleLightDto = articleRepository.save(newArticle).toLightDto()
        return savedArticleLightDto
    }
}