package org.home.zaval.zavalbackend.dto.article

import org.home.zaval.zavalbackend.dto.IdentifiedDto

class ArticleDto(
    id: Long,
    val title: String,
    val content: String,
    val directoryId: Long? = null,
) : IdentifiedDto(id)

class ArticleLightDto(
    id: Long,
    val title: String,
    val directoryId: Long? = null,
) : IdentifiedDto(id)

class ArticleDirectoryDto(
    id: Long,
    val name: String,
    val parentId: Long? = null,
) : IdentifiedDto(id)