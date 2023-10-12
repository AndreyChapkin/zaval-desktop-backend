package org.home.zaval.zavalbackend.repository

import org.home.zaval.zavalbackend.entity.Article
import org.home.zaval.zavalbackend.entity.ArticleLabel
import org.home.zaval.zavalbackend.entity.ArticleLightView
import org.home.zaval.zavalbackend.entity.LabelToArticleConnection
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository


@Repository
interface ArticleRepository : CrudRepository<Article, Long> {

    @Modifying
    @Query("update Article a set a.popularity = :POPULARITY where a.id = :ID")
    fun updatePopularity(@Param("ID") id: Long, @Param("POPULARITY") popularity: Long)

    @Query("select id, title, contentTitles, popularity from article a where a.id = :ARTICLE_ID", nativeQuery = true)
    fun findArticleLightById(@Param("ARTICLE_ID") articleId: Long): ArticleLightView?

    @Query("select id, title, contentTitles, popularity from article a where a.id in :IDS", nativeQuery = true)
    fun findAllArticleLightsById(@Param("IDS") ids: List<Long>): List<ArticleLightView>

    @Query("select id, title, contentTitles, popularity from article a where upper(title) like upper(:PATTERN)", nativeQuery = true)
    fun findAllLightArticlesWithTitleFragment(@Param("PATTERN") pattern: String): List<ArticleLightView>
}

@Repository
interface ArticleLabelRepository : CrudRepository<ArticleLabel, Long> {

    @Query("select al from ArticleLabel al where upper(al.name) like upper(:PATTERN)")
    fun findLabelsWithNameFragment(@Param("PATTERN") pattern: String): List<ArticleLabel>
}

@Repository
interface LabelToArticleConnectionRepository : CrudRepository<LabelToArticleConnection, Long> {

    @Query("select c from LabelToArticleConnection c where c.labelId = :LABEL_ID")
    fun findConnectionsWithLabelId(@Param("LABEL_ID") labelId: Long): List<LabelToArticleConnection>
}