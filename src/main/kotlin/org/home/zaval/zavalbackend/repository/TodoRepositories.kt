package org.home.zaval.zavalbackend.repository

import org.home.zaval.zavalbackend.entity.*
import org.home.zaval.zavalbackend.entity.projection.TodoIdsProjection
import org.home.zaval.zavalbackend.entity.projection.TodoLightProjection
import org.home.zaval.zavalbackend.entity.value.TodoStatus
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime

@Repository
interface TodoRepository : PagingAndSortingRepository<Todo, Long> {
    @Query("select t from Todo t where parentId = :ID")
    fun getAllChildrenOf(@Param("ID") todoId: Long): List<Todo>

    @Query("select t from Todo t where t.parentId is null")
    fun getAllRootTodos(): List<Todo>

    @Query(
        "select id, name, status, priority, interacted_on as interactedOn, parent_id as parentId from TODOS where status = :STATUS",
        nativeQuery = true
    )
    fun getAllTodosWithStatus(@Param("STATUS") status: String): List<TodoLightProjection>

    @Query(
        "select id, name, status, priority, interacted_on as interactedOn, parent_id as parentId from TODOS where id in :IDS",
        nativeQuery = true
    )
    fun getAllTodoLightsByIds(@Param("IDS") ids: Iterable<Long>): List<TodoLightProjection>

    @Query(
        "select id, name, status, priority, interacted_on as interactedOn, parent_id as parentId from TODOS t where upper(name) like upper(:PATTERN)",
        nativeQuery = true
    )
    fun findAllTodoLightsWithNameFragment(@Param("PATTERN") pattern: String): List<TodoLightProjection>

    @Query("select id, parent_id as parentId from TODOS", nativeQuery = true)
    fun getAllChildPlusParentPairs(): List<TodoIdsProjection>

    @Query("select id, parent_id as parentId from TODOS where id in :ids", nativeQuery = true)
    fun getChildPlusParentPairsByIds(ids: Collection<Long>): List<TodoIdsProjection>

    @Query("select description from TODOS where id = :ID", nativeQuery = true)
    fun getDescription(@Param("ID") todoId: Long): String

    @Modifying
    @Query("UPDATE TODOS SET status = :status WHERE id = :id", nativeQuery = true)
    fun updateStatus(id: Long, status: String)

    @Modifying
    @Query("UPDATE TODOS SET parent_id = :parentId WHERE id = :id", nativeQuery = true)
    fun updateParent(@Param("id") id: Long, @Param("parentId") parentId: Long?)

    @Modifying
    @Query("UPDATE TODOS SET interacted_on = :interactionDate WHERE id = :id", nativeQuery = true)
    fun updateInteractedOn(@Param("id") id: Long, @Param("interactionDate") interactionDate: OffsetDateTime)

    @Modifying
    @Query("UPDATE TODOS SET description = :description WHERE id = :id", nativeQuery = true)
    fun updateDescription(id: Long, description: String)
}