package org.home.zaval.zavalbackend.util

import org.home.zaval.zavalbackend.exception.AlreadyInPersistenceContextException
import org.home.zaval.zavalbackend.exception.NotPersistedObjectException
import org.home.zaval.zavalbackend.exception.NotTrackedPersistableObjectModificationException
import org.home.zaval.zavalbackend.util.singleton.StorageFileWorker
import java.nio.file.Path

class PersistableObject<T : Any>(
    val filePath: Path,
) {
    lateinit var onlyAssignObj: T
    val readObj: T
        get() = onlyAssignObj
    val modObj: T
        get() {
            if (!inPersistenceContext) {
                throw NotTrackedPersistableObjectModificationException()
            }
            return onlyAssignObj
        }
    var inPersistenceContext = false
}

inline fun <reified T : Any> PersistableObject<T>.load() {
    this.onlyAssignObj = StorageFileWorker.readObjectFromFile(this.filePath)
        ?: throw NotPersistedObjectException(this.filePath)
}

inline fun ensurePersistence(persistableObj: PersistableObject<*>, changer: () -> Unit) {
    if (persistableObj.inPersistenceContext) {
        throw AlreadyInPersistenceContextException()
    }
    persistableObj.inPersistenceContext = true
    changer()
    StorageFileWorker.writeObjectToFile(persistableObj.onlyAssignObj, persistableObj.filePath)
    persistableObj.inPersistenceContext = false
}



inline fun ensurePersistenceForAll(vararg persistableObjs: PersistableObject<*>, changer: () -> Unit) {
    persistableObjs.forEach {
        if (it.inPersistenceContext) {
            throw AlreadyInPersistenceContextException()
        }
        it.inPersistenceContext = true
    }
    changer()
    persistableObjs.forEach {
        StorageFileWorker.writeObjectToFile(it.onlyAssignObj, it.filePath)
        it.inPersistenceContext = false
    }
}