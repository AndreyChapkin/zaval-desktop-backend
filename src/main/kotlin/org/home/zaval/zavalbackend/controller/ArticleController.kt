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

    @PostMapping("/with-labels", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun findAllArticlesWithAllLabels(@RequestBody paramsBody: Map<String, List<Long>>): ResponseEntity<List<ArticleLightDto>> {
        val labelIds = paramsBody["labelIds"]!!
        return ResponseEntity.ok(articleService.findAllArticleLightsWithAllLabels(labelIds))
    }

    @GetMapping("/with-label-name-fragment", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun findAllArticlesWithLabelNameFragment(@RequestParam("name-fragment") nameFragment: String): ResponseEntity<List<ArticleWithLabelsDto>> {
        val decodedFragment = URLDecoder.decode(nameFragment, StandardCharsets.UTF_8)
        return ResponseEntity.ok(articleService.findAllArticlesWithLabelNameFragment(decodedFragment))
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

    @GetMapping("/{id}/connected-labels", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getArticleLabels(@PathVariable("id") articleId: String): ResponseEntity<List<ArticleLabelDto>> {
        return ResponseEntity.ok(articleService.getArticleLabels(articleId.toLong()))
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

    @PostMapping("/label", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun createArticleLabel(@RequestBody articleLabelDto: ArticleLabelDto): ResponseEntity<ArticleLabelDto> {
        return ResponseEntity.ok(articleService.createArticleLabel(articleLabelDto))
    }

    @GetMapping("/label/with-name-fragment", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun findAllArticleLabelsWithNameFragment(@RequestParam("name-fragment") nameFragment: String): ResponseEntity<List<ArticleLabelDto>> {
        val decodedFragment = URLDecoder.decode(nameFragment, StandardCharsets.UTF_8)
        return ResponseEntity.ok(articleService.findAllArticleLabelsWithNameFragment(decodedFragment))
    }

    @GetMapping("/label/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getArticleLabel(@PathVariable("id") articleLabelId: String): ResponseEntity<ArticleLabelDto?> {
        return ResponseEntity.ok(articleService.getArticleLabel(articleLabelId.toLong()))
    }

    @PatchMapping(
        "/label/{id}",
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun updateArticleLabel(
        @PathVariable("id") articleLabelId: String,
        @RequestBody newNameMap: Map<String, String>
    ): ResponseEntity<Unit> {
        val name = newNameMap["name"]
        articleService.updateArticleLabel(articleLabelId.toLong(), name)
        return ResponseEntity.ok(null)
    }

    @DeleteMapping("/label/{id}")
    fun deleteArticleLabel(@PathVariable("id") articleLabelId: String): ResponseEntity<Unit> {
        articleService.deleteArticleLabel(articleLabelId.toLong())
        return ResponseEntity.ok(null)
    }

    @PostMapping("/label/bind")
    fun bindLabelToArticle(
        @RequestBody idsMap: Map<String, Any>,
    ): ResponseEntity<Unit> {
        val labelIds = (idsMap["labelIds"]!! as List<Any>).map { it.toString().toLong() }
        val articleId = idsMap["articleId"]!!.toString().toLong()
        articleService.bindLabelsToArticle(
            labelIds = labelIds,
            articleId = articleId,
        )
        return ResponseEntity.ok(null)
    }

    @PostMapping("/label/unbind")
    fun unbindLabelFromArticle(
        @RequestBody idsMap: Map<String, Any>,
    ): ResponseEntity<Unit> {
        val labelIds = (idsMap["labelIds"]!! as List<Any>).map { it.toString().toLong() }
        val articleId = idsMap["articleId"]!!.toString().toLong()
        articleService.unbindLabelsFromArticle(
            labelIds = labelIds,
            articleId = articleId,
        )
        return ResponseEntity.ok(null)
    }

    @PostMapping(
        "/series",
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun createArticleSeries(@RequestBody createSeriesDto: CreateArticleSeriesDto): ResponseEntity<ArticleSeriesDto> {
        return ResponseEntity.ok(articleService.createArticleSeries(createSeriesDto))
    }

    @GetMapping("/series/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getArticleSeries(@PathVariable("id") number: String?): ResponseEntity<ArticleSeriesDto> {
        return ResponseEntity.ok(articleService.getArticleSeries(number?.toLong()))
    }

    @GetMapping("/series/with-fragment", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun findAllArticleSeriesWithTitleFragment(@RequestParam("fragment") fragment: String): ResponseEntity<List<ArticleSeriesDto>> {
        val decodedFragment = URLDecoder.decode(fragment, StandardCharsets.UTF_8)
        return ResponseEntity.ok(articleService.findAllArticleSeriesWithTitleFragment(decodedFragment))
    }

    @PostMapping("/series/with-labels", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun findAllArticleSeriesWithAllLabels(@RequestBody paramsBody: Map<String, List<Long>>): ResponseEntity<List<SeriesWithLabelsDto>> {
        val labelIds = paramsBody["labelIds"]!!
        return ResponseEntity.ok(articleService.findAllSeriesWithAllLabels(labelIds))
    }

    @GetMapping("/series/recent", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getTheMostRecentArticleSeries(@RequestParam("number") number: String?): ResponseEntity<List<ArticleSeriesDto>> {
        return ResponseEntity.ok(articleService.getTheMostRecentArticleSeries(number?.toInt()))
    }

    @PatchMapping(
        "/series/{id}",
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun updateArticleSeries(
        @PathVariable("id") articleSeriesId: String,
        @RequestBody updateArticleSeriesDto: UpdateArticleSeriesDto
    ): ResponseEntity<Unit> {
        articleService.updateArticleSeries(
            articleSeriesId.toLong(),
            updateArticleSeriesDto
        )
        return ResponseEntity.ok(null)
    }

    @DeleteMapping("/series/{id}")
    fun deleteArticleSeries(@PathVariable("id") articleSeriesId: String): ResponseEntity<Unit> {
        articleService.deleteArticleSeries(articleSeriesId.toLong())
        return ResponseEntity.ok(null)
    }
}