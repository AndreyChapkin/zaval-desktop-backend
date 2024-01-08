package org.home.zaval.zavalbackend.util

import org.home.zaval.zavalbackend.dto.article.RichFragmentDto

fun Map<String, Any>.toRichFragmentDto() = RichFragmentDto(
    richType = this["richType"] as String,
    attributes = this["attributes"] as Map<String, String>?,
    children = this["children"] as List<Any>,
)

// TODO: remove
//fun extractContentTitles(articleContent: String): List<ContentTitleDto> {
//    val richFragments = JsonHelper.deserializeObject<Array<RichFragmentDto>>(articleContent)
//    val resultTitleDtos = mutableListOf<ContentTitleDto>()
//    val fragmentsToVisitQueue = LinkedList<RichFragmentDto>().apply {
//        richFragments.forEach { add(it) }
//    }
//    while (fragmentsToVisitQueue.isNotEmpty()) {
//        val visitingFragment = fragmentsToVisitQueue.pollFirst()
//        when {
//            // Add to result if title
//            listOf("title-1", "title-2", "title-3", "title-4").contains(visitingFragment.richType) -> {
//                val titleContent = if (visitingFragment.children.isNotEmpty())
//                    visitingFragment.children[0] as String
//                else ""
//                val titleLevel = when (visitingFragment.richType) {
//                    "title-1" -> 1
//                    "title-2" -> 2
//                    "title-3" -> 3
//                    "title-4" -> 4
//                    else -> 0
//                }
//                val titleId = visitingFragment.attributes?.let { it["id"] }!!
//                resultTitleDtos.add(ContentTitleDto(level = titleLevel, title = titleContent, id = titleId))
//            }
//            // Plan to visit the rich fragment children next in their DIRECT order
//            visitingFragment.children.isNotEmpty() -> {
//                for (i in visitingFragment.children.indices.reversed()) {
//                    val child = visitingFragment.children[i]
//                    if (child is Map<*, *>) {
//                        val childFragment = (child as Map<String, Any>).toRichFragmentDto()
//                        fragmentsToVisitQueue.addFirst(childFragment)
//                    }
//                }
//            }
//        }
//    }
//    return resultTitleDtos
//}