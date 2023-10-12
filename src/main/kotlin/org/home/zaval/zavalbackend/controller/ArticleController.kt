package org.home.zaval.zavalbackend.controller

import org.home.zaval.zavalbackend.dto.article.ArticleContentDto
import org.home.zaval.zavalbackend.dto.article.ArticleLightDto
import org.home.zaval.zavalbackend.dto.article.UpdateArticleDto
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
    @GetMapping("/all", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getAllArticleLights(): ResponseEntity<List<ArticleLightDto>> {
        return ResponseEntity.ok(articleService.getAllArticleLights())
    }

    @GetMapping("/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getArticleLight(@PathVariable("id") articleId: String): ResponseEntity<ArticleLightDto?> {
        return ResponseEntity.ok(articleService.getArticleLight(articleId.toLong()))
    }

    @GetMapping("/{id}/content", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getArticleContent(@PathVariable("id") articleId: String): ResponseEntity<ArticleContentDto?> {
        return ResponseEntity.ok(articleService.getArticleContent(articleId.toLong()))
    }

    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun createArticle(@RequestBody articleLightDto: ArticleLightDto): ResponseEntity<ArticleLightDto> {
        return ResponseEntity.ok(articleService.createArticle(articleLightDto))
    }

    @PatchMapping("/{id}", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun updateArticle(
        @PathVariable("id") articleId: String,
        @RequestBody updateArticleDto: UpdateArticleDto
    ): ResponseEntity<Unit> {
        articleService.updateArticle(articleId.toLong(), updateArticleDto)
        return ResponseEntity.ok().build()
    }

    @PatchMapping(
        "/{id}/popularity",
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun updateArticlePopularity(
        @PathVariable("id") articleId: String,
        @RequestBody popularityMap: Map<String, String>
    ): ResponseEntity<Unit> {
        articleService.updateArticlePopularity(articleId.toLong(), popularityMap["popularity"]!!.toLong())
        return ResponseEntity.ok(null)
    }

    @DeleteMapping("/{id}")
    fun deleteArticle(@PathVariable("id") todoId: String): ResponseEntity<Unit> {
        articleService.deleteArticle(todoId.toLong())
        return ResponseEntity.ok(null)
    }

    @GetMapping("/with-name-fragment", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun findAllArticlesByNameFragment(@RequestParam("name-fragment") nameFragment: String): ResponseEntity<List<ArticleLightDto>> {
        val decodedFragment = URLDecoder.decode(nameFragment, StandardCharsets.UTF_8)
        return ResponseEntity.ok(articleService.findAllArticleLightsByTitleFragment(decodedFragment))
    }
}