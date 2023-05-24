package org.home.zaval.zavalbackend.store

import kotlin.properties.Delegates
import kotlin.reflect.KProperty

open class ObservableStore {
    // Real storage for values
    val valuesMap: MutableMap<String, Any?> = mutableMapOf()

    var isObservable = true

    var changesListener: ((Map<String, Any?>) -> Unit)? = null

    fun restoreOrInitializeValues(map: MutableMap<String, Any?>) {
        valuesMap.clear()
        map.entries.forEach { (key, value) -> valuesMap[key] = value }
    }
}

class ResourceDelegate<T> {

    operator fun getValue(thisRef: ObservableStore, property: KProperty<*>): T {
        return thisRef.valuesMap[property.name] as T
    }

    operator fun setValue(thisRef: ObservableStore, property: KProperty<*>, value: T) {
        thisRef.valuesMap[property.name] = value
        if (thisRef.isObservable) {
            thisRef.changesListener?.let { it(thisRef.valuesMap) }
        }
    }
}

/**
 * Use same delegate objects for all properties of all stores to minimize number of delegate objects.
 */
val observedBoolean = ResourceDelegate<Boolean>()
val observedObj = ResourceDelegate<Any>()
fun <T> observedValue(): ResourceDelegate<T> = observedObj as ResourceDelegate<T>

inline fun <T: ObservableStore> T.silentlyModify(filler: T.() -> Unit) {
    this.isObservable = false
    this.filler()
    this.isObservable = true
}

inline fun <T: ObservableStore> T.batchedModify(modifier: T.() -> Unit) {
    this.isObservable = false
    this.modifier()
    this.isObservable = true
    this.changesListener?.let { it(this.valuesMap) }
}