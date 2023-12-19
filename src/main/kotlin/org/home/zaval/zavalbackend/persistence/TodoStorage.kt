package org.home.zaval.zavalbackend.persistence

import org.home.zaval.zavalbackend.dto.todo.TodoDto

class TodoStorage : PersistableStorage<TodoDto>(
    storagePlacementPath = StorageFileWorker.globalStoragePath,
    storageDirName = "todo-storage",
    entityClass = TodoDto::class.java,
    maxEntitiesInFile = 10,
    reservationLevel = 3,
) {

}