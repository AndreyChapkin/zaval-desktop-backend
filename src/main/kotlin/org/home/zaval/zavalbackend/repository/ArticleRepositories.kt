package org.home.zaval.zavalbackend.repository

import org.home.zaval.zavalbackend.entity.Article
import org.home.zaval.zavalbackend.entity.projection.ArticleLightProjection
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime


@Repository
interface ArticleRepository : PagingAndSortingRepository<Article, Long> {

    @Query(
        "select id, title, interacted_on as interactedOn from ARTICLES where id in :IDS",
        nativeQuery = true
    )
    fun findArticleLightsByIds(@Param("IDS") ids: Iterable<Long>): List<ArticleLightProjection>

    @Query(
        "select id, title, interacted_on as interactedOn from ARTICLES",
        nativeQuery = true
    )
    fun findArticleLightsByPage(pageable: Pageable): Page<ArticleLightProjection>

    @Query(
        "select id, title, interacted_on as interactedOn from ARTICLES where upper(title) like upper(:PATTERN)",
        nativeQuery = true
    )
    fun findAllArticleLightsWithTitleFragment(@Param("PATTERN") pattern: String): List<ArticleLightProjection>

    @Query("select content from Article where id = :id")
    fun getArticleContentById(@Param("id") id: Long): String?

    @Query("UPDATE Article SET interactedOn = :interactedOn WHERE id = :id")
    fun updateInteractedOn(@Param("id") id: Long, @Param("interactedOn") interactedOn: OffsetDateTime)
}
