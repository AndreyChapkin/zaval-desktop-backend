package org.home.zaval.zavalbackend.dto.persistence

class ArticlePersistedValues(
    var idSequence: Long,
)

typealias ArticlePopularity = MutableMap<String, Long>
