package org.home.zaval.zavalbackend.model

import org.home.zaval.zavalbackend.model.value.TodoStatus
import javax.persistence.*

interface TodoShallowView {
    fun getId(): Long?
    fun getName(): String
    fun getStatus(): TodoStatus
    fun getParentId(): Long?
}

@Entity
class Todo(
    @Id
    @SequenceGenerator(name = "todo_generator", sequenceName = "hibernate_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "todo_generator")
    var id: Long?,
    @Column(length = 1000)
    var name: String,
    @Enumerated(EnumType.STRING)
    var status: TodoStatus,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PARENT_ID")
    var parent: Todo? = null,
)

/** Take primary key of todoInstance as foreign key to use as primary key */
@Entity
class TodoParentPath(
    @Id
    var id: Long?,
    @OneToMany(mappedBy = "parentPath", fetch = FetchType.EAGER, cascade = [CascadeType.ALL])
    @OrderBy("ORDER_INDEX")
    var segments: MutableList<TodoParentPathSegment>,
    var isLeave: Boolean,
)

@Entity
class TodoParentPathSegment(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "todo_generator")
    @SequenceGenerator(name = "todo_generator", sequenceName = "hibernate_sequence", allocationSize = 1)
    var id: Long? = null,
    @ManyToOne
    @JoinColumn(name = "PARENT_PATH_ID", nullable = false)
    var parentPath: TodoParentPath?,
    var parentId: Long,
    var orderIndex: Int,
)

@Entity
class TodoHistory(
    @Id
    var id: Long? = null,
    @Lob
    @Column(length = 10000)
    var records: String,
)