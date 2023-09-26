package org.home.zaval.zavalbackend.entity

import javax.persistence.*

interface ArticleLightView {
    fun getId(): Long?
    fun getTitle(): String
    fun getDirectoryId(): Long?
}

@Entity
class Article(
    @Id
    var id: Long?,
    @Column(length = 1000)
    var title: String,
    @Column(length = 20000)
    var content: String = "",
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DIRECTORY_ID")
    var directory: ArticleDirectory? = null,
)

@Entity
class ArticleDirectory(
    @Id
    var id: Long?,
    @Column(length = 1000)
    var name: String,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PARENT_ID")
    var parent: ArticleDirectory? = null,
)