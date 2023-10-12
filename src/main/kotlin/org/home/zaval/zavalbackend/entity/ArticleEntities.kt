package org.home.zaval.zavalbackend.entity

import javax.persistence.*

interface ArticleLightView {
    fun getId(): Long?
    fun getTitle(): String
    fun getContentTitles(): String
    fun getPopularity(): Long
}

@Entity
class Article(
    @Id
    var id: Long?,
    @Column(length = 1000)
    var title: String,
    @Column(length = 20000)
    var contentTitles: String = "",
    var popularity: Long,
)

@Entity
class ArticleLabel(
    @Id
    var id: Long?,
    @Column(length = 1000)
    var name: String,
    var popularity: Long,
)

@Entity
class LabelToArticleConnection(
    @Id
    var id: Long,
    var articleId: Long,
    var labelId: Long,
)