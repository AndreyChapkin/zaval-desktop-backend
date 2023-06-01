package org.home.zaval.zavalbackend.controller

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/article")
class ArticleController(
//    val articleService: ArticleService
) {
//    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
//    fun createArticles(@RequestBody articles: List<Article>): ResponseEntity<List<Article>> {
//        return ResponseEntity.ok(articleService.createArticles(articles))
//    }
//
//    @GetMapping("/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
//    fun getArticle(@PathVariable("id") articleId: String): ResponseEntity<Article?> {
//        return ResponseEntity.ok(articleService.getArticle(articleId.toLong()))
//    }
//
//    @PatchMapping("/{id}", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
//    fun updateArticle(
//        @PathVariable("id") articleId: String,
//        @RequestParam("command") command: ArticleChangeCommand,
//        @RequestBody article: Article
//    ): ResponseEntity<Article> {
//        return ResponseEntity.ok(articleService.updateArticle(articleId.toLong(), article, command))
//    }
//
//    @DeleteMapping("/{id}")
//    fun deleteArticle(@PathVariable("id") articleId: String): ResponseEntity<Unit> {
//        articleService.deleteArticles(listOf(articleId.toLong()))
//        return ResponseEntity.ok(null)
//    }
//
//    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
//    fun getAllArticlesWithoutTexts(): ResponseEntity<List<Article>> {
//        return ResponseEntity.ok(articleService.getAllArticlesWithoutTexts())
//    }
}