package org.home.zaval.zavalbackend.initialization

import org.home.zaval.zavalbackend.dto.todo.FullTodoDto
import org.home.zaval.zavalbackend.exception.NotPersistedObjectException
import org.home.zaval.zavalbackend.repository.TodoRepository
import org.home.zaval.zavalbackend.util.dto.ApplicationConfig
import org.home.zaval.zavalbackend.store.ApplicationConfigStore
import org.home.zaval.zavalbackend.util.singleton.JsonHelper
import org.home.zaval.zavalbackend.store.TodoStore
import org.home.zaval.zavalbackend.util.load
import org.home.zaval.zavalbackend.util.toEntity
import java.util.LinkedList
import java.util.Queue

fun loadConfig() {
    println(":::::::: Config loading ::::::::")
    println("Start loading config from ${ApplicationConfigStore.CONFIG_FILE_PATH}...")
    val config: ApplicationConfig? = ApplicationConfigStore.loadApplicationConfig()
    if (config == null) {
        println("No config file. Use default config.")
        val defaultConfig = ApplicationConfigStore.createDefaultConfig()
        ApplicationConfigStore.config = defaultConfig
        println(JsonHelper.serializeObjectPretty(defaultConfig))
    } else {
        ApplicationConfigStore.config = config
        println("Config is loaded successfully!")
    }
}

fun loadTodoTechnicalFiles() {
    println(":::::::: Todo loading ::::::::")
    println("Start loading persisted values...")
    val result = TodoStore.persistedValues.load {
        TodoStore.createDefaultPersistedValues()
    }
    ("Todo persisted values - ${result.result}")
    println("Start loading todo technical files...")
    val results = TodoStore.todosContent.loadTechnicalFiles()
    println("Todo content technical files:")
    results.forEach {
        println(it)
    }
    println("Start loading outdated todo technical files...")
    val outdatedResults = TodoStore.outdatedTodosContent.loadTechnicalFiles()
    println("Outdated todo content technical files:")
    outdatedResults.forEach {
        println(it)
    }
}

fun loadTodos(): List<FullTodoDto> {
    println(":::::::: Saved todos loading ::::::::")
    println("Start loading...")
    val persistedTodos = TodoStore.readAllTodos()
    if (persistedTodos.isNotEmpty()) {
        println("+++ Todos loaded successfully!")
    } else {
        println("--- No saved todos.")
    }
    return persistedTodos
}

fun placeTodosInMainMemory(todos: List<FullTodoDto>, todoRepository: TodoRepository) {
    val ROOT_ID_PLACEHOLDER = Long.MIN_VALUE
    val parentChildrenIds: MutableMap<Long, MutableList<Long>> = mutableMapOf()
    val idAndDtos: MutableMap<Long, FullTodoDto> = mutableMapOf()
    todos.forEach {
        idAndDtos[it.id] = it
        val resultParentId = it.parentId ?: ROOT_ID_PLACEHOLDER
        val childrenIds = parentChildrenIds.computeIfAbsent(resultParentId) { mutableListOf() }
        childrenIds.add(it.id)
    }
    val queueToSave: Queue<Long> = LinkedList<Long>().apply {
        addAll(parentChildrenIds[ROOT_ID_PLACEHOLDER] ?: emptyList())
    }
    while (queueToSave.isNotEmpty()) {
        val curId = queueToSave.poll()
        val curDto = idAndDtos[curId]!!
        todoRepository.save(curDto.toEntity().apply { id = curDto.id })
        parentChildrenIds[curId]?.let {
            queueToSave.addAll(it)
        }
    }
}