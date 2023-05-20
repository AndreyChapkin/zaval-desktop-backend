package org.home.zaval.zavalbackend.store

import org.home.zaval.zavalbackend.model.Todo
import org.home.zaval.zavalbackend.model.TodoStatus
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class TodoStore {
    companion object {
        val todoRoot: Todo = Todo(id = -1000, name= "Root", status = TodoStatus.ON_HOLD)
    }

    private var idSeq = 0L
    private val todos: MutableList<Todo> = kotlin.run {
        val taskA = Todo(idSeq++, name = "Task A", status = TodoStatus.ON_HOLD)
        val taskAA = Todo(idSeq++, name = "Task A-A", status = TodoStatus.ON_HOLD, parentId = taskA.id)
        val taskB = Todo(idSeq++, name = "Task B", status = TodoStatus.ON_HOLD)
        val taskBA = Todo(idSeq++, name = "Task B-A", status = TodoStatus.ON_HOLD, parentId = taskB.id)
        mutableListOf(
            taskA,
            taskAA,
            Todo(idSeq++, name = "Task A-A-A", status = TodoStatus.ON_HOLD, parentId = taskAA.id),
            Todo(idSeq++, name = "Task A-A-B", status = TodoStatus.ON_HOLD, parentId = taskAA.id),
            Todo(idSeq++, name = "Task A-B", status = TodoStatus.ON_HOLD, parentId = taskA.id),
            taskB,
            taskBA,
            Todo(idSeq++, name = "Task B-A-A", status = TodoStatus.ON_HOLD, parentId = taskBA.id),
            Todo(idSeq++, name = "Task B-A-B", status = TodoStatus.ON_HOLD, parentId = taskBA.id),
            Todo(idSeq++, name = "Task B-B", status = TodoStatus.ON_HOLD, parentId = taskB.id),
        )
    }

    fun getAllTodo(): List<Todo> {
        return todos
    }

    fun getTodo(todoId: Long): Todo? {
        return todos.find { it.id == todoId }
    }

    fun createTodo(todo: Todo): Todo {
        val result = todo.copy(id = idSeq++)
        todos.add(result)
        return result
    }

    fun updateTodo(todo: Todo) {
        val updateIndex = todos.indexOf(todo)
        todos[updateIndex] = todo
    }

    fun deleteTodo(todoId: Long) {
        todos.removeIf { it.id == todoId }
    }
}