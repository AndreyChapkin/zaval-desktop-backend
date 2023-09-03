package org.home.zaval.zavalbackend.entity

import org.home.zaval.zavalbackend.entity.value.TodoStatus
import org.home.zaval.zavalbackend.listener.TodoHistoryListener
import org.home.zaval.zavalbackend.listener.TodoListener
import org.home.zaval.zavalbackend.util.asUtc
import java.sql.Timestamp
import java.time.OffsetDateTime
import javax.persistence.*

interface TodoLightView {
    fun getId(): Long?
    fun getName(): String
    fun getStatus(): TodoStatus
    fun getPriority(): Int
    fun getInteractedOn(): Timestamp
    fun getParentId(): Long?
}

@Entity
@EntityListeners(TodoListener::class)
class Todo(
    @Id
    var id: Long?,
    @Column(length = 1000)
    var name: String,
    @Column(length = 10000)
    var description: String = "",
    @Enumerated(EnumType.STRING)
    var status: TodoStatus,
    var priority: Int = 0,
    var createdOn: OffsetDateTime = OffsetDateTime.now().asUtc,
    var interactedOn: OffsetDateTime = OffsetDateTime.now().asUtc,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PARENT_ID")
    var parent: Todo? = null,
)

@Entity
@EntityListeners(TodoHistoryListener::class)
class TodoHistory(
    @Id
    var id: Long? = null,
    @Column(length = 10000)
    var records: String,
)