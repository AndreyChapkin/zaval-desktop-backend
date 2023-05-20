package org.home.zaval.zavalbackend.controller

import org.springframework.boot.ExitCodeGenerator
import org.springframework.boot.SpringApplication
import org.springframework.context.ApplicationContext
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/application")
class ApplicationController(
    val context: ApplicationContext
) {

    @GetMapping("/shutdown")
    fun shutdown() {
        val exitCode: Int = SpringApplication.exit(context, ExitCodeGenerator { 0 } as ExitCodeGenerator)
        System.exit(exitCode)
    }
}