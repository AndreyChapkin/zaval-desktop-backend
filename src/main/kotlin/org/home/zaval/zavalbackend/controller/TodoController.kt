package org.home.zaval.zavalbackend.controller

import org.home.zaval.zavalbackend.dto.*
import org.home.zaval.zavalbackend.model.value.TodoStatus
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
    fun createTodo(@RequestBody createTodoDto: CreateTodoDto): ResponseEntity<TodoDto> {
        return ResponseEntity.ok(todoService.createTodo(createTodoDto))
    }

    @GetMapping("/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getTodo(@PathVariable("id") todoId: String): ResponseEntity<TodoDto?> {
        return ResponseEntity.ok(todoService.getTodo(todoId.toLong()))
    }

    @PatchMapping("/{id}", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun updateTodo(@PathVariable("id") todoId: String, @RequestBody todo: UpdateTodoDto): ResponseEntity<TodoDto> {
        return ResponseEntity.ok(todoService.updateTodo(todoId.toLong(), todo))
    }

    @DeleteMapping("/{id}")
    fun deleteTodo(@PathVariable("id") todoId: String): ResponseEntity<Unit> {
        todoService.deleteTodo(todoId.toLong())
        return ResponseEntity.ok(null)
    }

    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getAllTodos(@RequestParam("status", required = false) status: TodoStatus?): ResponseEntity<List<TodoDto>> {
        return ResponseEntity.ok(todoService.getAllTodos(status))
    }

    @GetMapping(value = ["/hierarchy", "/hierarchy/{id}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getTodoHierarchy(@PathVariable("id", required = false) todoId: String?): ResponseEntity<TodoHierarchyDto?> {
        return ResponseEntity.ok(todoService.getTodoBranch(todoId?.toLong()))
    }

    @PatchMapping("/move", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun moveTodo(@RequestBody todo: MoveTodoDto): ResponseEntity<Unit> {
        todoService.moveTodo(todo)
        return ResponseEntity.ok(null)
    }
}