package org.home.zaval.zavalbackend.util.singleton.todo

import org.home.zaval.zavalbackend.dto.LightTodoDto
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
    const val OUTDATED_TODOS_FILENAME = "outdated-todos.txt"

    const val PERSISTED_VALUES_FILENAME = "persisted-values.json"

    const val MAX_TODOS_NUMBER_AT_FILE = 4
    const val TODOS_SEPARATOR = "\n::::::::::::::::\n"

    lateinit var todoIndices: MutableMap<Long, String>
    lateinit var persistedValues: TodoPersistedValues

    fun getId(): Long {
        return persistedValues.idSequence++
    }

    fun saveOrUpdateTodo(todo: LightTodoDto) {
        val existingFilename = todoIndices[todo.id]
        var outdatedTodo: LightTodoDto? = null
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
        val incompleteFileFilename = todoIndices.values.let { filenames ->
            val counters: MutableMap<String, Int> = mutableMapOf()
            val incompleteFileFilenames: MutableSet<String> = mutableSetOf()
            filenames.forEach { filename ->
                val count = (counters[filename] ?: 0) + 1
                counters[filename] = count
                if (count < MAX_TODOS_NUMBER_AT_FILE) {
                    incompleteFileFilenames.add(filename)
                } else {
                    incompleteFileFilenames.remove(filename)
                }
            }
            // use the first one
            if (incompleteFileFilenames.size > 0) incompleteFileFilenames.first() else null
        }
        if (incompleteFileFilename != null) {
            // if there is incomplete file
            appendTodo(todo, incompleteFileFilename)
            todoIndices[todo.id] = incompleteFileFilename
            return
        }
        // There is no incomplete file. Add todoEntity to new file
        val newFilename = newTodoFileName()
        writeTodos(listOf(todo), newFilename)
        todoIndices[todo.id] = newFilename
        saveIndices()
    }

    fun removeTodo(todo: LightTodoDto) {
        val filename = todoIndices[todo.id]
        if (filename != null) {
            val savedTodos = readTodos(filename)
            var outdatedTodo: LightTodoDto? = null
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
        }
    }

    fun createDefaultPersistedValues(): TodoPersistedValues {
        return TodoPersistedValues(idSequence = 0L)
    }

    fun createDefaultIndices(): MutableMap<Long, String> {
        return mutableMapOf()
    }

    fun loadPersistedValues(): TodoPersistedValues? {
        return StorageFileWorker.readObjectFromFile(resolve(PERSISTED_VALUES_FILENAME))
    }

    fun loadIndices(): MutableMap<Long, String>? {
        return StorageFileWorker.readObjectFromFile(TODO_INDICES_FILENAME.path)
    }

    fun savePersistedValues() {
        return StorageFileWorker.writeObjectToFile(
            persistedValues,
            resolve(PERSISTED_VALUES_FILENAME)
        )
    }

    fun readTodos(filename: String): List<LightTodoDto> {
        val todosStr = StorageFileWorker.readFile(resolve(filename))
        if (todosStr != null) {
            return extractTodos(todosStr)
        }
        return emptyList()
    }

    fun readAllTodos(): List<LightTodoDto> {
        val result = mutableListOf<LightTodoDto>()
        filesInTheDir(resolve(ACTUAL_SUBDIR)).forEach {
            result.addAll(readTodos(it.fileName.toString()))
        }
        return result
    }

    private fun saveOutdatedTodo(todo: LightTodoDto) {
        val alreadyHasFile = StorageFileWorker.fileExists(resolve(OUTDATED_TODOS_FILENAME))
        if (alreadyHasFile) {
            appendTodo(todo, OUTDATED_TODOS_FILENAME)
        } else {
            writeTodos(listOf(todo), OUTDATED_TODOS_FILENAME)
        }
    }

    private fun saveIndices() {
        StorageFileWorker.writeObjectToFile(todoIndices, resolve(TODO_INDICES_FILENAME))
    }

    private fun writeTodos(todos: List<LightTodoDto>, filename: String) {
        val todosStr = mergeTodos(todos)
        StorageFileWorker.writeToFile(todosStr, resolve(filename))
    }

    private fun appendTodo(todo: LightTodoDto, filename: String) {
        val appendPortion = "$TODOS_SEPARATOR${JsonHelper.serializeObject(todo)}"
        StorageFileWorker.appendToFile(appendPortion, resolve(filename))
    }

    fun resolve(filename: String): Path {
        val enhancedFilename = when (filename) {
            TODOS_DIR -> TODOS_DIR
            OUTDATED_SUBDIR -> "$TODOS_DIR/$OUTDATED_SUBDIR"
            ACTUAL_SUBDIR -> "$TODOS_DIR/$ACTUAL_SUBDIR"
            TODO_INDICES_FILENAME, PERSISTED_VALUES_FILENAME -> "$TODOS_DIR/$filename"
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

    private fun extractTodos(str: String): List<LightTodoDto> {
        return str.split(TODOS_SEPARATOR).map {
            JsonHelper.deserializeObject<LightTodoDto>(it)
        }.toList()
    }

    private fun mergeTodos(todos: List<LightTodoDto>): String {
        return todos.map { JsonHelper.serializeObject(it) }
            .joinToString(TODOS_SEPARATOR)
    }
}