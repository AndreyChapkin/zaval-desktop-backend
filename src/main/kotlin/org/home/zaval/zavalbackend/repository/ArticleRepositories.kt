package org.home.zaval.zavalbackend.repository

import org.home.zaval.zavalbackend.entity.*
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ArticleRepository : CrudRepository<Article, Long> {

    @Query("select id, title, directory_id as directoryId from article a where upper(title) like upper(:PATTERN)", nativeQuery = true)
    fun findAllLightArticlesWithTitleFragment(@Param("PATTERN") pattern: String): List<ArticleLightView>
}

@Repository
interface ArticleDirectoryRepository : CrudRepository<ArticleDirectory, Long> {

    @Query("select d from ArticleDirectory d where d.parent is null")
    fun getAllTopDirectories(): List<Todo>

    @Modifying
    @Query("delete from ArticleDirectory d where d.id in :IDS")
    fun deleteAllForIds(@Param("IDS") todoIds: List<Long>)
}