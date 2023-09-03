package org.home.zaval.zavalbackend.exception

class CircularTodoDependencyException(finalParentId: Long, movingTodoId: Long) :
    RuntimeException("Circular dependency. Trying to move parent todo (${movingTodoId}) to child of it (${finalParentId})")