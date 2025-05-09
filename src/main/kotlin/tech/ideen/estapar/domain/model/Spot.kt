package tech.ideen.estapar.domain.model

import io.micronaut.core.annotation.NonNull
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.DateUpdated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation
import java.time.LocalDateTime

/**
 * Represents an individual parking spot with geolocation.
 */
@MappedEntity("spots")
data class Spot(
    @field:Id
    @NonNull
    var id: Int? = null,
    @NonNull
    var latitude: Double,
    @NonNull
    var longitude: Double,

    @NonNull
    @field:Relation(value = Relation.Kind.MANY_TO_ONE)
    var sector: Sector,

    @NonNull
    var occupied: Boolean = false,

    @field:DateCreated
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @field:DateUpdated
    val updatedAt: LocalDateTime? = null
)