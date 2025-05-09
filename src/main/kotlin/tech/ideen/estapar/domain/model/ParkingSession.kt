package tech.ideen.estapar.domain.model

import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation
import io.micronaut.data.annotation.Transient
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID
import kotlin.math.ceil

/**
 * Represents a complete parking session from entry to exit, including pricing information.
 */
@MappedEntity("parking_sessions")
class ParkingSession {

    @field:Id
    var id: UUID = UUID.randomUUID()

    var entryTime: LocalDateTime = LocalDateTime.now()

    var parkedTime: LocalDateTime = LocalDateTime.now()

    var exitTime: LocalDateTime? = null

    var price: BigDecimal? = null

    var appliedPriceFactor: BigDecimal? = null

    var active: Boolean = true

    @field:Relation(value = Relation.Kind.MANY_TO_ONE)
    var vehicle: Vehicle? = null

    @field:Relation(value = Relation.Kind.MANY_TO_ONE)
    var parkedSpot: Spot? = null

    @field:DateCreated
    var createdAt: LocalDateTime? = null

    constructor()

    constructor(entryTime: LocalDateTime, vehicle: Vehicle) {
        this.entryTime = entryTime
        this.vehicle = vehicle
    }

    constructor(entryTime: LocalDateTime, active: Boolean, vehicle: Vehicle?, parkedSpot: Spot?) {
        this.entryTime = entryTime
        this.active = active
        this.vehicle = vehicle
        this.parkedSpot = parkedSpot
    }


    /**
     * Calculates the price for this parking session based on the sector's base price,
     * applied price factor, and duration.
     *
     * @return The calculated price
     */
    fun calculatePrice(): BigDecimal {
        if (exitTime == null) {
            return BigDecimal.ZERO
        }

        val duration = getDuration()
        val hours = ceil(duration.toMinutes() / 60.0)

        val basePrice = parkedSpot!!.sector.basePrice!!
        return basePrice
            .multiply(BigDecimal.valueOf(hours))
            .multiply(appliedPriceFactor)
    }

    /**
     * Closes this parking session with the specified exit time.
     * Calculates the final price and marks the session as inactive.
     *
     * @param exitTime The time when the vehicle exited
     */
    fun close(exitTime: LocalDateTime) {
        this.exitTime = exitTime
        this.price = calculatePrice()
        this.active = false
    }

    /**
     * Gets the duration of this parking session.
     *
     * @return The duration, or Duration.ZERO if the session is still active
     */
    @Transient
    fun getDuration(): Duration {
        return if (exitTime != null) {
            Duration.between(entryTime, exitTime)
        } else {
            Duration.ZERO
        }
    }
}