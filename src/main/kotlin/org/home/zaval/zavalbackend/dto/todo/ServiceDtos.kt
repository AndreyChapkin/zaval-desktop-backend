package org.home.zaval.zavalbackend.dto.todo

class TodoParentPathDto(
    var todoId: Long,
    var parentIds: LinkedHashSet<Long>
)