package org.home.zaval.zavalbackend.model

import com.fasterxml.jackson.annotation.JsonBackReference
import com.fasterxml.jackson.annotation.JsonManagedReference
import org.hibernate.annotations.Parameter
import org.home.zaval.zavalbackend.model.value.TodoStatus
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.Lob
import javax.persistence.ManyToOne
import javax.persistence.MapsId
import javax.persistence.OneToOne
import javax.persistence.PrimaryKeyJoinColumn
import javax.persistence.SequenceGenerator

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
    @OneToOne(mappedBy = "todo", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @JsonManagedReference
    var todoBranch: TodoParentPath? = null,
)

// Take primary key of todoInstance as foreign key to use as primary key
@Entity
class TodoParentPath(
    @Id
    @GeneratedValue(generator = "addressKeyGenerator")
    @org.hibernate.annotations.GenericGenerator(
        name = "addressKeyGenerator",
        strategy = "foreign",
        parameters = [Parameter(name = "property", value = "user")]
    )
    var id: Long?,
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @PrimaryKeyJoinColumn
    @JsonBackReference
    var todo: Todo,
    @Lob
    @Column(length = 10000)
    var parentPath: String,
)

class MetaBranchInfo(
    @Id
    @SequenceGenerator(name = "todo_generator", sequenceName = "hibernate_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "todo_generator")
    var id: Long?,
    var todoParentPath: TodoParentPath,
    var todo: Todo,
)

class TodoHistory(
    id: Long,
    val actions: List<String>,
    val todoId: Long,
) : BaseEntity(id)