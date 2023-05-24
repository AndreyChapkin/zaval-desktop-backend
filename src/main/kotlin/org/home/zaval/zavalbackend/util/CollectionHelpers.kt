package org.home.zaval.zavalbackend.util

fun <T> List<T>.copyAndAppendAll(elements: Collection<T>): List<T> {
    val result = this.toMutableList()
    for (element in elements) {
        result.add(element)
    }
    return result
}

fun <T> List<T>.copyAndReplaceInPosition(position: Int, element: T): List<T> {
    val result = this.toMutableList()
    result[position] = element
    return result
}

fun <T> List<T>.copyAndRemoveAll(predicate: (T) -> Boolean): List<T> {
    val result = this.toMutableList()
    result.removeAll(predicate)
    return result
}