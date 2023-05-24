package org.home.zaval.zavalbackend.controller

import org.home.zaval.zavalbackend.dto.MoveTodoDto
import org.home.zaval.zavalbackend.model.Todo
import org.home.zaval.zavalbackend.model.view.TodoHierarchy
import org.home.zaval.zavalbackend.service.TodoService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/todo")
class TodoController(
    val todoService: TodoService
) {
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun createTodos(@RequestBody todos: List<Todo>): ResponseEntity<List<Todo>> {
        return ResponseEntity.ok(todoService.createTodos(todos))
    }

    @GetMapping("/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getTodo(@PathVariable("id") todoId: String): ResponseEntity<Todo?> {
        return ResponseEntity.ok(todoService.getTodo(todoId.toLong()))
    }

    @PatchMapping("/{id}", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun updateTodo(@PathVariable("id") todoId: String, @RequestBody todo: Todo): ResponseEntity<Todo> {
        return ResponseEntity.ok(todoService.updateTodo(todoId.toLong(), todo))
    }

    @DeleteMapping("/{id}")
    fun deleteTodo(@PathVariable("id") todoId: String): ResponseEntity<Unit> {
        todoService.deleteTodos(listOf(todoId.toLong()))
        return ResponseEntity.ok(null)
    }

    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getAllTodos(): ResponseEntity<List<Todo>> {
        return ResponseEntity.ok(todoService.getAllTodos())
    }

    @GetMapping("/{id}/hierarchy", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getTodoHierarchy(@PathVariable("id") todoId: String): ResponseEntity<TodoHierarchy?> {
        return ResponseEntity.ok(todoService.getTodoBranch(todoId.toLong()))
    }

    @PatchMapping("/move", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun moveToParent(@RequestBody todo: MoveTodoDto): ResponseEntity<Unit> {
        todoService.moveToParent(todo)
        return ResponseEntity.ok(null)
    }
}