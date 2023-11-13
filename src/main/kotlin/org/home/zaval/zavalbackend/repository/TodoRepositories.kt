package org.home.zaval.zavalbackend.repository

import org.home.zaval.zavalbackend.entity.*
import org.home.zaval.zavalbackend.entity.value.TodoStatus
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

// TODO: horrible projection decision
@Repository
interface TodoRepository : PagingAndSortingRepository<Todo, Long> {
    @Query("select t from Todo t where t.parent.id = :ID")
    fun getAllChildrenOf(@Param("ID") todoId: Long): List<Todo>

    @Query("select t from Todo t where t.parent is null")
    fun getAllTopTodos(): List<Todo>

    @Query("select id, name, status, priority, interacted_on as interactedOn, parent_id as parentId from Todo t where status = :STATUS", nativeQuery = true)
    fun getAllTodosWithStatus(@Param("STATUS") status: String): List<TodoLightView>

    @Query("select id, name, status, priority, interacted_on as interactedOn, parent_id as parentId from Todo t", nativeQuery = true)
    fun getAllTodos(): List<TodoLightView>

    @Query("select id, name, status, priority, interacted_on as interactedOn, parent_id as parentId from todo t where id in :IDS", nativeQuery = true)
    fun getAllShallowTodosByIds(@Param("IDS") ids: Iterable<Long>): List<TodoLightView>

    @Query("select id, name, status, priority, interacted_on as interactedOn, parent_id as parentId from todo t where upper(name) like upper(:PATTERN)", nativeQuery = true)
    fun findAllShallowTodosByNameFragment(@Param("PATTERN") pattern: String): List<TodoLightView>

    @Query("select description from todo t where t.id = :ID", nativeQuery = true)
    fun getDescription(@Param("ID") todoId: Long): String
}

@Repository
interface TodoHistoryRepository : CrudRepository<TodoHistory, Long> {

    @Modifying
    @Query("delete from TodoHistory th where th.id in :IDS")
    fun deleteAllForIds(@Param("IDS") todoIds: List<Long>)
}