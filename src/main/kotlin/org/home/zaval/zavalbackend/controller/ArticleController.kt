package org.home.zaval.zavalbackend.controller

import org.home.zaval.zavalbackend.dto.article.*
import org.home.zaval.zavalbackend.service.ArticleService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@RestController
@RequestMapping("/api/article")
@CrossOrigin("http://localhost:5173", "http://localhost:3000")
class ArticleController(
    val articleService: ArticleService
) {
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun createArticle(@RequestBody titleMap: Map<String, String>): ResponseEntity<ArticleLightDto> {
        val title = titleMap["title"]!!
        return ResponseEntity.ok(articleService.createArticle(title))
    }

    @PostMapping("by-id", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getArticleLightsById(@RequestBody articleIdsMap: Map<String, List<Long>>): ResponseEntity<List<ArticleLightDto>> {
        val articleIds = articleIdsMap["articleIds"] ?: emptyList()
        return ResponseEntity.ok(articleService.getArticleLightsByIds(articleIds))
    }

    @GetMapping("/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getArticleLight(@PathVariable("id") articleId: String): ResponseEntity<ArticleLightDto?> {
        return ResponseEntity.ok(articleService.getArticleLightById(articleId.toLong()))
    }

    @GetMapping("/with-title-fragment", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun findAllArticlesWithTitleFragment(@RequestParam("title-fragment") titleFragment: String): ResponseEntity<List<ArticleLightDto>> {
        val decodedFragment = URLDecoder.decode(titleFragment, StandardCharsets.UTF_8)
        return ResponseEntity.ok(articleService.findAllArticleLightsWithTitleFragment(decodedFragment))
    }

    @GetMapping("/recent", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getTheMostRecentArticleLights(@RequestParam("number") number: String?): ResponseEntity<List<ArticleLightDto>> {
        return ResponseEntity.ok(articleService.getTheMostRecentArticleLights(number?.toInt()))
    }

    @GetMapping("/{id}/content", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getArticleContent(@PathVariable("id") articleId: String): ResponseEntity<ArticleContentDto?> {
        return ResponseEntity.ok(articleService.getArticleContent(articleId.toLong()))
    }

    @PatchMapping("/{id}", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun updateArticle(
        @PathVariable("id") articleId: String,
        @RequestBody updateArticleDto: UpdateArticleDto
    ): ResponseEntity<Unit> {
        articleService.updateArticle(articleId.toLong(), updateArticleDto)
        return ResponseEntity.ok().build()
    }

    @DeleteMapping("/{id}")
    fun deleteArticle(@PathVariable("id") todoId: String): ResponseEntity<Unit> {
        articleService.deleteArticle(todoId.toLong())
        return ResponseEntity.ok(null)
    }
}