package org.home.zaval.zavalbackend.util

import org.home.zaval.zavalbackend.dto.TodoDto
import org.home.zaval.zavalbackend.model.Todo
import org.home.zaval.zavalbackend.dto.TodoHierarchyDto

fun Todo.toShallowHierarchy() = TodoHierarchyDto(id = this.id!!, name = this.name, status = this.status)

// TODO get rid of
fun Todo.toDto() = TodoDto(
    id = this.id!!,
    name = this.name,
    status = this.status,
    parentId = this.parent?.id
)