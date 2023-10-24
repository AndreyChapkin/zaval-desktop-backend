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

    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getAllArticleLights(): ResponseEntity<List<ArticleLightDto>> {
        return ResponseEntity.ok(articleService.getAllArticleLights())
    }

    @GetMapping("/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getArticleLight(@PathVariable("id") articleId: String): ResponseEntity<ArticleLightDto?> {
        return ResponseEntity.ok(articleService.getArticleLight(articleId.toLong()))
    }

    @PostMapping("/with-labels", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun findAllArticlesWithAllLabels(@RequestBody paramsBody: Map<String, List<Long>>): ResponseEntity<List<ArticleLightDto>> {
        val labelIds = paramsBody["labelIds"]!!
        return ResponseEntity.ok(articleService.findAllArticleLightsWithAllLabels(labelIds))
    }

    @GetMapping("/with-label-name-fragment", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun findAllArticlesWithLabelNameFragment(@RequestParam("name-fragment") nameFragment: String): ResponseEntity<List<ArticleLightWithLabelsDto>> {
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

    @GetMapping("/label", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getAllArticleLabels(): ResponseEntity<List<ArticleLabelDto>> {
        return ResponseEntity.ok(articleService.getAllArticleLabels())
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
        @RequestBody updateArticleLabelDto: UpdateArticleLabelDto
    ): ResponseEntity<Unit> {
        articleService.updateArticleLabel(articleLabelId.toLong(), updateArticleLabelDto)
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
        "/label/combination",
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun createLabelsCombination(@RequestBody labelIdsMap: Map<String, List<Long>>): ResponseEntity<LabelsCombinationDto> {
        val labelIds = labelIdsMap["labelIds"]!!
        return ResponseEntity.ok(articleService.createLabelsCombination(labelIds))
    }

    @GetMapping("/label/combination/popular", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getTheMostPopularLabelsCombinations(@RequestParam("number") number: String?): ResponseEntity<List<FilledLabelsCombinationDto>> {
        return ResponseEntity.ok(articleService.getTheMostPopularLabelsCombinations(number?.toInt()))
    }

    @PatchMapping(
        "/label/combination/{id}",
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun updateLabelsCombinationPopularity(
        @PathVariable("id") combinationId: String,
        @RequestBody popularityMap: Map<String, String>
    ): ResponseEntity<Unit> {
        val popularity = popularityMap["popularity"]!!
        articleService.updateLabelsCombinationPopularity(
            combinationId = combinationId.toLong(),
            popularity = popularity.toLong()
        )
        return ResponseEntity.ok(null)
    }

    @DeleteMapping("/label/combination/{id}")
    fun deleteLabelsCombination(@PathVariable("id") combinationId: String): ResponseEntity<Unit> {
        articleService.deleteLabelsCombination(combinationId.toLong())
        return ResponseEntity.ok(null)
    }
}