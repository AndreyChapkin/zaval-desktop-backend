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

    @GetMapping("with-name-fragment", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun findAllTodosByNameFragment(@RequestParam("name-fragment") nameFragment: String): ResponseEntity<List<TodoDto>> {
        return ResponseEntity.ok(todoService.findAllShallowTodosByNameFragment(nameFragment))
    }

    @GetMapping("status-branches", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getAllTodoWithStatusBranches(@RequestParam("status") status: TodoStatus): ResponseEntity<List<TodoBranchDto>> {
        return ResponseEntity.ok(todoService.getBranchesWithStatus(status))
    }

    @GetMapping("prioritized-list", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getPrioritizedListOfTodosWithStatus(@RequestParam("status") status: TodoStatus): ResponseEntity<TodosListDto> {
        return ResponseEntity.ok(todoService.getPrioritizedListOfTodosWithStatus(status))
    }

    @GetMapping(value = ["/hierarchy", "/hierarchy/{id}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getTodoHierarchy(@PathVariable("id", required = false) todoId: String?): ResponseEntity<TodoHierarchyDto?> {
        return ResponseEntity.ok(todoService.getTodoHierarchy(todoId?.toLong()))
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