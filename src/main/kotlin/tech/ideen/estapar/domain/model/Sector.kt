package tech.ideen.estapar.domain.model

import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.DateUpdated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Represents a section of the garage with specific pricing and capacity rules.
 */
@MappedEntity("sectors")
class Sector {
    @field:Id
    @NonNull
    var code: String? = null

    @NonNull
    var basePrice: BigDecimal? = null

    @NonNull
    var maxCapacity: Int? = null

    @NonNull
    var openHour: LocalTime? = null

    @NonNull
    var closeHour: LocalTime? = null

    @NonNull
    var durationLimitMinutes: Int? = null

    @NonNull
    @field:Relation(value = Relation.Kind.MANY_TO_ONE)
    var garage: Garage? = null

    @Nullable
    @field:Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = "sector")
    var spots: MutableList<Spot> = mutableListOf()

    @NonNull
    @field:DateCreated
    val createdAt: LocalDateTime = LocalDateTime.now()

    @field:DateUpdated
    val updatedAt: LocalDateTime? = null


    constructor()

    constructor(
        code: String,
        basePrice: BigDecimal,
        maxCapacity: Int,
        openHour: LocalTime,
        closeHour: LocalTime,
        durationLimitMinutes: Int,
        garage: Garage?,
    ) {
        this.code = code
        this.basePrice = basePrice
        this.maxCapacity = maxCapacity
        this.openHour = openHour
        this.closeHour = closeHour
        this.durationLimitMinutes = durationLimitMinutes
        this.garage = garage
    }

    /**
     * Calculates the current occupancy percentage of this sector.
     *
     * @return The occupancy percentage (0.0 to 1.0)
     */
    fun calculateOccupancyPercentage(): Double {
        if (maxCapacity!! <= 0) return 0.0
        val occupiedCount = spots.count { it.occupied }
        return occupiedCount.toDouble() / maxCapacity!!.toDouble()
    }

    /**
     * Calculates the price factor based on current occupancy.
     * - Less than 25% occupancy: 0.9 (10% discount)
     * - 25% to 50% occupancy: 1.0 (no change)
     * - 50% to 75% occupancy: 1.1 (10% increase)
     * - 75% to 100% occupancy: 1.25 (25% increase)
     *
     * @return The price factor to apply
     */
    fun calculatePriceFactor(): Double {
        val occupancy = calculateOccupancyPercentage()
        return when {
            occupancy < 0.25 -> 0.9  // 10% discount
            occupancy < 0.5 -> 1.0   // No change
            occupancy < 0.75 -> 1.1  // 10% increase
            else -> 1.25             // 25% increase
        }
    }

    /**
     * Checks if the sector is open at the specified time.
     *
     * @param time The time to check
     * @return true if the sector is open, false otherwise
     */
    fun isOpen(time: LocalDateTime): Boolean {
        val timeOfDay = time.toLocalTime()
        return !timeOfDay.isBefore(openHour) && !timeOfDay.isAfter(closeHour)
    }

    /**
     * Adds a spot to this sector.
     *
     * @param spot The spot to add
     */
    fun addSpot(spot: Spot) {
        spots.add(spot)
    }

    /**
     * Removes a spot from this sector.
     *
     * @param spot The spot to remove
     */
    fun removeSpot(spot: Spot) {
        spots.remove(spot)
    }
}