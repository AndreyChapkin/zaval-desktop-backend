package org.home.zaval.zavalbackend.repository

import org.home.zaval.zavalbackend.entity.*
import org.home.zaval.zavalbackend.entity.value.TodoStatus
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

// TODO: horrible projection decision
@Repository
interface TodoRepository : CrudRepository<Todo, Long> {
    @Query("select t from Todo t where t.parent.id = :ID")
    fun getAllChildrenOf(@Param("ID") todoId: Long): List<Todo>

    @Query("select t from Todo t where id in (select parentId from TodoParentPathSegment tpps where tpps.parentPath.id = :ID)")
    fun getAllParentsOf(@Param("ID") todoId: Long): List<Todo>

    @Query("select t from Todo t where t.parent is null")
    fun getAllTopTodos(): List<Todo>

    @Query("select id, name, status, priority, interacted_on as interactedOn, parent_id as parentId from Todo t where status = :STATUS", nativeQuery = true)
    fun getAllTodosWithStatus(@Param("STATUS") status: String): List<TodoLightView>

    @Query("select t from Todo t where id in (select id from TodoParentPath tpp where tpp.isLeave = true)")
    fun getAllLeavesTodos(): List<Todo>

    @Query("select id, name, status, priority, interacted_on as interactedOn, parent_id as parentId from Todo t", nativeQuery = true)
    fun getAllTodos(): List<TodoLightView>

    @Query("select id, name, status, priority, interacted_on as interactedOn, parent_id as parentId from todo t where id in :IDS", nativeQuery = true)
    fun getAllShallowTodosByIds(@Param("IDS") ids: Iterable<Long>): List<TodoLightView>

    @Query("select id, name, status, priority, interacted_on as interactedOn, parent_id as parentId from todo t where upper(name) like upper(:PATTERN)", nativeQuery = true)
    fun findAllShallowTodosByNameFragment(@Param("PATTERN") pattern: String): List<TodoLightView>
}

@Repository
interface TodoParentPathRepository : CrudRepository<TodoParentPath, Long> {

    @Query("select tpps from TodoParentPathSegment tpps where tpps.parentPath.id = :ID order by tpps.orderIndex")
    fun getOrderedParentPathSegments(@Param("ID") todoId: Long): List<TodoParentPathSegment>

    @Query("select tpp from TodoParentPath tpp left join fetch tpp.segments where tpp.id in (select t.id from Todo t where t.status = :STATUS)")
    fun getAllTodoParentPathsWithTodoStatus(@Param("STATUS") status: TodoStatus): List<TodoParentPath>

    @Query("select tpp from TodoParentPath tpp where tpp.isLeave = true and tpp.id in (select t.id from Todo t where t.status = :STATUS)")
    fun getAllLeavesTodoParentPathsWithTodoStatus(@Param("STATUS") status: TodoStatus): List<TodoParentPath>

    @Query("select tpp from TodoParentPath tpp where tpp.isLeave = true")
    fun getAllLeavesTodoParentPaths(): List<TodoParentPath>

//    @Query("select tb from TodoParentPath tb where tb.parentPath like :PATH_PATTERN")
    @Query("select tb from TodoParentPath tb where tb.id in (select s.parentPath.id from TodoParentPathSegment s where s.parentId = :PARENT_ID)")
    fun findAllLevelChildren(
        @Param("PARENT_ID") parentId: Long
    ): List<TodoParentPath>

    @Query("select s.parentPath.id from TodoParentPathSegment s where s.parentId = :PARENT_ID")
    fun findAllLevelChildrenIds(
        @Param("PARENT_ID") parentId: Long
    ): List<Long>

    @Modifying
    @Query("delete from TodoParentPathSegment t where t.id in :IDS")
    fun removeAllSegmentsByIds(
        @Param("IDS") segmentsIds: List<Long>
    )
}

@Repository
interface TodoHistoryRepository : CrudRepository<TodoHistory, Long> {

    @Modifying
    @Query("delete from TodoHistory th where th.id in :IDS")
    fun deleteAllForIds(@Param("IDS") todoIds: List<Long>)
}