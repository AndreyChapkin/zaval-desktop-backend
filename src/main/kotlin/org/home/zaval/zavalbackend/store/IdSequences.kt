package org.home.zaval.zavalbackend.store

interface IdGenerator {
    fun generateId(): Long
}

object GlobalIdSequence: IdGenerator {
    var sequence: Long = -100L
        get() {
            if (field < 0) {
                // TODO loading logic
                return 1
            }
            return field
        }

    override fun generateId(): Long = sequence++
}