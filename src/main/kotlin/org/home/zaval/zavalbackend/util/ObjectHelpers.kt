package org.home.zaval.zavalbackend.util

import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty0
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

class Copier<T : Any>(private val obj: T) {
    private val overriding: MutableMap<String, Any> = mutableMapOf()
    private val NULL_VALUE = "UNIQ_NULL_VALUE"

    inline fun copyWithOverriding(configurer: Copier<T>.() -> Unit): T {
        this.configurer()
        return createCopy()
    }

    infix fun <U> KProperty0<U>.overrideWith(value: U) {
        overriding[this.name] = value ?: NULL_VALUE
    }

    fun createCopy(): T {
        val clazz = obj::class
        val primaryConstructor = clazz.primaryConstructor
        val primaryArgs = primaryConstructor!!.parameters
            .asSequence()
            .map { it.name }
            .mapNotNull { name -> clazz.memberProperties.find { it.name == name } }
            .map {
                val possibleOverridingValue = overriding[it.name]
                if (possibleOverridingValue == NULL_VALUE) {
                    return@map null
                }
                possibleOverridingValue ?: it.getter.call(obj)
            }
            .toList()
            .toTypedArray()
        val newObj = primaryConstructor.call(*primaryArgs)
        return newObj
    }
}