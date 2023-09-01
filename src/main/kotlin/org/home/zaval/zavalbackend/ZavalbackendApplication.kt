package org.home.zaval.zavalbackend

import org.home.zaval.zavalbackend.initialization.loadConfig
import org.home.zaval.zavalbackend.initialization.loadTodoPersistedValuesAndOptimizationFiles
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ZavalbackendApplication

fun main(args: Array<String>) {
	runApplication<ZavalbackendApplication>(*args)
}
