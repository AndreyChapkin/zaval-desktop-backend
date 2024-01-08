package org.home.zaval.zavalbackend.entity

import org.home.zaval.zavalbackend.util.asUtc
import java.time.OffsetDateTime
import javax.persistence.*

@Entity
@Table(name = "ARTICLES")
class Article(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "general_sequence_generator")
    @SequenceGenerator(name = "general_sequence_generator", sequenceName = "article_seq", allocationSize = 1)
    var id: Long?,
    var title: String,
    var content: String,
    var interactedOn: OffsetDateTime = OffsetDateTime.now().asUtc,
)

@Entity
@Table(name = "ARTICLE_SERIES")
class ArticleSeries(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "general_sequence_generator")
    @SequenceGenerator(name = "general_sequence_generator", sequenceName = "article_seq", allocationSize = 1)
    var id: Long?,
    var title: String,
    var interactedOn: OffsetDateTime = OffsetDateTime.now().asUtc,
)

@Entity
@Table(name = "SERIES_ARTICLE_CONNECTIONS")
class SeriesArticleConnection(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "general_sequence_generator")
    @SequenceGenerator(name = "general_sequence_generator", sequenceName = "article_seq", allocationSize = 1)
    var id: Long?,
    var seriesId: Long,
    var articleId: Long,
)

@Entity
@Table(name = "ARTICLE_LABELS")
class ArticleLabel(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "general_sequence_generator")
    @SequenceGenerator(name = "general_sequence_generator", sequenceName = "article_seq", allocationSize = 1)
    var id: Long?,
    @Column(length = 1000)
    var name: String,
)

@Entity
@Table(name = "LABEL_ARTICLE_CONNECTIONS")
class LabelArticleConnection(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "general_sequence_generator")
    @SequenceGenerator(name = "general_sequence_generator", sequenceName = "article_seq", allocationSize = 1)
    var id: Long?,
    var articleId: Long,
    var labelId: Long,
)

@Entity
@Table(name = "LABEL_SERIES_CONNECTIONS")
class LabelSeriesConnection(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "general_sequence_generator")
    @SequenceGenerator(name = "general_sequence_generator", sequenceName = "article_seq", allocationSize = 1)
    var id: Long?,
    var seriesId: Long,
    var labelId: Long,
)
