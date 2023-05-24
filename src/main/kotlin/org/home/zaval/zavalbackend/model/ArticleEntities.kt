package org.home.zaval.zavalbackend.model

class Article(
    id: Long,
    val name: String,
    val text: String,
) : BaseEntity(id)