package org.home.zaval.zavalbackend.todo

import org.home.zaval.zavalbackend.dto.CreateTodoDto
import org.home.zaval.zavalbackend.dto.MoveTodoDto
import org.home.zaval.zavalbackend.dto.TodoDto
import org.home.zaval.zavalbackend.model.value.TodoStatus
import org.home.zaval.zavalbackend.repository.TodoHistoryRepository
import org.home.zaval.zavalbackend.repository.TodoParentPathRepository
import org.home.zaval.zavalbackend.repository.TodoRepository
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

import org.home.zaval.zavalbackend.service.TodoService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TodoFunctionalityTests {

    @Autowired
    lateinit var todoService: TodoService

    @Autowired
    lateinit var todoRepository: TodoRepository

    @Autowired
    lateinit var todoParentPathRepository: TodoParentPathRepository

    @Autowired
    lateinit var todoHistoryRepository: TodoHistoryRepository

    val initTodosNameAndIdMap: MutableMap<String, Long> = mutableMapOf()

    @BeforeEach
    fun createTestTodos() {
        this.todoRepository.deleteAll()
        this.todoParentPathRepository.deleteAll()
        this.todoHistoryRepository.deleteAll()
        val createTodoDtos = createInitialTestTodos()
        val createdTodos = mutableListOf<TodoDto>()
        val firstTodo = todoService.createTodo(createTodoDtos[0]).also { createdTodos.add(it) }
        val secondTodo = todoService.createTodo(
            createTodoDtos[1].copy(parentId = firstTodo.id)
        ).also { createdTodos.add(it) }
        val thirdTodo = todoService.createTodo(
            createTodoDtos[2].copy(parentId = secondTodo.id)
        ).also { createdTodos.add(it) }
        val fourthTodo = todoService.createTodo(createTodoDtos[3]).also { createdTodos.add(it) }
        val fifthTodo = todoService.createTodo(
            createTodoDtos[4].copy(parentId = fourthTodo.id)
        ).also { createdTodos.add(it) }
        createdTodos.forEach {
            initTodosNameAndIdMap[it.name] = it.id
        }
    }

    @Test
    fun moveTodoAndSeeParentIdsOfAnotherChildTodo() {
        // move
        val allTodos = this.todoService.getAllTodos(null)
        val movingTodoId = initTodosNameAndIdMap["First"]!!
        val newParentTodoId = initTodosNameAndIdMap["Fifth"]!!
        val checkChildTodoId = initTodosNameAndIdMap["Third"]!!
        val moveTodoDto = MoveTodoDto(todoId = movingTodoId, parentId = newParentTodoId)
        this.todoService.moveTodo(moveTodoDto)
        // check parents
        val expectedParentIdsChain =
            listOf("Fourth", "Fifth", "First", "Second").mapNotNull { initTodosNameAndIdMap[it] }
        val actualParentIdsChain = this.todoService
            .getTodoHierarchy(checkChildTodoId)!!
            .parents.map { it.id }
        // assert
        Assertions.assertEquals(expectedParentIdsChain, actualParentIdsChain)
    }

    @Test
    fun createTodosAndSeeHierarchy() {
        // create main todo_instance
        val createNewDto = CreateTodoDto(
            name = "New",
            status = TodoStatus.BACKLOG,
            parentId = initTodosNameAndIdMap["Third"]
        )
        val createdTodoDto = this.todoService.createTodo(createNewDto)
        // create children
        val createdFirstChildTodoDto = this.todoService.createTodo(
            CreateTodoDto(
                name = "New First Child",
                status = TodoStatus.BACKLOG,
                parentId = createdTodoDto.id
            )
        )
        val createdSecondChildTodoDto = this.todoService.createTodo(
            CreateTodoDto(
                name = "New Second Child",
                status = TodoStatus.BACKLOG,
                parentId = createdTodoDto.id
            )
        )
        // fetch new todo_instance hierarchy
        val createdTodoHierarchyDto = this.todoService.getTodoHierarchy(createdTodoDto.id)!!
        // assert
        // parents path is full
        val expectedParentIdsChain =
            listOf("First", "Second", "Third").mapNotNull { initTodosNameAndIdMap[it] }
        val actualParentIdsChain = createdTodoHierarchyDto.parents.map { it.id }
        Assertions.assertEquals(expectedParentIdsChain, actualParentIdsChain)
        // children are presented
        val expectedChildrenIdsSet = setOf(createdFirstChildTodoDto.id, createdSecondChildTodoDto.id)
        val actualChildrenIdsSet = createdTodoHierarchyDto.children!!.map { it.id }.toSet()
        Assertions.assertEquals(expectedChildrenIdsSet, actualChildrenIdsSet)
    }
}