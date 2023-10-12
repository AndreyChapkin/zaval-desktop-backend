package org.home.zaval.zavalbackend.store

import org.home.zaval.zavalbackend.dto.article.*
import org.home.zaval.zavalbackend.dto.persistence.ArticlePersistedValues
import org.home.zaval.zavalbackend.dto.persistence.ArticlePopularity
import org.home.zaval.zavalbackend.persistence.MultiFilePersistableObjects
import org.home.zaval.zavalbackend.persistence.PersistableObject
import org.home.zaval.zavalbackend.persistence.ensurePersistence
import org.home.zaval.zavalbackend.util.path
import org.home.zaval.zavalbackend.util.toLightDto
import org.home.zaval.zavalbackend.util.toStableDto

object ArticleStore {
    val BASE_DIR = "article-store".path

    val ARTICLE_LIGHTS_DIR = BASE_DIR
        .resolve("article-lights")
    val ARTICLE_CONTENTS_DIR = BASE_DIR
        .resolve("article-contents")
    val ARTICLE_CONTENT_TITLES_DIR = BASE_DIR
        .resolve("article-content-titles")
    val ARTICLE_LABELS_SUBDIR = BASE_DIR
        .resolve("article-labels")
    val LABEL_TO_ARTICLE_CONNECTIONS_SUBDIR = BASE_DIR
        .resolve("label-to-article-connections")

    val ACTUAL_ARTICLE_LIGHTS_SUBDIR = ARTICLE_LIGHTS_DIR
        .resolve("actual")
    val OUTDATED_ARTICLE_LIGHTS_SUBDIR = ARTICLE_LIGHTS_DIR
        .resolve("outdated")

    val ACTUAL_ARTICLE_CONTENTS_SUBDIR = ARTICLE_CONTENTS_DIR
        .resolve("actual")
    val OUTDATED_ARTICLE_CONTENTS_SUBDIR = ARTICLE_CONTENTS_DIR
        .resolve("outdated")

    val ACTUAL_ARTICLE_CONTENT_TITLES_SUBDIR = ARTICLE_CONTENT_TITLES_DIR
        .resolve("actual")
    val OUTDATED_ARTICLE_CONTENT_TITLES_SUBDIR = ARTICLE_CONTENT_TITLES_DIR
        .resolve("outdated")

    val ARTICLE_POPULARITY_FILENAME = BASE_DIR
        .resolve("article-popularity.json")

    val PERSISTED_VALUES_FILENAME = BASE_DIR
        .resolve("persisted-values.json")

    val persistedValues = PersistableObject<ArticlePersistedValues>(PERSISTED_VALUES_FILENAME)

    // Articles' light parts
    val actualArticleLightStablesContent = MultiFilePersistableObjects(
        relativeToStorageRootDirPath = ACTUAL_ARTICLE_LIGHTS_SUBDIR.toString(),
        entityClass = ArticleLightStableDto::class.java,
        idExtractor = { it.id.toString() },
    )
    val outdatedArticleLightStablesContent = MultiFilePersistableObjects(
        relativeToStorageRootDirPath = OUTDATED_ARTICLE_LIGHTS_SUBDIR.toString(),
        entityClass = ArticleLightStableDto::class.java,
        idExtractor = { it.id.toString() },
    )

    // Articles' heavy parts
    val actualArticleContentsContent = MultiFilePersistableObjects(
        relativeToStorageRootDirPath = ACTUAL_ARTICLE_CONTENTS_SUBDIR.toString(),
        entityClass = ArticleContentDto::class.java,
        idExtractor = { it.id.toString() },
    )
    val outdatedArticleContentsContent = MultiFilePersistableObjects(
        relativeToStorageRootDirPath = OUTDATED_ARTICLE_CONTENTS_SUBDIR.toString(),
        entityClass = ArticleContentDto::class.java,
        idExtractor = { it.id.toString() },
    )

    // Articles' the most volatile fields
    val articlePopularity = PersistableObject<ArticlePopularity>(ARTICLE_POPULARITY_FILENAME)

    // Labels
    val articleLabelsContent = MultiFilePersistableObjects(
        relativeToStorageRootDirPath = ARTICLE_LABELS_SUBDIR.toString(),
        entityClass = ArticleLabelDto::class.java,
        idExtractor = { it.id.toString() },
        maxEntitiesInFile = 10,
    )

    // Label to article connections
    val labelToArticleConnectionsContent = MultiFilePersistableObjects(
        relativeToStorageRootDirPath = LABEL_TO_ARTICLE_CONNECTIONS_SUBDIR.toString(),
        entityClass = LabelToArticleConnectionDto::class.java,
        idExtractor = { it.id.toString() },
        maxEntitiesInFile = 10,
    )

    var active = true

    fun saveArticleLight(article: ArticleLightDto) {
        if (!active) {
            return
        }
        actualArticleLightStablesContent.saveEntity(article.toStableDto())
        updateArticlePopularity(article.id, article.popularity)
    }

    fun saveArticleContent(articleContent: ArticleContentDto) {
        if (!active) {
            return
        }
        actualArticleContentsContent.saveEntity(articleContent)
    }

    fun saveArticleLabel(articleLabel: ArticleLabelDto) {
        if (!active) {
            return
        }
        articleLabelsContent.saveEntity(articleLabel)
    }

    fun saveLabelToArticleConnection(labelToArticleConnection: LabelToArticleConnectionDto) {
        if (!active) {
            return
        }
        labelToArticleConnectionsContent.saveEntity(labelToArticleConnection)
    }

    fun updateArticleLight(articleLight: ArticleLightDto) {
        if (!active) {
            return
        }
        val outdatedArticleLight = actualArticleLightStablesContent.updateEntity(articleLight.toStableDto())
        val isOutdatedAlreadySaved = outdatedArticleLightStablesContent.readEntity(outdatedArticleLight.id) != null
        if (isOutdatedAlreadySaved) {
            outdatedArticleLightStablesContent.updateEntity(outdatedArticleLight)
        } else {
            outdatedArticleLightStablesContent.saveEntity(outdatedArticleLight)
        }
        updateArticlePopularity(articleLight.id, articleLight.popularity)
    }

    fun updateArticlePopularity(articleId: Long, popularity: Long?) {
        ensurePersistence(articlePopularity) {
            if (popularity != null) {
                articlePopularity.modObj[articleId.toString()] = popularity
            } else {
                articlePopularity.modObj.remove(articleId.toString())
            }
        }
    }

    fun updateArticleContent(articleContent: ArticleContentDto) {
        if (!active) {
            return
        }
        val outdatedArticleContent = actualArticleContentsContent.updateEntity(articleContent)
        val isOutdatedAlreadySaved = outdatedArticleContentsContent.readEntity(outdatedArticleContent.id) != null
        if (isOutdatedAlreadySaved) {
            outdatedArticleContentsContent.updateEntity(outdatedArticleContent)
        } else {
            outdatedArticleContentsContent.saveEntity(outdatedArticleContent)
        }
    }

    fun updateArticleLabel(articleLabel: ArticleLabelDto) {
        if (!active) {
            return
        }
        articleLabelsContent.updateEntity(articleLabel)
    }

    fun removeArticleLight(articleId: Long) {
        if (!active) {
            return
        }
        val outdatedArticleLight: ArticleLightStableDto? =
            actualArticleLightStablesContent.removeEntity(articleId.toString())
        if (outdatedArticleLight != null) {
            val isAlreadySaved = outdatedArticleLightStablesContent.readEntity(outdatedArticleLight.id) != null
            if (isAlreadySaved) {
                outdatedArticleLightStablesContent.updateEntity(outdatedArticleLight)
            } else {
                outdatedArticleLightStablesContent.saveEntity(outdatedArticleLight)
            }
        }
        updateArticlePopularity(articleId, null)
    }

    fun removeArticleContent(articleId: Long) {
        if (!active) {
            return
        }
        val outdatedArticleContent: ArticleContentDto? = actualArticleContentsContent.removeEntity(articleId.toString())
        if (outdatedArticleContent != null) {
            val isAlreadySaved = outdatedArticleContentsContent.readEntity(outdatedArticleContent.id) != null
            if (isAlreadySaved) {
                outdatedArticleContentsContent.updateEntity(outdatedArticleContent)
            } else {
                outdatedArticleContentsContent.saveEntity(outdatedArticleContent)
            }
        }
    }

    fun removeArticleLabel(articleLabelId: Long) {
        if (!active) {
            return
        }
        articleLabelsContent.removeEntity(articleLabelId.toString())
    }

    fun removeLabelToArticleConnection(articleToLabelConnectionId: Long) {
        if (!active) {
            return
        }
        labelToArticleConnectionsContent.removeEntity(articleToLabelConnectionId.toString())
    }

    fun readAllArticleLights(): List<ArticleLightDto> {
        val stableDtos = actualArticleLightStablesContent.readAllEntities()
        val popularity = articlePopularity.readObj
        val lightDtos = stableDtos.map {
            it.toLightDto(popularity = popularity[it.id.toString()]!!)
        }
        return lightDtos
    }

    fun readArticleContent(articleId: Long): ArticleContentDto? {
        return actualArticleContentsContent.readEntity(articleId.toString())
    }

    fun readAllArticleLabels(): List<ArticleLabelDto> {
        return articleLabelsContent.readAllEntities()
    }

    fun readAllLabelToArticleConnections(): List<LabelToArticleConnectionDto> {
        return labelToArticleConnectionsContent.readAllEntities()
    }

    fun createDefaultPersistedValues(): ArticlePersistedValues {
        return ArticlePersistedValues(idSequence = 1L)
    }

    fun getId(): Long {
        var resultId = Long.MIN_VALUE
        ensurePersistence(persistedValues) {
            resultId = persistedValues.modObj.idSequence++
        }
        return resultId
    }
}