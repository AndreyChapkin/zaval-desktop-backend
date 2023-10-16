package org.home.zaval.zavalbackend.util

import org.home.zaval.zavalbackend.dto.article.*
import org.home.zaval.zavalbackend.entity.Article
import org.home.zaval.zavalbackend.entity.ArticleLabel
import org.home.zaval.zavalbackend.entity.LabelToArticleConnection
import org.home.zaval.zavalbackend.persistence.JsonHelper
import java.util.*


fun Article.toLightDto() = ArticleLightDto(
    id = this.id!!,
    title = this.title,
    contentTitles = this.contentTitles.asContentTitleDtos(),
    interactedOn = this.interactedOn.asStringFormattedWithISO8601withOffset(),
)

fun Article.toContentDto(content: String) = ArticleContentDto(
    id = this.id!!,
    content = content,
)

fun ArticleLightDto.toEntity() = Article(
    id = this.id,
    title = this.title,
    contentTitles = this.contentTitles.asString(),
    interactedOn = this.interactedOn.asOffsetDateTimeFromISO8601WithOffset(),
)

fun ArticleLightDto.toStableDto() = ArticleLightStableDto(
    id = this.id,
    title = this.title,
    contentTitles = this.contentTitles,
)

fun ArticleLightDto.toVolatileDto() = ArticleVolatileDto(
    id = this.id,
    interactedOn = this.interactedOn,
)

fun ArticleLightDto.toContentDto(content: String) = ArticleContentDto(
    id = this.id,
    content = content,
)

fun ArticleLightStableDto.toLightDto(interactedOn: String) = ArticleLightDto(
    id = this.id,
    title = this.title,
    contentTitles = this.contentTitles,
    interactedOn = interactedOn,
)

fun ArticleLabel.toDto() = ArticleLabelDto(
    id = this.id,
    name = this.name,
)

fun ArticleLabelDto.toEntity() = ArticleLabel(
    id = this.id,
    name = this.name,
)

fun LabelToArticleConnection.toDto() = LabelToArticleConnectionDto(
    id = this.id,
    articleId = this.articleId,
    labelId = this.labelId
)

fun List<ContentTitleDto>.asString() = JsonHelper.serializeObject(this)

fun String.asContentTitleDtos() = JsonHelper.deserializeObject<Array<ContentTitleDto>>(this).asList()

fun Map<String, Any>.toRichFragmentDto() = RichFragmentDto(
    richType = this["richType"] as String,
    attributes = this["attributes"] as Map<String, String>?,
    children = this["children"] as List<Any>,
)

fun extractContentTitles(articleContent: String): List<ContentTitleDto> {
    val richFragments = JsonHelper.deserializeObject<Array<RichFragmentDto>>(articleContent)
    val resultTitleDtos = mutableListOf<ContentTitleDto>()
    val fragmentsToVisitQueue = LinkedList<RichFragmentDto>().apply {
        richFragments.forEach { add(it) }
    }
    while (fragmentsToVisitQueue.isNotEmpty()) {
        val visitingFragment = fragmentsToVisitQueue.pollFirst()
        when {
            // Add to result if title
            listOf("title-1", "title-2", "title-3", "title-4").contains(visitingFragment.richType) -> {
                val titleContent = if (visitingFragment.children.isNotEmpty())
                    visitingFragment.children[0] as String
                else ""
                val titleLevel = when (visitingFragment.richType) {
                    "title-1" -> 1
                    "title-2" -> 2
                    "title-3" -> 3
                    "title-4" -> 4
                    else -> 0
                }
                val titleId = visitingFragment.attributes?.let { it["id"] }!!
                resultTitleDtos.add(ContentTitleDto(level = titleLevel, title = titleContent, id = titleId))
            }
            // Plan to visit the rich fragment children next in their DIRECT order
            visitingFragment.children.isNotEmpty() -> {
                for (i in visitingFragment.children.indices.reversed()) {
                    val child = visitingFragment.children[i]
                    if (child is Map<*, *>) {
                        val childFragment = (child as Map<String, Any>).toRichFragmentDto()
                        fragmentsToVisitQueue.addFirst(childFragment)
                    }
                }
            }
        }
    }
    return resultTitleDtos
}