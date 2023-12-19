package org.home.zaval.zavalbackend.persistence

import java.nio.file.Path

/**
 * Heavy creation
 */
open class SavedMap<K, V>(
    fileName: String,
    storageDirPath: Path,
    private val keySerializer: SavedMapElementSerializer<K>,
    private val valueSerializer: SavedMapElementSerializer<V>,
) : MemoryManageable() {

    init {
        loadFromDisk()
    }

    private val KEY_VALUE_SEPARATOR = ">>>>>>>>"
    private val NULL_VALUE_INDICATOR = "~~~~IT_IS_NULL~~~~"
    private val RECORD_SEPARATOR = "\n:::::%#%&@$::::::\n"

    private val dataFilePath = storageDirPath.resolve("${fileName}.txt")
    private val inMemoryData: MutableMap<K, V> = mutableMapOf()
    private var isBusyPrivate = false
    override val isBusy: Boolean
        get() = isBusyPrivate

    val data: Map<K, V>
        get() = inMemoryData.toMap()

    fun put(key: K, value: V): V? {
        return reserveWhileDo {
            // save to RAM
            val prevValue = inMemoryData.put(key, value)
            if (prevValue == null || prevValue != value) {
                // save to persistent memory
                addToFile(key, value)
            }
            return@reserveWhileDo prevValue
        }
    }

    fun get(key: K): V? {
        return inMemoryData[key]
    }

    fun remove(key: K): V? {
        return reserveWhileDo {
            // remove from persistent memory
            addToFile(key, null)
            // remove from RAM
            return@reserveWhileDo inMemoryData.remove(key)
        }
    }

    override fun estimateOccupancy(): Double {
        return reserveWhileDo {
            val allRecords = readAllRecords()
            val effectiveSize = inMemoryData.size
            return@reserveWhileDo effectiveSize.toDouble() / allRecords.size
        }
    }

    override fun reduceSize() {
        reserveWhileDo {
            val allRecordsSize = readAllRecords().size
            val effectiveSize = inMemoryData.size
            if (allRecordsSize != effectiveSize) {
                rewriteOnDisk()
            }
        }
    }

    override fun markAsBusy() {
        isBusyPrivate = true
    }

    override fun markAsIdle() {
        isBusyPrivate = false
    }

    private fun loadFromDisk() {
        inMemoryData.clear()
        val allRecords = readAllRecords()
        if (allRecords.isEmpty()) {
            // Ensure existence
            StorageFileWorker.writeToFile("", dataFilePath)
        } else {
            for (record in allRecords) {
                val (key, value) = parseRecord(record)
                if (value != null) {
                    inMemoryData[key] = value
                }
            }
        }
    }

    /**
     * Clear the index file from garbage and reduce the file size.
     */
    private fun rewriteOnDisk() {
        val serializedData = inMemoryData.entries.joinToString(RECORD_SEPARATOR) {
            createRecord(it.key, it.value)
        }
        StorageFileWorker.writeToFile(serializedData, dataFilePath)
    }

    /**
     * Only add to the file. Previous same key entries become garbage.
     */
    private fun addToFile(key: K, value: V?) {
        val newRecord = createRecord(key, value)
        val isFirstRecord = inMemoryData.isEmpty()
        val addPortion = if (isFirstRecord) {
            newRecord
        } else {
            "${RECORD_SEPARATOR}${newRecord}"
        }
        StorageFileWorker.appendToFile(addPortion, dataFilePath)
    }

    private fun readAllRecords(): List<String> {
        return StorageFileWorker.readFile(dataFilePath)
            ?.split(RECORD_SEPARATOR)
            ?: emptyList()
    }

    private fun createRecord(key: K, value: V?): String {
        val keyStr = keySerializer.serialize(key)
        val valueStr = value?.let { valueSerializer.serialize(it) } ?: NULL_VALUE_INDICATOR
        return "${keyStr}${KEY_VALUE_SEPARATOR}${valueStr}"
    }

    private fun parseRecord(str: String): Pair<K, V?> {
        val keyAndValueStr = str.split(KEY_VALUE_SEPARATOR)
        val keyStr = keyAndValueStr[0]
        val valueStr = keyAndValueStr[1]
        val key = keySerializer.deserialize(keyStr)
        val value = if (valueStr == NULL_VALUE_INDICATOR)
            null
        else
            valueSerializer.deserialize(valueStr)
        return key to value
    }
}

class LRUCacheMap<K, V>(
    private val maxSize: Int
) {
    private val recentNodesIndexMap: MutableMap<K, DoubleLinkedListNode<Pair<K, V>>> = HashMap(maxSize)

    // head of the list is the most recent, tail is the least recent
    private val recentList: DoubleLinkedList<Pair<K, V>> = DoubleLinkedList(listOf())

    /**
     * @return previous value associated with the key if exists or null.
     */
    fun put(key: K, value: V): V? {
        val existingNode = recentNodesIndexMap[key]
        if (existingNode != null) {
            // if already exists - update and make recent
            recentList.moveToHead(existingNode)
            val prevValue = existingNode.value.second
            existingNode.value = key to value
            return prevValue
        }
        if (recentNodesIndexMap.size > maxSize) {
            // Delete the least recent item if the size is exceeded
            val leastRecentNode = recentList.tail
            val leastRecentKey = leastRecentNode!!.value.first
            recentNodesIndexMap.remove(leastRecentKey)
            recentList.remove(leastRecentNode)
        }
        // add new value as recent element
        val newNode = recentList.addToHead(key to value)
        recentNodesIndexMap[key] = newNode
        return null
    }

    fun putAll(from: Map<out K, V>) {
        from.entries.forEach {
            put(it.key, it.value)
        }
    }

    fun get(key: K): V? {
        val foundNode = recentNodesIndexMap[key]
        if (foundNode != null) {
            // make it recent
            recentList.moveToHead(foundNode)
            return foundNode.value.second
        }
        return null
    }

    fun remove(key: K): V? {
        val removedNode = recentNodesIndexMap.remove(key)
        if (removedNode != null) {
            recentList.remove(removedNode)
            return removedNode.value.second
        }
        return null
    }

    fun clear() {
        recentNodesIndexMap.clear()
        recentList.clear()
    }
}