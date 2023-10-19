package org.home.zaval.zavalbackend.store

import org.home.zaval.zavalbackend.dto.article.*
import org.home.zaval.zavalbackend.dto.persistence.ArticlePersistedValues
import org.home.zaval.zavalbackend.persistence.MultiFilePersistableObjects
import org.home.zaval.zavalbackend.persistence.PersistableObject
import org.home.zaval.zavalbackend.persistence.ensurePersistence
import org.home.zaval.zavalbackend.util.path
import org.home.zaval.zavalbackend.util.toLightDto
import org.home.zaval.zavalbackend.util.toStableDto

object ArticleStore {
    val BASE_DIR = "article-store".path

    val ARTICLE_LIGHTS_DIR = BASE_DIR.resolve("article-lights")
    val ARTICLE_VOLATILE_SUBDIR = BASE_DIR.resolve("article-volatile")
    val ARTICLE_CONTENTS_DIR = BASE_DIR.resolve("article-contents")
    val ARTICLE_LABELS_SUBDIR = BASE_DIR.resolve("article-labels")
    val LABEL_TO_ARTICLE_CONNECTIONS_SUBDIR = BASE_DIR.resolve("label-to-article-connections")
    val LABEL_COMPBINATIONS_SUBDIR = BASE_DIR.resolve("label-combinations")

    val ACTUAL_ARTICLE_LIGHTS_SUBDIR = ARTICLE_LIGHTS_DIR.resolve("actual")
    val OUTDATED_ARTICLE_LIGHTS_SUBDIR = ARTICLE_LIGHTS_DIR.resolve("outdated")

    val ACTUAL_ARTICLE_VOLATILE_SUBDIR = ARTICLE_VOLATILE_SUBDIR.resolve("actual")

    val ACTUAL_ARTICLE_CONTENTS_SUBDIR = ARTICLE_CONTENTS_DIR.resolve("actual")
    val OUTDATED_ARTICLE_CONTENTS_SUBDIR = ARTICLE_CONTENTS_DIR.resolve("outdated")

    val PERSISTED_VALUES_FILENAME = BASE_DIR.resolve("persisted-values.json")

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
    val actualArticleVolatileContent = MultiFilePersistableObjects(
        relativeToStorageRootDirPath = ACTUAL_ARTICLE_VOLATILE_SUBDIR.toString(),
        entityClass = ArticleVolatileDto::class.java,
        idExtractor = { it.id.toString() },
        maxEntitiesInFile = 6,
    )

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

    // Label combinations
    val labelCombinationsInMemory: MutableMap<Long, LabelsCombinationDto> = mutableMapOf()
    val labelCombinationsContent = MultiFilePersistableObjects(
        relativeToStorageRootDirPath = LABEL_COMPBINATIONS_SUBDIR.toString(),
        entityClass = LabelsCombinationDto::class.java,
        idExtractor = { it.id.toString() },
        maxEntitiesInFile = 6,
    )

    private var active = true

    fun saveArticleLight(article: ArticleLightDto) {
        if (!active) {
            return
        }
        actualArticleLightStablesContent.saveEntity(article.toStableDto())
        saveArticleInteractedOn(article.id, article.interactedOn)
    }

    fun readAllArticleLights(): List<ArticleLightDto> {
        val stableDtos = actualArticleLightStablesContent.readAllEntities()
        val articleVolatileDtos = actualArticleVolatileContent.readAllEntities()
        val volatilesMap = mutableMapOf<Long, ArticleVolatileDto>().apply {
            articleVolatileDtos.forEach { this[it.id] = it }
        }
        val lightDtos = stableDtos.map {
            it.toLightDto(volatilesMap[it.id]!!.interactedOn)
        }
        return lightDtos
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
        updateArticleInteractedOn(articleId = articleLight.id, interactedOn = articleLight.interactedOn)
    }

    fun saveArticleInteractedOn(articleId: Long, interactedOn: String) {
        actualArticleVolatileContent.saveEntity(
            ArticleVolatileDto(
                id = articleId,
                interactedOn = interactedOn,
            )
        )
    }

    fun updateArticleInteractedOn(articleId: Long, interactedOn: String) {
        actualArticleVolatileContent.updateEntity(
            ArticleVolatileDto(
                id = articleId,
                interactedOn = interactedOn,
            )
        )
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
        actualArticleVolatileContent.removeEntity(articleId)
    }

    fun saveArticleContent(articleContent: ArticleContentDto) {
        if (!active) {
            return
        }
        actualArticleContentsContent.saveEntity(articleContent)
    }

    fun readArticleContent(articleId: Long): ArticleContentDto? {
        return actualArticleContentsContent.readEntity(articleId.toString())
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

    fun saveArticleLabel(articleLabel: ArticleLabelDto) {
        if (!active) {
            return
        }
        articleLabelsContent.saveEntity(articleLabel)
    }

    fun readAllArticleLabels(): List<ArticleLabelDto> {
        return articleLabelsContent.readAllEntities()
    }

    fun updateArticleLabel(articleLabel: ArticleLabelDto) {
        if (!active) {
            return
        }
        articleLabelsContent.updateEntity(articleLabel)
    }

    fun removeArticleLabel(articleLabelId: Long) {
        if (!active) {
            return
        }
        articleLabelsContent.removeEntity(articleLabelId.toString())
    }

    fun saveLabelToArticleConnection(labelToArticleConnection: LabelToArticleConnectionDto) {
        if (!active) {
            return
        }
        labelToArticleConnectionsContent.saveEntity(labelToArticleConnection)
    }

    fun readAllLabelToArticleConnections(): List<LabelToArticleConnectionDto> {
        return labelToArticleConnectionsContent.readAllEntities()
    }

    fun removeLabelToArticleConnection(articleToLabelConnectionId: Long) {
        if (!active) {
            return
        }
        labelToArticleConnectionsContent.removeEntity(articleToLabelConnectionId)
    }

    fun saveLabelsCombination(labelsCombinationDto: LabelsCombinationDto) {
        if (!active) {
            return
        }
        labelCombinationsInMemory[labelsCombinationDto.id] = labelsCombinationDto
        labelCombinationsContent.saveEntity(labelsCombinationDto)
    }

    fun readAllLabelCombinationsInMemory() {
        val labelsCombinationDtos = labelCombinationsContent.readAllEntities()
        labelCombinationsInMemory.clear()
        labelsCombinationDtos.forEach {
            labelCombinationsInMemory[it.id] = it
        }
    }

    fun updateLabelsCombinationPopularity(labelsCombinationId: Long, popularity: Long) {
        if (!active) {
            return
        }
        val labelsCombinationDto = labelCombinationsInMemory[labelsCombinationId]
        if (labelsCombinationDto != null) {
            labelsCombinationDto.popularity = popularity
            labelCombinationsContent.updateEntity(labelsCombinationDto)
        }
    }

    fun removeLabelsCombination(combinationId: Long) {
        if (!active) {
            return
        }
        labelCombinationsInMemory.remove(combinationId)
        labelCombinationsContent.removeEntity(combinationId)
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