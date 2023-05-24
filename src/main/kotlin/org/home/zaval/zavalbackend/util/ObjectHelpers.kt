package org.home.zaval.zavalbackend.util

import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

fun <T : Any> T.makeCopy() = Copier<T, Any?>(this).createCopy()
fun <T : Any> T.makeCopyWithOverriding(configurer: Copier<T, Any?>.() -> Unit) =
    Copier<T, Any?>(this).copyWithOverriding(configurer)

class Copier<T : Any, U>(private val obj: T) {
    private val overriding: MutableMap<String, Any> = mutableMapOf()
    private lateinit var overrideName: String
    private val NULL_VALUE = "UNIQ_NULL_VALUE"

    inline fun copyWithOverriding(configurer: Copier<T, U>.() -> Unit): T {
        this.configurer()
        return createCopy()
    }

    fun <R> fill(property: KProperty1<T, R>): Copier<T, R> {
        this.overrideName = property.name
        return this as Copier<T, R>
    }

    fun withValue(value: U) {
        this.overriding[this.overrideName] = value ?: this.NULL_VALUE
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