package org.home.zaval.zavalbackend.repository

import org.home.zaval.zavalbackend.entity.*
import org.home.zaval.zavalbackend.entity.projection.ArticleLightProjection
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
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

@Repository
interface ArticleSeriesRepository : PagingAndSortingRepository<ArticleSeries, Long> {

    @Query("select a from ArticleSeries a where upper(title) like upper(:PATTERN)")
    fun findAllArticleSeriesWithTitleFragment(@Param("PATTERN") pattern: String): List<ArticleSeries>

    @Query("UPDATE ArticleSeries SET interactedOn = :interactedOn WHERE id = :id")
    fun updateInteractedOn(@Param("id") id: Long, @Param("interactedOn") interactedOn: OffsetDateTime)
}

@Repository
interface SeriesArticleConnectionRepository : CrudRepository<SeriesArticleConnection, Long> {
    @Query("select c from SeriesArticleConnection c where c.seriesId = :SERIES_ID")
    fun findConnectionsWithSeriesId(@Param("SERIES_ID") seriesId: Long): List<SeriesArticleConnection>

    @Query("select c from SeriesArticleConnection c where c.seriesId in :SERIES_IDS")
    fun findAllConnectionsWithSeriesIds(@Param("SERIES_IDS") seriesIds: Collection<Long>): List<SeriesArticleConnection>

    @Modifying
    @Query("delete from SeriesArticleConnection c where c.seriesId = :SERIES_ID")
    fun deleteAllConnectionsWithSeriesId(@Param("SERIES_ID") seriesId: Long)
}

@Repository
interface ArticleLabelRepository : CrudRepository<ArticleLabel, Long> {

    @Query("select al from ArticleLabel al where upper(name) like upper(:PATTERN)")
    fun findLabelsWithNameFragment(@Param("PATTERN") pattern: String): List<ArticleLabel>
}

@Repository
interface LabelArticleConnectionRepository : CrudRepository<LabelArticleConnection, Long> {

    @Query("select c from LabelArticleConnection c where c.labelId in :LABEL_IDS")
    fun findConnectionsWithLabelIds(@Param("LABEL_IDS") labelIds: Iterable<Long>): List<LabelArticleConnection>

    @Query("select c from LabelArticleConnection c where c.articleId = :ARTICLE_ID")
    fun findConnectionsWithArticleId(@Param("ARTICLE_ID") articleId: Long): List<LabelArticleConnection>

    @Query("select c from LabelArticleConnection c where c.labelId = :LABEL_ID and c.articleId = :ARTICLE_ID")
    fun findConnectionWithLabelAndArticleIds(
        @Param("LABEL_ID") labelId: Long,
        @Param("ARTICLE_ID") articleId: Long
    ): LabelArticleConnection?

    @Modifying
    @Query("delete from LabelArticleConnection c where c.labelId = :LABEL_ID")
    fun deleteAllConnectionsWithLabelId(@Param("LABEL_ID") labelId: Long)

    @Modifying
    @Query("delete from LabelArticleConnection c where c.articleId = :ARTICLE_ID")
    fun deleteAllConnectionsWithArticleId(@Param("ARTICLE_ID") articleId: Long)

    @Modifying
    @Query("delete from LabelArticleConnection where articleId = :ARTICLE_ID and labelId in :LABEL_IDS")
    fun deleteAllConnectionOfArticleWithLabels(
        @Param("ARTICLE_ID") articleId: Long,
        @Param("LABEL_IDS") labelIds: Collection<Long>
    )
}

@Repository
interface LabelSeriesConnectionRepository : CrudRepository<LabelSeriesConnection, Long> {

    @Query("select c from LabelSeriesConnection c where c.labelId in :LABEL_IDS")
    fun findConnectionsWithLabelIds(@Param("LABEL_IDS") labelIds: Iterable<Long>): List<LabelSeriesConnection>

    @Query("select c from LabelSeriesConnection c where c.seriesId = :SERIES_ID")
    fun findConnectionsWithSeriesId(@Param("SERIES_ID") seriesId: Long): List<LabelSeriesConnection>

    @Modifying
    @Query("delete from LabelArticleConnection c where c.labelId = :LABEL_ID")
    fun deleteAllConnectionsWithLabelId(@Param("LABEL_ID") labelId: Long)

    @Modifying
    @Query("delete from LabelArticleConnection c where c.articleId = :ARTICLE_ID")
    fun deleteAllConnectionsWithArticleId(@Param("ARTICLE_ID") articleId: Long)

    @Modifying
    @Query("delete from LabelArticleConnection where articleId = :ARTICLE_ID and labelId in :LABEL_IDS")
    fun deleteAllConnectionOfArticleWithLabels(
        @Param("ARTICLE_ID") articleId: Long,
        @Param("LABEL_IDS") labelIds: Collection<Long>
    )
}