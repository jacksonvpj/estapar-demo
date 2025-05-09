package tech.ideen.estapar.domain.model

import io.micronaut.core.annotation.NonNull
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation
import java.time.LocalDateTime
import java.util.UUID

/**
 * Represents events related to vehicles (entry, parked, exit).
 */
@MappedEntity("parking_events")
class ParkingEvent {
    @field:Id
    @NonNull
    var id: UUID = UUID.randomUUID()

    @NonNull
    var eventType: EventType? = null

    @NonNull
    var eventTime: LocalDateTime? = null

    @NonNull
    @field:Relation(value = Relation.Kind.MANY_TO_ONE)
    var vehicle: Vehicle? = null

    @NonNull
    @field:Relation(value = Relation.Kind.MANY_TO_ONE)
    var spot: Spot? = null

    @NonNull
    @field:DateCreated
    var createdAt: LocalDateTime? = LocalDateTime.now()

    constructor()

    constructor(eventType: EventType, eventTime: LocalDateTime, vehicle: Vehicle) {
        this.eventType = eventType
        this.eventTime = eventTime
        this.vehicle = vehicle
    }

    constructor(eventType: EventType, eventTime: LocalDateTime, vehicle: Vehicle, spot: Spot?) :
            this(eventType, eventTime, vehicle) {
        this.spot = spot
    }
}