package org.home.zaval.zavalbackend.repository

import org.home.zaval.zavalbackend.store.LocalStore

open class RepositoryLocal {

    var listenToChanges: Boolean = true

    protected open val changeListener: (Map<String, Any?>) -> Unit = {
        if (listenToChanges) {
            println("@@@ detected changes in store.")
        }
    }

    protected val localStore = LocalStore().apply {
        changesListener = this@RepositoryLocal.changeListener
    }

    fun batched(modifier: () -> Unit) {
        this.listenToChanges = false
        modifier()
        this.listenToChanges = true
        changeListener(localStore.valuesMap)
    }
}