package org.home.zaval.zavalbackend.model

data class Todo(
    val id: Long,
    val name: String,
    val status: TodoStatus,
    val parentId: Long? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Todo

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

data class TodoHierarchy(
    val id: Long,
    val name: String,
    val status: TodoStatus,
    var parent: TodoHierarchy? = null,
    var children: Array<TodoHierarchy>? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Todo

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

enum class TodoStatus {
    NEED_ATTENTION, ON_HOLD, IN_PROGRESS
}