package tech.ideen.estapar.domain.model

import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation
import java.time.LocalDateTime
import java.util.UUID

/**
 * Represents a parking garage facility.
 */
@MappedEntity("garages")
class Garage {
    @field:Id
    var id: UUID = UUID.randomUUID()

    @Nullable
    @field:Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = "garage")
    var sectors: MutableList<Sector> = mutableListOf()

    @field:DateCreated
    var createdAt: LocalDateTime? = LocalDateTime.now()

    /**
     * Adds a sector to this garage.
     *
     * @param sector The sector to add
     */
    fun addSector(sector: Sector) {
        sectors.add(sector)
    }

    /**
     * Removes a sector from this garage.
     *
     * @param sector The sector to remove
     */
    fun removeSector(sector: Sector) {
        sectors.remove(sector)
    }
}