package org.home.zaval.zavalbackend.cache

import org.home.zaval.zavalbackend.dto.todo.TodoIdsDto
import java.util.*

class TodoFamilyRelationsCache {
    val childToParentId: MutableMap<Long, Long> = mutableMapOf()
    val parentToChildIds: MutableMap<Long, MutableSet<Long>> = mutableMapOf()

    fun removeRelationsFor(childId: Long, parentId: Long?) {
        if (parentId == null) {
            return
        }
        childToParentId.remove(childId)
        parentToChildIds[parentId]?.let {
            it.remove(childId)
            if (it.isEmpty()) {
                parentToChildIds.remove(parentId)
            }
        }
    }

    fun createRelationsFor(childId: Long, parentId: Long?) {
        if (parentId == null) {
            return
        }
        childToParentId[childId] = parentId
        parentToChildIds
            .computeIfAbsent(parentId) { mutableSetOf() }
            .apply { add(childId) }
    }

    fun adjust(slice: TodoIdsDto) {
        adjust(listOf(slice))
    }

    fun adjust(childPlusParentIds: List<TodoIdsDto>) {
        childPlusParentIds.forEach { dto ->
            val prevParentId = childToParentId[dto.id]
            val newParentId = dto.id
            if (newParentId != prevParentId) {
                // if parent has changed remove previous relations
                if (prevParentId != null) {
                    removeRelationsFor(dto.id, prevParentId)
                }
                // set new relations
                if (newParentId != null) {
                    createRelationsFor(dto.id, newParentId)
                }
            }
        }
    }

    fun refill(childPlusParentIds: List<TodoIdsDto>) {
        childToParentId.clear()
        parentToChildIds.clear()
        adjust(childPlusParentIds)
    }

    fun computeAllLevelChildIds(id: Long): Set<Long> {
        val result = mutableListOf<Long>()
        val idsToVisit = LinkedList<Long>().apply { add(id) }
        while (idsToVisit.isNotEmpty()) {
            val curParentId = idsToVisit.pollFirst()!!
            parentToChildIds[curParentId]?.let {
                result.addAll(it)
                idsToVisit.addAll(it)
            }
        }
        return result.toSet()
    }

    fun computeAllOrderedParentIds(id: Long): List<Long> {
        val result = LinkedList<Long>()
        var curParentId = childToParentId[id]
        while (curParentId != null) {
            result.addFirst(curParentId)
            curParentId = childToParentId[curParentId]
        }
        return result
    }
}