package org.home.zaval.zavalbackend.util

import org.home.zaval.zavalbackend.dto.article.*
import org.home.zaval.zavalbackend.entity.Article
import org.home.zaval.zavalbackend.entity.ArticleLightView
import org.home.zaval.zavalbackend.persistence.JsonHelper
import java.util.LinkedList

fun ArticleLightView.toLightDto() = ArticleLightDto(
    id = this.getId() ?: -100,
    title = this.getTitle(),
    contentTitles = this.getContentTitles().asContentTitleDtos(),
    popularity = this.getPopularity(),
)

fun Article.toLightDto() = ArticleLightDto(
    id = this.id!!,
    title = this.title,
    contentTitles = this.contentTitles.asContentTitleDtos(),
    popularity = this.popularity,
)

fun Article.toContentDto(content: String) = ArticleContentDto(
    id = this.id!!,
    content = content,
)

fun ArticleLightDto.toEntity() = Article(
    id = this.id,
    title = this.title,
    contentTitles = this.contentTitles.asString(),
    popularity = this.popularity,
)

fun ArticleLightDto.toStableDto() = ArticleLightStableDto(
    id = this.id,
    title = this.title,
    contentTitles = this.contentTitles,
)

fun ArticleLightDto.toContentDto(content: String) = ArticleContentDto(
    id = this.id,
    content = content,
)

fun ArticleLightStableDto.toLightDto(popularity: Long) = ArticleLightDto(
    id = this.id,
    title = this.title,
    contentTitles = this.contentTitles,
    popularity = popularity,
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
    var diveLevel = 1
    // to sign the level of dive
    val DELIMITER_FRAGMENT = RichFragmentDto(
        richType = "STACK_DELIMITER",
        children = listOf()
    )
    val fragmentsToVisitQueue = LinkedList<RichFragmentDto>().apply {
        richFragments.forEach { add(it) }
    }
    while (fragmentsToVisitQueue.isNotEmpty()) {
        val visitingFragment = fragmentsToVisitQueue.pollFirst()
        when {
            // if delimiter - float and skip iteration
            visitingFragment.richType == DELIMITER_FRAGMENT.richType -> {
                diveLevel--
            }
            // Add to result if title
            visitingFragment.richType == "title" -> {
                val titleContent = if (visitingFragment.children.isNotEmpty())
                    visitingFragment.children[0] as String
                else ""
                resultTitleDtos.add(ContentTitleDto(level = diveLevel, title = titleContent))
            }
            // Plan to visit the rich fragment children next in their DIRECT order
            visitingFragment.children.isNotEmpty() -> {
                diveLevel++
                fragmentsToVisitQueue.addFirst(DELIMITER_FRAGMENT)
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