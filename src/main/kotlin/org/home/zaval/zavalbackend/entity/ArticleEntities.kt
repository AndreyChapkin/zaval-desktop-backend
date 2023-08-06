package org.home.zaval.zavalbackend.entity

class Article(
    id: Long,
    val name: String,
    val text: String,
) : BaseEntity(id)