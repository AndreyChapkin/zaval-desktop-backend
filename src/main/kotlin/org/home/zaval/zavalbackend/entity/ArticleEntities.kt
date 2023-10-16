package org.home.zaval.zavalbackend.entity

import org.home.zaval.zavalbackend.util.asUtc
import java.time.OffsetDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id

@Entity
class Article(
    @Id
    var id: Long?,
    @Column(length = 1000)
    var title: String,
    @Column(length = 20000)
    var contentTitles: String = "",
    var interactedOn: OffsetDateTime = OffsetDateTime.now().asUtc,
)

@Entity
class ArticleLabel(
    @Id
    var id: Long?,
    @Column(length = 1000)
    var name: String,
)

@Entity
class LabelToArticleConnection(
    @Id
    var id: Long,
    var articleId: Long,
    var labelId: Long,
)
