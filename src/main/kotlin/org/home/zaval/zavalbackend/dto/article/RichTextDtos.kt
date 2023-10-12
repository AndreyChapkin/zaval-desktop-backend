package org.home.zaval.zavalbackend.dto.article

class RichFragmentDto(
    val richType: String,
    val attributes: Map<String, String>? = null,
    val children: List<Any>
)