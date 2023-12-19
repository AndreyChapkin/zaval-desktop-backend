package org.home.zaval.zavalbackend.persistence

import org.home.zaval.zavalbackend.dto.IdentifiedDto
import java.lang.RuntimeException
import java.nio.file.Path
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.jvmErasure

open class PersistableStorage<T : IdentifiedDto>(
    private val storagePlacementPath: Path,
    private val storageDirName: String,
    private val entityClass: Class<T>,
    private val maxEntitiesInFile: Int = 10,
    val reservationLevel: Int = 0,
    indexedProps: List<KProperty1<T, *>> = emptyList()
) : CrudStorage<T> {

    protected val storagePath = storagePlacementPath.resolve(storageDirName)
    /**
     * Property name to property index
     */
    protected val indices = indexedProps.associate { it.name to createIndexFor(it) }

    protected val chambersManager = EntityChambersManager(
        subdirName = "entities",
        storageDirPath = storagePath,
        entityClass = entityClass,
    )
    protected val reserveChambersManagers = initializeReserveEntityManagers(reservationLevel)

    override fun saveEntity(entity: T) {
        return chambersManager.saveEntity(entity)
    }

    override fun readEntity(id: Long): T? {
        return chambersManager.readEntities(listOf(id)).firstOrNull()
    }

    override fun readEntities(ids: Collection<Long>): List<T> {
        return chambersManager.readEntities(ids)
    }

    override fun updateEntity(entity: T) {
        reserve(listOf(entity.id))
        chambersManager.updateEntity(entity)
    }

    override fun removeEntity(id: Long) {
        reserve(listOf(id))
        chambersManager.removeEntities(listOf(id))
    }

    override fun removeEntities(ids: Collection<Long>) {
        reserve(ids)
        chambersManager.removeEntities(ids)
    }

    protected fun getIndexFor(prop: KProperty1<T, *>): SavedMap<*, List<Long>> {
        return indices[prop.name] ?: throw RuntimeException("No index for property '${prop.name}' of '${prop.javaField?.declaringClass}'")
    }

    private fun createIndexFor(prop: KProperty1<T, *>): SavedMap<*, List<Long>> {
        return when (prop.returnType.jvmErasure) {
            String::class -> StringPropIndex(prop.name, storagePath)
            Long::class -> LongPropIndex(prop.name, storagePath)
            else -> throw RuntimeException("Unsupported index type for property '${prop.name}' of '${prop.javaField?.declaringClass}'")
        }
    }

    private fun reserve(ids: Collection<Long>) {
        for (i in reserveChambersManagers.indices.reversed()) {
            val revisionManager = reserveChambersManagers[i]
            val revisionEntities = revisionManager.readEntities(ids)
            val fresherManager = if (i > 0) {
                reserveChambersManagers[i - 1]
            } else {
                chambersManager
            }
            val fresherEntities = fresherManager.readEntities(ids)
            fresherEntities.forEach { freshEntity ->
                val oldRevisionEntity = revisionEntities.find { it == freshEntity }
                if (oldRevisionEntity != null) {
                    revisionManager.updateEntity(freshEntity)
                } else {
                    revisionManager.saveEntity(freshEntity)
                }
            }
        }
    }

    private fun initializeReserveEntityManagers(reservationLevel: Int): List<EntityChambersManager<T>> {
        return (1..reservationLevel)
            .asSequence()
            .map {
                EntityChambersManager(
                    subdirName = "entities-reserve-${it}",
                    storageDirPath = storagePath,
                    entityClass = entityClass,
                )
            }.toList()
    }
}