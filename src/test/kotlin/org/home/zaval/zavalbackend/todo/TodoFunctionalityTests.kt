package org.home.zaval.zavalbackend.todo

import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc

import org.home.zaval.zavalbackend.service.TodoService
import org.junit.jupiter.api.BeforeAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class TodoFunctionalityTests {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var todoService: TodoService

    @BeforeAll
    fun createTestTodos() {
        val createTodoDtos = createInitialTestTodos()
        val firstTodo = todoService.createTodo(createTodoDtos[0])
        val secondTodo = todoService.createTodo(
            createTodoDtos[1].copy(parentId = firstTodo.id)
        )
        val thirdTodo = todoService.createTodo(
            createTodoDtos[2].copy(parentId = secondTodo.id)
        )
        val fourthTodo = todoService.createTodo(createTodoDtos[3])
        val fifthTodo = todoService.createTodo(
            createTodoDtos[4].copy(parentId = fourthTodo.id)
        )
    }

    @Test
    @Throws(Exception::class)
    fun shouldReturnDefaultMessage() {
        this.mockMvc.perform(get("/api/todo"))
            .andDo {
                println("@@@ Result: " + it.response.contentAsString)
            }
            .andExpect(status().isOk())
//            .andExpect(content().string(containsString("Hello, World")))
    }
}