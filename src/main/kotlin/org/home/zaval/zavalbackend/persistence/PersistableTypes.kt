package org.home.zaval.zavalbackend.persistence


class FileIsNotReadyException(filename: String) : RuntimeException("File is not ready $filename")

interface SavedMapElementSerializer<T> {
    fun serialize(obj: T): String
    fun deserialize(s: String): T
}

class LongSerializer : SavedMapElementSerializer<Long> {

    override fun serialize(obj: Long): String {
        return obj.toString()
    }

    override fun deserialize(s: String): Long {
        return s.toLong()
    }
}

class IntSerializer : SavedMapElementSerializer<Int> {

    override fun serialize(obj: Int): String {
        return obj.toString()
    }

    override fun deserialize(s: String): Int {
        return s.toInt()
    }
}

class StringSerializer : SavedMapElementSerializer<String> {

    override fun serialize(obj: String): String {
        return obj
    }

    override fun deserialize(s: String): String {
        return s
    }
}

class LongListSerializer : SavedMapElementSerializer<List<Long>> {

    private val ELEMENT_SEPARATOR = " , "

    override fun serialize(obj: List<Long>): String {
        return obj.joinToString(ELEMENT_SEPARATOR)
    }

    override fun deserialize(s: String): List<Long> {
        return s.split(ELEMENT_SEPARATOR).map { it.toLong() }
    }
}

class DoubleLinkedList<T>(elements: Collection<T>) {
    var head: DoubleLinkedListNode<T>? = null
    var tail: DoubleLinkedListNode<T>? = null
    val isEmpty: Boolean
        get() = head == null || tail == null

    init {
        elements.forEach {
            addToTail(it)
        }
    }

    fun addToTail(element: T): DoubleLinkedListNode<T> {
        val node = DoubleLinkedListNode(element)
        if (isEmpty) {
            head = node
        } else {
            val prevTail = tail!!
            node.standAfter(prevTail)
        }
        tail = node
        return node
    }

    fun moveToTail(node: DoubleLinkedListNode<T>) {
        if (head === node && node.previous != null) {
            head = node.previous
        }
        node.leaveAndJoinNeighbours()
        val prevTail = tail
        prevTail?.standBefore(node)
        tail = node
    }

    fun addToHead(element: T): DoubleLinkedListNode<T> {
        val node = DoubleLinkedListNode(element)
        if (isEmpty) {
            tail = node
        } else {
            val prevHead = head!!
            node.standBefore(prevHead)
        }
        head = node
        return node
    }

    fun moveToHead(node: DoubleLinkedListNode<T>) {
        if (tail === node && node.next != null) {
            tail = node.next
        }
        node.leaveAndJoinNeighbours()
        val prevHead = head
        prevHead?.standAfter(node)
        head = node
    }

    fun remove(node: DoubleLinkedListNode<T>) {
        if (tail === node) {
            tail = node.next
        }
        if (head === node) {
            head = node.previous
        }
        node.leaveAndJoinNeighbours()
    }

    fun clear() {
        tail = null
        head = null
    }
}

class DoubleLinkedListNode<T>(var value: T) {
    var next: DoubleLinkedListNode<T>? = null
    var previous: DoubleLinkedListNode<T>? = null

    fun standAfter(nextNode: DoubleLinkedListNode<T>?) {
        if (nextNode !== this) {
            next = nextNode
            if (nextNode != null) {
                nextNode.previous = this
            }
        }
    }

    fun standBefore(previousNode: DoubleLinkedListNode<T>?) {
        if (previousNode !== this) {
            previous = previousNode
            if (previousNode != null) {
                previousNode.next = this
            }
        }
    }

    fun leaveAndJoinNeighbours() {
        if (previous !== next) {
            previous?.next = next
            next?.previous = previous
        }
        previous = null
        next = null
    }
}