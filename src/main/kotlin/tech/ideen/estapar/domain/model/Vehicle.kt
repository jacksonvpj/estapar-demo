package tech.ideen.estapar.domain.model

import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.DateUpdated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation
import java.time.LocalDateTime
import java.util.UUID

/**
 * Represents a vehicle identified by its license plate.
 */
@MappedEntity("vehicles")
class Vehicle {
    @field:Id
    var id: UUID? = UUID.randomUUID()

    @NonNull
    var licensePlate: String? = null

    @Nullable
    @field:Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = "vehicle")
    var events: MutableList<ParkingEvent> = mutableListOf()

    @Nullable
    @field:Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = "vehicle")
    var sessions: MutableList<ParkingSession> = mutableListOf()

    @NonNull
    @field:DateCreated
    var createdAt: LocalDateTime = LocalDateTime.now()

    @Nullable
    @field:DateUpdated
    var updatedAt: LocalDateTime? = null

    constructor()

    constructor(licensePlate: String) {
        this.licensePlate = licensePlate
    }

    /**
     * Adds an event to this vehicle.
     *
     * @param event The event to add
     */
    fun addEvent(event: ParkingEvent) {
        events.add(event)
    }

    /**
     * Adds a session to this vehicle.
     *
     * @param session The session to add
     */
    fun addSession(session: ParkingSession) {
        sessions.add(session)
    }

}