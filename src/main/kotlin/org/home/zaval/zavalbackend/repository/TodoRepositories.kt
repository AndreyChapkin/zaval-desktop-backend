package org.home.zaval.zavalbackend.repository

import org.home.zaval.zavalbackend.model.Todo
import org.home.zaval.zavalbackend.model.TodoHistory
import org.home.zaval.zavalbackend.model.TodoParentPath
import org.home.zaval.zavalbackend.model.TodoShallowView
import org.home.zaval.zavalbackend.model.value.TodoStatus
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import javax.persistence.SqlResultSetMapping

// TODO: horrible projection decision
@Repository
interface TodoRepository : CrudRepository<Todo, Long> {
    @Query("select t from Todo t where t.parent.id = :ID")
    fun getAllChildrenOf(@Param("ID") todoId: Long): List<Todo>

    @Query("select t from Todo t where t.parent is null")
    fun getAllTopTodos(): List<Todo>

    @Query("select id, name, status, parent_id as parentId from Todo t where status = :STATUS", nativeQuery = true)
    fun getAllTodosWithStatus(@Param("STATUS") status: String): List<TodoShallowView>

    @Query("select t from Todo t where id in (select id from TodoParentPath tpp where tpp.isLeave = true)")
    fun getAllLeavesTodos(): List<Todo>

    @Query("select id, name, status, parent_id as parentId from Todo t", nativeQuery = true)
    fun getAllTodos(): List<TodoShallowView>
}

@Repository
interface TodoBranchRepository : CrudRepository<TodoParentPath, Long> {

    @Query("select parentPath from TodoParentPath tb where tb.id = :ID")
    fun getParentsPath(@Param("ID") todoId: Long): String

    @Query("select tpp from TodoParentPath tpp where tpp.isLeave = true and tpp.id in (select t.id from Todo t where t.status = :STATUS)")
    fun getAllLeavesTodoParentPathsWithTodoStatus(@Param("STATUS") status: TodoStatus): List<TodoParentPath>

    @Query("select tpp from TodoParentPath tpp where tpp.isLeave = true")
    fun getAllLeavesTodoParentPaths(): List<TodoParentPath>

    @Query("select tb from TodoParentPath tb where tb.parentPath like :PATH_PATTERN")
    fun findAllLevelChildren(
        @Param("PATH_PATTERN") pathPattern: String
    ): List<TodoParentPath>
}

@Repository
interface TodoHistoryRepository : CrudRepository<TodoHistory, Long> {

    @Modifying
    @Query("delete from TodoHistory th where th.id in :IDS")
    fun deleteAllForIds(@Param("IDS") todoIds: List<Long>)
}