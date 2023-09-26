package org.home.zaval.zavalbackend.store

import org.home.zaval.zavalbackend.dto.article.ArticleDirectoryDto
import org.home.zaval.zavalbackend.dto.article.ArticleDto
import org.home.zaval.zavalbackend.dto.persistence.AggregationInfoDto
import org.home.zaval.zavalbackend.dto.persistence.TodoPersistedValues
import org.home.zaval.zavalbackend.exception.UnknownFileException
import org.home.zaval.zavalbackend.persistence.MultiFilePersistableObjects
import org.home.zaval.zavalbackend.persistence.PersistableObject
import org.home.zaval.zavalbackend.persistence.StorageFileWorker
import org.home.zaval.zavalbackend.persistence.ensurePersistence
import org.home.zaval.zavalbackend.util.path
import java.nio.file.Path

object ArticleStore {
    const val ARTICLES_DIR = "articles"
    const val ARTICLE_DIRECTORIES_SUBDIR = "article-directories"
    const val ACTUAL_SUBDIR = "actual"
    const val OUTDATED_SUBDIR = "outdated"

    const val AGGREGATION_INFO_CACHE = "aggregation-info-cache.json"
    const val PERSISTED_VALUES_FILENAME = "persisted-values.json"

    val aggregationInfo = PersistableObject<AggregationInfoDto>(resolve(AGGREGATION_INFO_CACHE))
    val persistedValues = PersistableObject<TodoPersistedValues>(resolve(PERSISTED_VALUES_FILENAME))
    val articleDirectoriesContent = MultiFilePersistableObjects(
        relativeToStorageRootDirPath = resolveRelative(ARTICLE_DIRECTORIES_SUBDIR),
        entityClass = ArticleDirectoryDto::class.java,
        idExtractor = { it.id.toString() },
    )
    val articlesContent = MultiFilePersistableObjects(
        relativeToStorageRootDirPath = resolveRelative(ACTUAL_SUBDIR),
        entityClass = ArticleDto::class.java,
        idExtractor = { it.id.toString() },
    )
    val outdatedTodosContent = MultiFilePersistableObjects(
        relativeToStorageRootDirPath = resolveRelative(OUTDATED_SUBDIR),
        entityClass = ArticleDto::class.java,
        idExtractor = { it.id.toString() },
    )

    var active = true

    fun getId(): Long {
        var resultId = Long.MIN_VALUE
        ensurePersistence(persistedValues) {
            resultId = persistedValues.modObj.idSequence++
        }
        return resultId
    }

    fun resolveRelative(filename: String): String {
        return when (filename) {
            ARTICLES_DIR -> ARTICLES_DIR
            ACTUAL_SUBDIR, OUTDATED_SUBDIR, ARTICLE_DIRECTORIES_SUBDIR,
            AGGREGATION_INFO_CACHE, PERSISTED_VALUES_FILENAME -> "$ARTICLES_DIR/$filename"

            else -> throw UnknownFileException(filename)
        }
    }

    fun resolve(filename: String): Path {
        val enhancedFilename = resolveRelative(filename)
        return StorageFileWorker.resolveRelative(enhancedFilename.path)
    }
}