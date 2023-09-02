package org.home.zaval.zavalbackend.store

import org.home.zaval.zavalbackend.dto.FullTodoDto
import org.home.zaval.zavalbackend.util.dto.FilesInfoCache
import org.home.zaval.zavalbackend.util.dto.TodoPersistedValues
import org.home.zaval.zavalbackend.util.filesInTheDir
import org.home.zaval.zavalbackend.util.path
import org.home.zaval.zavalbackend.util.singleton.JsonHelper
import org.home.zaval.zavalbackend.util.singleton.StorageFileWorker
import java.nio.file.Path

object TodoStore {
    const val TODOS_DIR = "todos"
    const val ACTUAL_SUBDIR = "actual"
    const val OUTDATED_SUBDIR = "outdated"

    const val TODO_INDICES_FILENAME = "todo-indices.json"
    const val FILES_INFO_CACHE = "files-info-cache.json"
    const val OUTDATED_TODOS_FILENAME = "outdated-todos.txt"

    const val PERSISTED_VALUES_FILENAME = "persisted-values.json"

    const val MAX_TODOS_NUMBER_AT_FILE = 4
    const val TODOS_SEPARATOR = "\n::::::::::::::::\n"

    lateinit var filesInfoCache: FilesInfoCache
    lateinit var todoIndices: MutableMap<Long, String>
    lateinit var persistedValues: TodoPersistedValues

    var active = true

    fun getId(): Long {
        return persistedValues.idSequence++
    }

    fun saveOrUpdateTodo(todo: FullTodoDto) {
        if (!active) {
            return
        }
        val existingFilename = todoIndices[todo.id]
        var outdatedTodo: FullTodoDto? = null
        if (existingFilename != null) {
            // TodoEntity already was persisted. Update in file
            val savedTodos = readTodos(existingFilename)
            val updatedTodos = savedTodos.map {
                if (it.id != todo.id) {
                    it
                } else {
                    outdatedTodo = it
                    todo
                }
            }
            outdatedTodo?.let { saveOutdatedTodo(it) }
            writeTodos(updatedTodos, existingFilename)
            return
        }
        // TodoEntity was not persisted
        // Try to append to incomplete file
        val incompleteFileFilename = filesInfoCache.incompleteFilenames.keys.takeIf { it.size > 0 }?.first()
        if (incompleteFileFilename != null) {
            // if there is incomplete file
            appendTodo(todo, incompleteFileFilename)
            val newCount = filesInfoCache.incompleteFilenames[incompleteFileFilename]!! + 1
            if (newCount < MAX_TODOS_NUMBER_AT_FILE) {
                filesInfoCache.incompleteFilenames[incompleteFileFilename] = newCount
            } else {
                filesInfoCache.incompleteFilenames.remove(incompleteFileFilename)
            }
            todoIndices[todo.id] = incompleteFileFilename
        } else {
            // There is no incomplete file. Add todoEntity to new file
            val newFilename = newTodoFileName()
            writeTodos(listOf(todo), newFilename)
            filesInfoCache.incompleteFilenames[newFilename] = 1
            todoIndices[todo.id] = newFilename
        }
        saveFilesInfoCache()
        saveIndices()
    }

    fun removeTodo(todo: FullTodoDto) {
        if (!active) {
            return
        }
        val filename = todoIndices[todo.id]
        if (filename != null) {
            val savedTodos = readTodos(filename)
            var outdatedTodo: FullTodoDto? = null
            val updatedTodos = savedTodos.filter {
                if (it.id == todo.id) {
                    outdatedTodo = it
                    false
                } else {
                    true
                }
            }
            outdatedTodo?.let { saveOutdatedTodo(it) }
            writeTodos(updatedTodos, filename)
            // Manage excessive empty files and cache
            val newCount = filesInfoCache.incompleteFilenames
                .computeIfAbsent(filename) { MAX_TODOS_NUMBER_AT_FILE } - 1
            if (newCount < 1) {
                filesInfoCache.incompleteFilenames.remove(filename)
                StorageFileWorker.removeFile(resolve(filename))
            } else {
                filesInfoCache.incompleteFilenames[filename] = newCount
            }
            saveFilesInfoCache()
        }
    }

    fun createDefaultPersistedValues(): TodoPersistedValues {
        return TodoPersistedValues(idSequence = 0L)
    }

    fun createDefaultIndices(): MutableMap<Long, String> {
        return mutableMapOf()
    }

    fun createDefaultFilesInfoCache(): FilesInfoCache {
        return FilesInfoCache(mutableMapOf())
    }

    fun loadPersistedValues(): TodoPersistedValues? {
        return StorageFileWorker.readObjectFromFile(resolve(PERSISTED_VALUES_FILENAME))
    }

    fun savePersistedValues() {
        return StorageFileWorker.writeObjectToFile(
            persistedValues,
            resolve(PERSISTED_VALUES_FILENAME)
        )
    }

    fun loadFilesInfoCache(): FilesInfoCache? {
        return StorageFileWorker.readObjectFromFile(resolve(FILES_INFO_CACHE))
    }

    private fun saveFilesInfoCache() {
        StorageFileWorker.writeObjectToFile(filesInfoCache, resolve(FILES_INFO_CACHE))
    }

    fun loadIndices(): MutableMap<Long, String>? {
        return StorageFileWorker.readObjectFromFile(resolve(TODO_INDICES_FILENAME))
    }

    private fun saveIndices() {
        StorageFileWorker.writeObjectToFile(todoIndices, resolve(TODO_INDICES_FILENAME))
    }

    fun readTodos(filename: String): List<FullTodoDto> {
        val todosStr = StorageFileWorker.readFile(resolve(filename))
        if (todosStr != null) {
            return extractTodos(todosStr)
        }
        return emptyList()
    }

    fun readAllTodos(): List<FullTodoDto> {
        val result = mutableListOf<FullTodoDto>()
        filesInTheDir(resolve(ACTUAL_SUBDIR)).forEach {
            result.addAll(readTodos(it.fileName.toString()))
        }
        return result
    }

    private fun saveOutdatedTodo(todo: FullTodoDto) {
        val alreadyHasFile = StorageFileWorker.fileExists(resolve(OUTDATED_TODOS_FILENAME))
        if (alreadyHasFile) {
            appendTodo(todo, OUTDATED_TODOS_FILENAME)
        } else {
            writeTodos(listOf(todo), OUTDATED_TODOS_FILENAME)
        }
    }

    private fun writeTodos(todos: List<FullTodoDto>, filename: String) {
        val todosStr = mergeTodos(todos)
        StorageFileWorker.writeToFile(todosStr, resolve(filename))
    }

    private fun appendTodo(todo: FullTodoDto, filename: String) {
        val appendPortion = "$TODOS_SEPARATOR${JsonHelper.serializeObject(todo)}"
        StorageFileWorker.appendToFile(appendPortion, resolve(filename))
    }

    fun resolve(filename: String): Path {
        val enhancedFilename = when (filename) {
            TODOS_DIR -> TODOS_DIR
            OUTDATED_SUBDIR -> "$TODOS_DIR/$OUTDATED_SUBDIR"
            ACTUAL_SUBDIR -> "$TODOS_DIR/$ACTUAL_SUBDIR"
            TODO_INDICES_FILENAME, PERSISTED_VALUES_FILENAME, FILES_INFO_CACHE -> "$TODOS_DIR/$filename"
            OUTDATED_TODOS_FILENAME -> "$TODOS_DIR/$OUTDATED_SUBDIR/$filename"
            else -> "$TODOS_DIR/$ACTUAL_SUBDIR/$filename"
        }
        return StorageFileWorker.resolveRelative(enhancedFilename.path)
    }

    private fun newTodoFileName(): String {
        val todosDir = resolve(ACTUAL_SUBDIR)
        val fileNames = filesInTheDir(todosDir)
        val number = fileNames.size
        return "todos-$number.txt"
    }

    private fun extractTodos(str: String): List<FullTodoDto> {
        return str.split(TODOS_SEPARATOR).map {
            JsonHelper.deserializeObject<FullTodoDto>(it)
        }.toList()
    }

    private fun mergeTodos(todos: List<FullTodoDto>): String {
        return todos.map { JsonHelper.serializeObject(it) }
            .joinToString(TODOS_SEPARATOR)
    }
}