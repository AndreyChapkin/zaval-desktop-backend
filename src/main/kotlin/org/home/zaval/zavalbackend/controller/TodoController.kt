package org.home.zaval.zavalbackend.controller

import org.home.zaval.zavalbackend.dto.todo.*
import org.home.zaval.zavalbackend.entity.value.TodoStatus
import org.home.zaval.zavalbackend.service.TodoService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@RestController
@RequestMapping("/api/todo")
@CrossOrigin("http://localhost:5173", "http://localhost:3000")
class TodoController(
    val todoService: TodoService
) {
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun createTodo(@RequestBody createTodoDto: TodoCreateDto): ResponseEntity<TodoLightDto> {
        return ResponseEntity.ok(todoService.createTodo(createTodoDto))
    }

    @GetMapping("/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getLightTodo(@PathVariable("id") todoId: String): ResponseEntity<TodoLightDto?> {
        return ResponseEntity.ok(todoService.getLightTodo(todoId.toLong()))
    }

    @PatchMapping("/{id}", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun updateTodo(
        @PathVariable("id") todoId: String,
        @RequestBody updateTodoDto: TodoUpdateDto
    ): ResponseEntity<TodoLightDto> {
        return ResponseEntity.ok(todoService.updateTodo(todoId.toLong(), updateTodoDto))
    }

    @DeleteMapping("/{id}")
    fun deleteTodo(@PathVariable("id") todoId: String): ResponseEntity<Unit> {
        todoService.deleteTodo(todoId.toLong())
        return ResponseEntity.ok(null)
    }

    @GetMapping("/recent", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getTheMostDatedLightTodos(
        @RequestParam("count") count: String?,
        @RequestParam("orderType") orderType: String?
    ): ResponseEntity<List<TodoLightDto>> {
        return ResponseEntity.ok(todoService.getTheMostDatedTodoLights(count?.toInt(), orderType))
    }

    @GetMapping("/with-name-fragment", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun findAllTodosWithNameFragment(@RequestParam("name-fragment") nameFragment: String): ResponseEntity<List<TodoLightDto>> {
        val decodedFragment = URLDecoder.decode(nameFragment, StandardCharsets.UTF_8)
        return ResponseEntity.ok(todoService.findAllTodoLightsWithNameFragment(decodedFragment))
    }

    @PostMapping(
        "/prioritized-list",
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun getPrioritizedListOfTodos(@RequestBody paramsBody: Map<String, List<Long>>): ResponseEntity<TodoLeavesAndBranchesDto> {
        val todoIds = paramsBody["todoIds"] ?: emptyList()
        return ResponseEntity.ok(todoService.getPrioritizedListOfTodos(todoIds))
    }

    @GetMapping("/prioritized-list/{status}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getPrioritizedListOfTodosWithStatus(@PathVariable("status") status: TodoStatus): ResponseEntity<TodoLeavesAndBranchesDto> {
        return ResponseEntity.ok(todoService.getPrioritizedListOfTodosWithStatus(status))
    }

    @GetMapping("/root", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getRootTodos(): ResponseEntity<List<TodoLightDto>> {
        return ResponseEntity.ok(todoService.getRootTodos())
    }

    @GetMapping("/family/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getTodoFamily(@PathVariable("id", required = false) todoId: String?): ResponseEntity<TodoFamilyDto?> {
        return ResponseEntity.ok(todoService.getTodoFamily(todoId?.toLong()))
    }

    @PatchMapping("/move", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun moveTodo(@RequestBody todo: TodoMoveDto): ResponseEntity<Unit> {
        todoService.moveTodo(todo)
        return ResponseEntity.ok(null)
    }
}