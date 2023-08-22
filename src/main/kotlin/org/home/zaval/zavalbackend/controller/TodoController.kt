package org.home.zaval.zavalbackend.controller

import org.home.zaval.zavalbackend.dto.*
import org.home.zaval.zavalbackend.entity.value.TodoStatus
import org.home.zaval.zavalbackend.service.TodoService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/todo")
@CrossOrigin("http://localhost:5173")
class TodoController(
    val todoService: TodoService
) {
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun createTodo(@RequestBody createTodoDto: CreateTodoDto): ResponseEntity<LightTodoDto> {
        return ResponseEntity.ok(todoService.createTodo(createTodoDto))
    }

    @GetMapping("/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getLightTodo(@PathVariable("id") todoId: String): ResponseEntity<LightTodoDto?> {
        return ResponseEntity.ok(todoService.getLightTodo(todoId.toLong()))
    }

    @PatchMapping("/{id}", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun updateTodo(@PathVariable("id") todoId: String, @RequestBody updateTodoDto: UpdateTodoDto): ResponseEntity<LightTodoDto> {
        return ResponseEntity.ok(todoService.updateTodo(todoId.toLong(), updateTodoDto))
    }

    @DeleteMapping("/{id}")
    fun deleteTodo(@PathVariable("id") todoId: String): ResponseEntity<Unit> {
        todoService.deleteTodo(todoId.toLong())
        return ResponseEntity.ok(null)
    }

    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getAllTodos(@RequestParam("status", required = false) status: TodoStatus?): ResponseEntity<List<LightTodoDto>> {
        return ResponseEntity.ok(todoService.getAllTodos(status))
    }

    @GetMapping("with-name-fragment", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun findAllTodosByNameFragment(@RequestParam("name-fragment") nameFragment: String): ResponseEntity<List<LightTodoDto>> {
        return ResponseEntity.ok(todoService.findAllShallowTodosByNameFragment(nameFragment))
    }

    @GetMapping("prioritized-list", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getPrioritizedListOfTodosWithStatus(@RequestParam("status") status: TodoStatus): ResponseEntity<TodosListDto> {
        return ResponseEntity.ok(todoService.getPrioritizedListOfTodosWithStatus(status))
    }

    @GetMapping(value = ["/detailed", "/detailed/{id}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getDetailedTodo(@PathVariable("id", required = false) todoId: String?): ResponseEntity<DetailedTodoDto?> {
        return ResponseEntity.ok(todoService.getDetailedTodo(todoId?.toLong()))
    }

    @PatchMapping("/move", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun moveTodo(@RequestBody todo: MoveTodoDto): ResponseEntity<Unit> {
        todoService.moveTodo(todo)
        return ResponseEntity.ok(null)
    }

    @GetMapping("/{id}/history", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getTodoHistory(@PathVariable("id") todoId: String): ResponseEntity<TodoHistoryDto?> {
        return ResponseEntity.ok(todoService.getTodoHistory(todoId.toLong()))
    }

    @PatchMapping(
        "/{id}/history",
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun updateTodoHistory(
        @PathVariable("id") todoId: String,
        @RequestBody todoHistoryDto: TodoHistoryDto
    ): ResponseEntity<TodoHistoryDto> {
        return ResponseEntity.ok(todoService.updateTodoHistory(todoId.toLong(), todoHistoryDto))
    }
}