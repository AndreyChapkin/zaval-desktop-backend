package org.home.zaval.zavalbackend.initialization

import org.home.zaval.zavalbackend.dto.CreateTodoDto
import org.home.zaval.zavalbackend.dto.LightTodoDto
import org.home.zaval.zavalbackend.repository.TodoRepository
import org.home.zaval.zavalbackend.service.TodoService
import org.home.zaval.zavalbackend.util.dto.ApplicationConfig
import org.home.zaval.zavalbackend.util.dto.TodoPersistedValues
import org.home.zaval.zavalbackend.util.singleton.ApplicationConfigStore
import org.home.zaval.zavalbackend.util.singleton.JsonHelper
import org.home.zaval.zavalbackend.util.singleton.todo.TodoStore
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

fun loadTodoPersistedValuesAndOptimizationFiles() {
    println(":::::::: Todo persisted values loading ::::::::")
    println("Start loading values from ${TodoStore.resolve(TodoStore.PERSISTED_VALUES_FILENAME)}...")
    val persistedValues: TodoPersistedValues? = TodoStore.loadPersistedValues()
    if (persistedValues == null) {
        println("--- No persisted values. Use default values.")
        val defaultPersistedValues = TodoStore.createDefaultPersistedValues()
        TodoStore.persistedValues = defaultPersistedValues
        println(JsonHelper.serializeObjectPretty(defaultPersistedValues))
    } else {
        TodoStore.persistedValues = persistedValues
        println("+++ Persisted values are loaded successfully!")
    }
    println("Start loading todo indices from ${TodoStore.resolve(TodoStore.TODO_INDICES_FILENAME)}...")
    val todoIndices: MutableMap<Long, String>? = TodoStore.loadIndices()
    if (todoIndices == null) {
        println("--- No indices. Use default.")
        val defaultTodoIndices = TodoStore.createDefaultIndices()
        TodoStore.todoIndices = defaultTodoIndices
        println(JsonHelper.serializeObjectPretty(defaultTodoIndices))
    } else {
        TodoStore.todoIndices = todoIndices
        println("+++ Todo indices are loaded successfully!")
    }
}

fun loadTodos(): List<LightTodoDto> {
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

fun placeTodosInMainMemory(todos: List<LightTodoDto>, todoRepository: TodoRepository) {
    val ROOT_ID_PLACEHOLDER = Long.MIN_VALUE
    val parentChildrenIds: MutableMap<Long, MutableList<Long>> = mutableMapOf()
    val idAndDtos: MutableMap<Long, LightTodoDto> = mutableMapOf()
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