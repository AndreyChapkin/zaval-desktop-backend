package org.home.zaval.zavalbackend.model

import jakarta.persistence.*
import org.home.zaval.zavalbackend.model.value.TodoStatus

@Entity
class Todo(
    @Id
    @SequenceGenerator(name = "todo_generator", sequenceName = "hibernate_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "todo_generator")
    var id: Long?,
    var name: String,
    @Enumerated(EnumType.STRING)
    var status: TodoStatus,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PARENT_ID")
    var parent: Todo? = null,
)

class TodoHistory(
    id: Long,
    val actions: List<String>,
    val todoId: Long,
) : BaseEntity(id)