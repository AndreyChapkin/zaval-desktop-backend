package org.home.zaval.zavalbackend.entity

import org.home.zaval.zavalbackend.entity.value.TodoStatus
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

//@Entity
//class TodoParentPath(
//    /** Is filled manually with id of an according todo_instance */
//    @Id
//    var id: Long?,
//    @OneToMany(mappedBy = "parentPath", fetch = FetchType.EAGER, cascade = [CascadeType.ALL])
//    @OrderBy("ORDER_INDEX")
//    var segments: MutableList<TodoParentPathSegment>,
//    var isLeave: Boolean,
//)
//
//@Entity
//class TodoParentPathSegment(
//    @Id
//    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "todo_generator")
//    @SequenceGenerator(name = "todo_generator", sequenceName = "hibernate_sequence", allocationSize = 1)
//    var id: Long? = null,
//    @ManyToOne
//    @JoinColumn(name = "PARENT_PATH_ID", nullable = false)
//    var parentPath: TodoParentPath?,
//    var parentId: Long,
//    var orderIndex: Int,
//)

@Entity
class TodoHistory(
    @Id
    var id: Long? = null,
    @Lob
    @Column(length = 10000)
    var records: String,
)