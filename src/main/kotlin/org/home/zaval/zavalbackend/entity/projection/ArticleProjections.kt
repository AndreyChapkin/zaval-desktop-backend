package org.home.zaval.zavalbackend.entity.projection

import java.sql.Timestamp

interface ArticleLightProjection {
    fun getId(): Long
    fun getTitle(): String
    fun getInteractedOn(): Timestamp
}