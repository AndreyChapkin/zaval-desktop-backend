package org.home.zaval.zavalbackend.controller

import org.springframework.boot.ExitCodeGenerator
import org.springframework.boot.SpringApplication
import org.springframework.context.ApplicationContext
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit


@RestController
@RequestMapping("/application")
class ApplicationController(
    val context: ApplicationContext
) {

    // TODO bad implementation. Need response and shutdown of all threads
    @GetMapping("/shutdown")
    fun shutdown() {
        val exitCode: Int = SpringApplication.exit(context, ExitCodeGenerator { 0 })
        System.exit(exitCode)
    }
}