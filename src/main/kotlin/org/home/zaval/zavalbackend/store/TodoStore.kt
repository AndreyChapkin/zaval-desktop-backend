package org.home.zaval.zavalbackend.store

import org.home.zaval.zavalbackend.dto.todo.FullTodoDto
import org.home.zaval.zavalbackend.dto.todo.TodoParentPathDto
import org.home.zaval.zavalbackend.exception.UnknownFileException
import org.home.zaval.zavalbackend.util.*
import org.home.zaval.zavalbackend.util.dto.TodoPersistedValues
import org.home.zaval.zavalbackend.util.singleton.StorageFileWorker
import java.nio.file.Path

object TodoStore {
    const val TODOS_DIR = "todos"
    const val ACTUAL_SUBDIR = "actual"
    const val OUTDATED_SUBDIR = "outdated"

    const val AGGREGATION_INFO_CACHE = "aggregation-info-cache.json"

    const val PERSISTED_VALUES_FILENAME = "persisted-values.json"

    val aggregationInfo = PersistableObject<List<TodoParentPathDto>>(resolve(AGGREGATION_INFO_CACHE))
    val persistedValues = PersistableObject<TodoPersistedValues>(resolve(PERSISTED_VALUES_FILENAME))
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
        return persistedValues.readObj.idSequence++
    }

    fun saveOrUpdateTodo(todo: FullTodoDto) {
        if (!active) {
            return
        }
        val outdatedTodo = todosContent.saveOrUpdateEntity(todo)
        if (outdatedTodo != null) {
            outdatedTodosContent.saveOrUpdateEntity(outdatedTodo)
        }
    }

    fun removeTodo(todo: FullTodoDto) {
        if (!active) {
            return
        }
        val outdatedTodo: FullTodoDto? = todosContent.removeEntity(todo)
        if (outdatedTodo != null) {
            outdatedTodosContent.saveOrUpdateEntity(outdatedTodo)
        }
    }

    fun createDefaultPersistedValues(): TodoPersistedValues {
        return TodoPersistedValues(idSequence = 0L)
    }

    fun readAllTodos(): List<FullTodoDto> {
        return todosContent.readAllEntities()
    }

    fun resolveRelative(filename: String): String {
        return when (filename) {
            TODOS_DIR -> TODOS_DIR
            ACTUAL_SUBDIR -> "$TODOS_DIR/$ACTUAL_SUBDIR"
            OUTDATED_SUBDIR -> "$TODOS_DIR/$OUTDATED_SUBDIR"
            AGGREGATION_INFO_CACHE, PERSISTED_VALUES_FILENAME -> "$TODOS_DIR/$filename"
            else -> throw UnknownFileException(filename)
        }
    }

    fun resolve(filename: String): Path {
        val enhancedFilename = resolveRelative(filename)
        return StorageFileWorker.resolveRelative(enhancedFilename.path)
    }
}