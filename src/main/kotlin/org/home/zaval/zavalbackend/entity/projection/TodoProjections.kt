package org.home.zaval.zavalbackend.entity.projection

import org.home.zaval.zavalbackend.entity.value.TodoStatus
import java.sql.Timestamp

interface TodoLightProjection {
    fun getId(): Long
    fun getName(): String
    fun getStatus(): TodoStatus
    fun getPriority(): Int
    fun getInteractedOn(): Timestamp
    fun getParentId(): Long?
}

interface TodoIdsProjection {
    fun getId(): Long
    fun getParentId(): Long?
}