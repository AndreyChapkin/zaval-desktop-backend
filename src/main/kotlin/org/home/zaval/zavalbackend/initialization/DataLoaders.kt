package org.home.zaval.zavalbackend.initialization

import org.home.zaval.zavalbackend.dto.todo.FullTodoDto
import org.home.zaval.zavalbackend.dto.todo.TodoHistoryDto
import org.home.zaval.zavalbackend.repository.TodoRepository
import org.home.zaval.zavalbackend.service.TodoService
import org.home.zaval.zavalbackend.persistence.ApplicationConfig
import org.home.zaval.zavalbackend.store.ApplicationConfigStore
import org.home.zaval.zavalbackend.persistence.JsonHelper
import org.home.zaval.zavalbackend.store.TodoStore
import org.home.zaval.zavalbackend.dto.persistence.AggregationInfoDto
import org.home.zaval.zavalbackend.persistence.DataArchiver
import org.home.zaval.zavalbackend.persistence.load
import org.home.zaval.zavalbackend.util.numberedFilenamesInDir
import org.home.zaval.zavalbackend.util.toEntity
import java.nio.file.Files
import java.nio.file.Paths
import java.util.LinkedList
import java.util.Queue
import kotlin.io.path.name

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

fun reserveCurrentData() {
    println(":::::::: Reserving current application data ::::::::")
    val storageDirPath = Paths.get(ApplicationConfigStore.config.storageDirectory)
    val dirToSaveArchivePath = Paths.get(ApplicationConfigStore.config.saveArchivesToDirectory)
    val zipName = storageDirPath.name
    // If there is archive already - rename it
    val existingArchivePath = dirToSaveArchivePath.resolve("$zipName.zip")
    if (Files.exists(existingArchivePath)) {
        println("There is used archive. Make it older.")
        val numberedArchiveNamesWithNumbers = numberedFilenamesInDir(dirToSaveArchivePath)
        val maxNumberedArchivesNumber = ApplicationConfigStore.config.maxArchivesNumber - 1
        if (numberedArchiveNamesWithNumbers.keys.size >= maxNumberedArchivesNumber) {
            // delete the oldest archive
            numberedArchiveNamesWithNumbers.keys.maxOrNull()?.let { oldestArchiveNumber ->
                val oldestArchivePath = dirToSaveArchivePath.resolve("$zipName-$oldestArchiveNumber.zip")
                println("Delete the oldest archive: $oldestArchivePath")
                Files.delete(oldestArchivePath)
                numberedArchiveNamesWithNumbers.remove(oldestArchiveNumber)
            }
        }
        // rename rest of files
        println("Rename all other archives to make them older.")
        numberedArchiveNamesWithNumbers.keys
            .sortedByDescending { it }
            .forEach { numberKey ->
                val prevArchivePath = dirToSaveArchivePath.resolve("$zipName-$numberKey.zip")
                val newArchivePath = dirToSaveArchivePath.resolve("$zipName-${numberKey + 1}.zip")
                Files.move(prevArchivePath, newArchivePath)
            }
        // add number to unnumbered archive name
        val newArchivePath = dirToSaveArchivePath.resolve("$zipName-1.zip")
        Files.move(existingArchivePath, newArchivePath)
    }
    println("Start creating reserve archive...")
    // Create new archive
    DataArchiver(
        storageDirAbsolutePath = storageDirPath,
        dirToSaveZipAbsolutePath = dirToSaveArchivePath,
        zipName
    ).createArchive()
    println("Archive created successfully!")
}

fun loadTodoTechnicalFiles() {
    println(":::::::: Todo loading ::::::::")
    println("Start loading persisted values...")
    val result = TodoStore.persistedValues.load {
        TodoStore.createDefaultPersistedValues()
    }
    println("Start loading aggregation info...")
    val aggregationInfoResult = TodoStore.aggregationInfo.load {
        AggregationInfoDto(childToParentIds = mutableMapOf(), parentToChildrenIds = mutableMapOf())
    }
    ("Todo aggregation info - ${aggregationInfoResult.result}")
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

fun loadTodoHistoryTechnicalFiles() {
    println(":::::::: Todo history technical files loading ::::::::")
    println("Start todo loading history technical files...")
    val results = TodoStore.todosHistoryContent.loadTechnicalFiles()
    println("Todo history content technical files:")
    results.forEach {
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

fun loadTodoHistories(): List<TodoHistoryDto> {
    println(":::::::: Saved todo histories loading ::::::::")
    println("Start loading...")
    val persistedHistoryDtos = TodoStore.readAllHistories()
    if (persistedHistoryDtos.isNotEmpty()) {
        println("+++ Todo histories loaded successfully!")
    } else {
        println("--- No saved todo histories.")
    }
    return persistedHistoryDtos
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

fun placeTodosHistoriesInMemory(historyDtos: List<TodoHistoryDto>, todoService: TodoService) {
    historyDtos.forEach {
        todoService.updateTodoHistory(it.todoId, it)
    }
}