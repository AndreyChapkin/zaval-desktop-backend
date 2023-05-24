package org.home.zaval.zavalbackend.repository

import org.home.zaval.zavalbackend.model.Todo
import org.home.zaval.zavalbackend.model.value.TodoStatus
import org.home.zaval.zavalbackend.store.GlobalIdSequence
import org.home.zaval.zavalbackend.store.IdGenerator
import org.home.zaval.zavalbackend.store.TodoStore
import org.home.zaval.zavalbackend.store.silentlyModify
import org.home.zaval.zavalbackend.util.*
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
class TodoRepositoryLocal : TodoRepository, IdGenerator by GlobalIdSequence {

    var listenToChanges: Boolean = true

    val changeListener: (Map<String, Any?>) -> Unit = {
        if (listenToChanges) {
            println("@@@ detected changes in store.")
        }
    }

    final val store = TodoStore().apply {
        changesListener = this@TodoRepositoryLocal.changeListener
    }

    init {
        store.silentlyModify {
            todos = kotlin.run {
                val taskA = Todo(generateId(), name = "Task A", status = TodoStatus.BACKLOG)
                val taskAA = Todo(generateId(), name = "Task A-A", status = TodoStatus.BACKLOG, parentId = taskA.id)
                val taskB = Todo(generateId(), name = "Task B", status = TodoStatus.BACKLOG)
                val taskBA = Todo(generateId(), name = "Task B-A", status = TodoStatus.BACKLOG, parentId = taskB.id)
                mutableListOf(
                    taskA,
                    taskAA,
                    Todo(generateId(), name = "Task A-A-A", status = TodoStatus.BACKLOG, parentId = taskAA.id),
                    Todo(generateId(), name = "Task A-A-B", status = TodoStatus.BACKLOG, parentId = taskAA.id),
                    Todo(generateId(), name = "Task A-B", status = TodoStatus.BACKLOG, parentId = taskA.id),
                    taskB,
                    taskBA,
                    Todo(generateId(), name = "Task B-A-A", status = TodoStatus.BACKLOG, parentId = taskBA.id),
                    Todo(generateId(), name = "Task B-A-B", status = TodoStatus.BACKLOG, parentId = taskBA.id),
                    Todo(generateId(), name = "Task B-B", status = TodoStatus.BACKLOG, parentId = taskB.id),
                )
            }
        }
    }

    override fun getTodo(todoId: Long): Todo? {
        return store.todos.find { it.id == todoId }
    }

    override fun createTodos(todos: List<Todo>): List<Todo> {
        val resultTodos = mutableListOf<Todo>()
        for (todo in todos) {
            val result = todo.makeCopyWithOverriding {
                fill(Todo::id).withValue(generateId())
            }
            resultTodos.add(result)
        }
        store.todos = store.todos.copyAndAppendAll(resultTodos)
        return resultTodos
    }

    override fun updateTodos(todos: List<Todo>) {
        val resultTodos = mutableListOf<Todo>()
        for (todo in store.todos) {
            var resultTodo = todo
            val indexOfUpdatedTodo = todos.indexOf(todo)
            if (indexOfUpdatedTodo > -1) {
                resultTodo = todos[indexOfUpdatedTodo]
            }
            resultTodos.add(resultTodo)
        }
        store.todos = resultTodos
    }

    override fun deleteTodos(todoIds: List<Long>) {
        store.todos = store.todos.copyAndRemoveAll { todoIds.contains(it.id) }
    }

    override fun getAllTodo(): List<Todo> {
        return store.todos
    }

    override fun getAllChildrenOf(todoId: Long): List<Todo> {
        return store.todos.filter { it.parentId == todoId }
    }

    override fun getAllParentsOf(todoId: Long): List<Todo> {
        val parents = mutableListOf<Todo>()
        val todo = getTodo(todoId)
        if (todo != null) {
            if (todo.parentId != null) {
                var curParent = getTodo(todo.parentId)
                while (curParent != null) {
                    parents.add(curParent)
                    curParent = curParent.parentId?.let { getTodo(it) }
                }
            }
        }
        return parents
    }

    override fun isExist(todoId: Long): Boolean {
        return store.todos.indexOfFirst { it.id == todoId } > 0
    }

    override fun batched(modifier: () -> Unit) {
        this.listenToChanges = false
        modifier()
        this.listenToChanges = true
        changeListener(store.valuesMap)
    }
}
