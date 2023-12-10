package org.home.zaval.zavalbackend.persistence

import java.nio.file.Path

open class SavedMap<K, V>(
    fileName: String,
    storageDirPath: Path,
    private val keySerializer: SavedMapElementSerializer<K>,
    private val valueSerializer: SavedMapElementSerializer<V>,
) {

    private val KEY_VALUE_SEPARATOR = ">>>>>>>>"
    private val NULL_VALUE_INDICATOR = "~~~~IT_IS_NULL~~~~"
    private val RECORD_SEPARATOR = "\n:::::%#%&@$::::::\n"

    private val dataFilePath = storageDirPath.resolve("${fileName}.txt")
    private val inMemoryData: MutableMap<K, V> = mutableMapOf()
    private var isInitialized = false
    private var isReady = false

    fun put(key: K, value: V): V? {
        panicIfNotReady()
        // save to RAM
        val prevValue = inMemoryData.put(key, value)
        if (prevValue == null || prevValue != value) {
            // save to persistent memory
            addToFile(key, value)
        }
        return prevValue
    }

    fun get(key: K): V? {
        panicIfNotReady()
        return inMemoryData[key]
    }

    fun remove(key: K): V? {
        panicIfNotReady()
        // remove from persistent memory
        addToFile(key, null)
        // remove from RAM
        return inMemoryData.remove(key)
    }

    fun initialize() {
        loadFromDisk()
        isInitialized = true
        isReady = true
    }

    fun tryToReduceSize() {
        if (occupancyRate() < 1) {
            rewriteOnDisk()
        }
    }

    /**
     * @return <actual records number>/<all records number>
     */
    private fun occupancyRate(): Double {
        val allRecords = readAllRecords()
        val foldedRecords = foldRecords(allRecords)
        return foldedRecords.size.toDouble() / allRecords.size
    }

    private fun loadFromDisk() {
        makeBusyWhileDo {
            val allRecords = readAllRecords()
            inMemoryData.clear()
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
        makeBusyWhileDo {
            val serializedIndex = inMemoryData.entries.joinToString(System.lineSeparator()) {
                createRecord(it.key, it.value)
            }
            StorageFileWorker.writeToFile(serializedIndex, dataFilePath)
        }
    }

    /**
     * Only add to the file. Previous entries become garbage.
     */
    private fun addToFile(key: K, value: V?) {
        makeBusyWhileDo {
            val newRecord = createRecord(key, value)
            val isFirstRecord = inMemoryData.isEmpty()
            val addPortion = if (isFirstRecord)
                newRecord
            else
                "${RECORD_SEPARATOR}${newRecord}"
            StorageFileWorker.appendToFile(addPortion, dataFilePath)
        }
    }

    private fun readAllRecords(): List<String> {
        return StorageFileWorker.readFile(dataFilePath)
            ?.split(RECORD_SEPARATOR)
            ?: emptyList()
    }

    private fun foldRecords(allRecords: Collection<String>): Map<K, String> {
        val keyToRecordMap = allRecords
            .associateBy { parseRecord(it).first }
            .toMutableMap()
        // remove null entries
        for ((key, value) in keyToRecordMap) {
            if (value.endsWith(NULL_VALUE_INDICATOR)) {
                keyToRecordMap.remove(key)
            }
        }
        return keyToRecordMap
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

    private inline fun makeBusyWhileDo(action: () -> Unit) {
        if (isInitialized) {
            isReady = false
            action()
            isReady = true
        }
    }

    private fun panicIfNotReady() {
        if (!isReady) {
            throw FileIsNotReadyException(dataFilePath.toString())
        }
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

    fun clear() {
        recentNodesIndexMap.clear()
        recentList.clear()
    }
}