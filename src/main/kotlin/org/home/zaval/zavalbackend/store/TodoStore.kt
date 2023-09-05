package org.home.zaval.zavalbackend.store

import org.home.zaval.zavalbackend.dto.todo.FullTodoDto
import org.home.zaval.zavalbackend.dto.todo.TodoHistoryDto
import org.home.zaval.zavalbackend.exception.UnknownFileException
import org.home.zaval.zavalbackend.persistence.MultiFilePersistableObjects
import org.home.zaval.zavalbackend.persistence.PersistableObject
import org.home.zaval.zavalbackend.util.*
import org.home.zaval.zavalbackend.dto.persistence.AggregationInfoDto
import org.home.zaval.zavalbackend.dto.persistence.TodoPersistedValues
import org.home.zaval.zavalbackend.persistence.StorageFileWorker
import org.home.zaval.zavalbackend.persistence.ensurePersistence
import java.nio.file.Path
import java.util.LinkedList

object TodoStore {
    const val TODOS_DIR = "todos"
    const val HISTORY_SUBDIR = "history"
    const val ACTUAL_SUBDIR = "actual"
    const val OUTDATED_SUBDIR = "outdated"

    const val AGGREGATION_INFO_CACHE = "aggregation-info-cache.json"

    const val PERSISTED_VALUES_FILENAME = "persisted-values.json"

    val aggregationInfo = PersistableObject<AggregationInfoDto>(resolve(AGGREGATION_INFO_CACHE))
    val persistedValues = PersistableObject<TodoPersistedValues>(resolve(PERSISTED_VALUES_FILENAME))
    val todosHistoryContent = MultiFilePersistableObjects(
        relativeStorageRootDirPath = resolveRelative(HISTORY_SUBDIR),
        entityClass = TodoHistoryDto::class.java,
        idExtractor = { it.todoId },
    )
    val todosContent = MultiFilePersistableObjects(
        relativeStorageRootDirPath = resolveRelative(ACTUAL_SUBDIR),
        entityClass = FullTodoDto::class.java,
        idExtractor = { it.id },
    )
    val outdatedTodosContent = MultiFilePersistableObjects(
        relativeStorageRootDirPath = resolveRelative(OUTDATED_SUBDIR),
        entityClass = FullTodoDto::class.java,
        idExtractor = { it.id },
    )

    var active = true

    fun getId(): Long {
        var resultId = Long.MIN_VALUE
        ensurePersistence(persistedValues) {
            resultId = persistedValues.modObj.idSequence++
        }
        return resultId
    }

    fun readAllTodos(): List<FullTodoDto> {
        return todosContent.readAllEntities()
    }

    fun saveOrUpdateTodo(todo: FullTodoDto) {
        if (!active) {
            return
        }
        val outdatedTodo = todosContent.saveOrUpdateEntity(todo)
        updateAggregationInfo(todo)
        if (outdatedTodo != null) {
            outdatedTodosContent.saveOrUpdateEntity(outdatedTodo)
        }
    }

    fun removeTodo(todo: FullTodoDto) {
        if (!active) {
            return
        }
        val outdatedTodo: FullTodoDto? = todosContent.removeEntity(todo)
        removeAggregationInfo(todo)
        if (outdatedTodo != null) {
            outdatedTodosContent.saveOrUpdateEntity(outdatedTodo)
        }
    }

    fun readAllHistories(): List<TodoHistoryDto> {
        return todosHistoryContent.readAllEntities()
    }

    fun saveOrUpdateHistory(historyDto: TodoHistoryDto) {
        if (!active) {
            return
        }
        todosHistoryContent.saveOrUpdateEntity(historyDto)
    }

    fun removeHistory(historyDto: TodoHistoryDto) {
        if (!active) {
            return
        }
        todosHistoryContent.removeEntity(historyDto)
    }

    fun getAllParentsOf(todoId: Long): List<Long> {
        val result = LinkedList<Long>()
        var curParentId = aggregationInfo.readObj.childToParentIds[todoId]
        while (curParentId != null) {
            result.addFirst(curParentId)
            curParentId = aggregationInfo.readObj.childToParentIds[curParentId]
        }
        return result
    }

    fun getAllLevelChildrenOf(todoId: Long): List<Long> {
        val result = mutableListOf<Long>()
        val parentIdsToCheck = LinkedList<Long>().apply { add(todoId) }
        while (parentIdsToCheck.isNotEmpty()) {
            val curParentId = parentIdsToCheck.pollFirst()!!
            val curChildrenIds = aggregationInfo.readObj.parentToChildrenIds[curParentId]
            curChildrenIds?.let {
                result.addAll(it)
                parentIdsToCheck.addAll(it)
            }
        }
        return result
    }

    private fun removeAggregationInfo(todo: FullTodoDto) {
        val todoId = todo.id
        ensurePersistence(aggregationInfo) {
            // Try to remove info for entity and all level children
            val idsToDelete = LinkedList<Long>().apply { add(todoId) }
            while (idsToDelete.isNotEmpty()) {
                val curDeleteId = idsToDelete.pollFirst()!!
                // Remove all children list for this entity and plan them for deletion too
                val removedChildrenIds = aggregationInfo.modObj.parentToChildrenIds.remove(curDeleteId)
                if (removedChildrenIds != null) {
                    idsToDelete.addAll(removedChildrenIds)
                }
                // Remove child to parent index
                val prevParentId = aggregationInfo.readObj.childToParentIds.remove(curDeleteId)
                // Remove entity from children of its parent
                if (prevParentId != null) {
                    aggregationInfo.modObj.parentToChildrenIds[prevParentId]?.let { prevParentChildren ->
                        prevParentChildren.removeIf { it == curDeleteId }
                        if (prevParentChildren.size < 1) {
                            aggregationInfo.modObj.parentToChildrenIds.remove(prevParentId)
                        }
                    }
                }
            }
        }
    }

    private fun updateAggregationInfo(todo: FullTodoDto) {
        val todoId = todo.id
        val prevParentId = aggregationInfo.readObj.childToParentIds[todoId]
        val newParentId = todo.parentId
        if (newParentId == prevParentId) {
            // Parent was not changed
            return
        }
        ensurePersistence(aggregationInfo) {
            if (newParentId == null) {
                // No more parents at all
                aggregationInfo.modObj.childToParentIds.remove(todoId)
            } else {
                // Point to new parent
                aggregationInfo.modObj.childToParentIds[todoId] = newParentId
                // Add to new parent's children
                aggregationInfo.modObj.parentToChildrenIds
                    .computeIfAbsent(newParentId) { mutableListOf() }
                    .apply { add(todoId) }
            }
            // Try to remove from previous parent children
            aggregationInfo.modObj.parentToChildrenIds[prevParentId]?.let { prevParentChildren ->
                prevParentChildren.removeIf { it == todoId }
                // Clear parent to children info if there is no more children
                if (prevParentChildren.size < 1) {
                    aggregationInfo.modObj.parentToChildrenIds.remove(prevParentId)
                }
            }
        }
    }

    fun createDefaultPersistedValues(): TodoPersistedValues {
        return TodoPersistedValues(idSequence = 1L)
    }

    fun resolveRelative(filename: String): String {
        return when (filename) {
            TODOS_DIR -> TODOS_DIR

            ACTUAL_SUBDIR, OUTDATED_SUBDIR, HISTORY_SUBDIR,
            AGGREGATION_INFO_CACHE, PERSISTED_VALUES_FILENAME -> "$TODOS_DIR/$filename"

            else -> throw UnknownFileException(filename)
        }
    }

    fun resolve(filename: String): Path {
        val enhancedFilename = resolveRelative(filename)
        return StorageFileWorker.resolveRelative(enhancedFilename.path)
    }
}