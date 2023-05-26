package org.home.zaval.zavalbackend.util

import org.home.zaval.zavalbackend.model.Todo
import org.home.zaval.zavalbackend.model.view.TodoHierarchy

fun Todo.toShallowHierarchy() = TodoHierarchy(id = this.id!!, name = this.name, status = this.status)