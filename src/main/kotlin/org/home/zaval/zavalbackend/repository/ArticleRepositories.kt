package org.home.zaval.zavalbackend.repository

import org.home.zaval.zavalbackend.entity.Article
import org.home.zaval.zavalbackend.entity.ArticleLabel
import org.home.zaval.zavalbackend.entity.LabelToArticleConnection
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository


@Repository
interface ArticleRepository : PagingAndSortingRepository<Article, Long> {

    @Query(
        "select a from Article a " +
                "where upper(title) like upper(:PATTERN) or upper(contentTitles) like upper(:PATTERN)"
    )
    fun findAllArticlesWithTitleFragment(@Param("PATTERN") pattern: String): List<Article>

    @Query("select a from Article a where a.id in :IDS")
    fun findByIds(@Param("IDS") ids: Collection<Long>): List<Article>
}

@Repository
interface ArticleLabelRepository : CrudRepository<ArticleLabel, Long> {

    @Query("select al from ArticleLabel al where upper(al.name) like upper(:PATTERN)")
    fun findLabelsWithNameFragment(@Param("PATTERN") pattern: String): List<ArticleLabel>
}

@Repository
interface LabelToArticleConnectionRepository : CrudRepository<LabelToArticleConnection, Long> {

    @Query("select c from LabelToArticleConnection c where c.labelId in :LABEL_IDS")
    fun findConnectionsWithLabelIds(@Param("LABEL_IDS") labelIds: Iterable<Long>): List<LabelToArticleConnection>

    @Query("select c from LabelToArticleConnection c where c.articleId = :ARTICLE_ID")
    fun findConnectionsWithArticleId(@Param("ARTICLE_ID") articleId: Long): List<LabelToArticleConnection>

    @Query("select c from LabelToArticleConnection c where c.labelId = :LABEL_ID and c.articleId = :ARTICLE_ID")
    fun findConnectionWithLabelAndArticleIds(
        @Param("LABEL_ID") labelId: Long,
        @Param("ARTICLE_ID") articleId: Long
    ): LabelToArticleConnection?

    @Modifying
    @Query("delete from LabelToArticleConnection c where c.labelId = :LABEL_ID")
    fun deleteAllConnectionsWithLabelId(@Param("LABEL_ID") labelId: Long)

    @Modifying
    @Query("delete from LabelToArticleConnection c where c.articleId = :ARTICLE_ID")
    fun deleteAllConnectionsWithArticleId(@Param("ARTICLE_ID") articleId: Long)
}