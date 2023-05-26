package org.home.zaval.zavalbackend.repository

import org.home.zaval.zavalbackend.model.Todo
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface TodoRepository : CrudRepository<Todo, Long> {
    @Query("select t from Todo t where t.parent.id = :ID")
    fun getAllChildrenOf(@Param("ID") todoId: Long): List<Todo>
}