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
