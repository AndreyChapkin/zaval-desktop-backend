package org.home.zaval.zavalbackend.entity

import org.home.zaval.zavalbackend.entity.value.TodoStatus
import org.home.zaval.zavalbackend.util.asUtc
import java.time.OffsetDateTime
import javax.persistence.*

@Entity
@Table(name = "TODOS")
class Todo(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "general_sequence_generator")
    @SequenceGenerator(name = "general_sequence_generator", sequenceName = "todo_seq", allocationSize = 1)
    var id: Long?,
    var name: String,
    var description: String = "",
    @Enumerated(EnumType.STRING)
    var status: TodoStatus,
    var priority: Int = 0,
    var createdOn: OffsetDateTime = OffsetDateTime.now().asUtc,
    var interactedOn: OffsetDateTime = OffsetDateTime.now().asUtc,
    @JoinColumn(name = "PARENT_ID")
    var parentId: Long?,
)

@Entity
@Table(name = "TODO_HISTORIES")
class TodoHistory(
    @Id
    var id: Long? = null,
    var records: String,
)