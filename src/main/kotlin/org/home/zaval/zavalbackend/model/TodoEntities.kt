package org.home.zaval.zavalbackend.model

import org.home.zaval.zavalbackend.model.value.TodoStatus
import javax.persistence.*

interface TodoShallowView {
    fun getId(): Long?
    fun getName(): String
    fun getStatus(): TodoStatus
    fun getParentId(): Long?
}

//class TodoShallowView(
//    var id: Long,
//    var name: String,
//    var status: TodoStatus,
//    var parentId: Long?
//)

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

// Take primary key of todoInstance as foreign key to use as primary key
@Entity
class TodoParentPath(
    @Id
    var id: Long?,
    @Lob
    @Column(length = 10000)
    var parentPath: String,
)

@Entity
class TodoHistory(
    @Id
    var id: Long? = null,
    @Lob
    @Column(length = 10000)
    var records: String,
)