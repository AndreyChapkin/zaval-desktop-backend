package org.home.zaval.zavalbackend.repository

import org.home.zaval.zavalbackend.cache.TodoFamilyRelationsCache
import org.home.zaval.zavalbackend.dto.todo.TodoIdsDto
import org.home.zaval.zavalbackend.dto.todo.toDto
import org.home.zaval.zavalbackend.entity.Todo
import org.springframework.stereotype.Component
import java.util.*

interface TodoComplexRepository {

    val repository: TodoRepository

    fun save(todo: Todo): Todo

    fun updateParent(id: Long, parentId: Long?)

    fun deleteById(id: Long): Set<Long>

    fun deleteAllById(ids: Collection<Long>): Set<Long>

    fun getOrderedParentIdsOf(id: Long): List<Long>

    fun getAllLevelChildrenIdsOf(id: Long): Set<Long>
}

@Component
class TodoCachedRepository(
    override val repository: TodoRepository,
) : TodoComplexRepository {

    private val cache: TodoFamilyRelationsCache = TodoFamilyRelationsCache()

    init {
        initializeCache()
    }

    override fun save(todo: Todo): Todo {
        val result = repository.save(todo)
        cache.adjust(TodoIdsDto(result.id!!, result.parentId))
        return result
    }

    override fun updateParent(id: Long, parentId: Long?) {
        repository.updateParent(id, parentId)
        cache.adjust(TodoIdsDto(id, parentId))
    }

    override fun deleteById(id: Long): Set<Long> {
        repository.deleteById(id)
        return correctExistenceInCache(listOf(id))
    }

    override fun deleteAllById(ids: Collection<Long>): Set<Long> {
        repository.deleteAllById(ids)
        return correctExistenceInCache(ids)
    }

    override fun getOrderedParentIdsOf(id: Long): List<Long> {
        return cache.computeAllOrderedParentIds(id)
    }

    override fun getAllLevelChildrenIdsOf(id: Long): Set<Long> {
        return cache.computeAllLevelChildIds(id)
    }

    private fun initializeCache() {
        val childPlusParentIds = repository.getAllChildPlusParentPairs()
        cache.refill(childPlusParentIds.map { it.toDto() })
    }

    private fun correctExistenceInCache(ids: Collection<Long>): Set<Long> {
        // collect all potentially removed ids
        val checkExistenceIds = ids.toSet() + ids.flatMap {
            cache.computeAllLevelChildIds(it)
        }
        val existingChildPlusParentIds = repository.getChildPlusParentPairsByIds(checkExistenceIds)
        val removedIds = checkExistenceIds.toSet() - existingChildPlusParentIds
            .map { it.getId() }
            .toSet()
        removedIds.forEach { removedId ->
            val prevParentOfRemovedId = cache.childToParentId[removedId]
            cache.removeRelationsFor(removedId, prevParentOfRemovedId)
        }
        return removedIds
    }
}