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
    fun createArticle(@RequestBody articleLightDto: ArticleLightDto): ResponseEntity<ArticleLightDto> {
        return ResponseEntity.ok(articleService.createArticle(articleLightDto))
    }

    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getAllArticleLights(): ResponseEntity<List<ArticleLightDto>> {
        return ResponseEntity.ok(articleService.getAllArticleLights())
    }

    @GetMapping("/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getArticleLight(@PathVariable("id") articleId: String): ResponseEntity<ArticleLightDto?> {
        return ResponseEntity.ok(articleService.getArticleLight(articleId.toLong()))
    }

    @GetMapping("/with-labels", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun findAllArticlesWithAllLabels(@RequestBody labelIdsMap: Map<String, List<Long>>): ResponseEntity<List<ArticleLightDto>> {
        val labelIds = labelIdsMap["labelIds"]!!
        return ResponseEntity.ok(articleService.findAllArticleLightsWithAllLabels(labelIds))
    }

    @GetMapping("/with-name-fragment", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun findAllArticlesByNameFragment(@RequestParam("name-fragment") nameFragment: String): ResponseEntity<List<ArticleLightDto>> {
        val decodedFragment = URLDecoder.decode(nameFragment, StandardCharsets.UTF_8)
        return ResponseEntity.ok(articleService.findAllArticleLightsByTitleFragment(decodedFragment))
    }

    @GetMapping("/popular", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getTheMostPopularArticleLights(@RequestParam("number") number: String?): ResponseEntity<List<ArticleLightDto>> {
        return ResponseEntity.ok(articleService.getTheMostPopularArticleLights(number?.toInt()))
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

    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun createArticleLabel(@RequestBody articleLabelDto: ArticleLabelDto): ResponseEntity<ArticleLabelDto> {
        return ResponseEntity.ok(articleService.createArticleLabel(articleLabelDto))
    }

    @GetMapping("/label", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getAllArticleLabels(): ResponseEntity<List<ArticleLabelDto>> {
        return ResponseEntity.ok(articleService.getAllArticleLabels())
    }

    @GetMapping("/label/popular", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getTheMostPopularArticleLabels(@RequestParam("number") number: String?): ResponseEntity<List<ArticleLabelDto>> {
        return ResponseEntity.ok(articleService.get(number?.toInt()))
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
        @RequestParam("labelId") labelId: String,
        @RequestParam("articleId") articleId: String,
    ): ResponseEntity<Unit> {
        articleService.bindLabelToArticle(
            labelId = labelId.toLong(),
            articleId = articleId.toLong(),
        )
        return ResponseEntity.ok(null)
    }

    @PostMapping("/label/unbind")
    fun unbindLabelFromArticle(
        @RequestParam("labelId") labelId: String,
        @RequestParam("articleId") articleId: String,
    ): ResponseEntity<Unit> {
        articleService.unbindLabelFromArticle(
            labelId = labelId.toLong(),
            articleId = articleId.toLong(),
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

    @GetMapping("/label/combination/populart", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getTheMostPopularLabelsCombinations(@RequestParam("number") number: String?): ResponseEntity<List<LabelsCombinationDto>> {
        return ResponseEntity.ok(articleService.get(number?.toInt()))
    }

    @PatchMapping(
        "/label/combination",
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