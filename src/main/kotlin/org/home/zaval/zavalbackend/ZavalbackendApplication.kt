package org.home.zaval.zavalbackend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class ZavalbackendApplication

fun main(args: Array<String>) {
	runApplication<ZavalbackendApplication>(*args)
}
