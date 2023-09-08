package org.home.zaval.zavalbackend.exception

import java.nio.file.Path

class NotPersistedObjectException(filePath: Path) : RuntimeException("No persisted object in file: $filePath")
class AlreadyInPersistenceContextException() :
    RuntimeException("Persistable object already is in persistence context.")

class NotTrackedPersistableObjectModificationException() :
    RuntimeException("Persistable object modification out of persistence context is not allowed.")

class UnknownFileException(filename: String) : RuntimeException("Unknown file or directory: ${filename}")

class IncorrectEntityUpdateException(id: Any) : RuntimeException("There is no entity with id '${id}' to update.")

class ExcessiveEntitiesException(message: String) : RuntimeException("There is excessive entities. Info: ${message}")