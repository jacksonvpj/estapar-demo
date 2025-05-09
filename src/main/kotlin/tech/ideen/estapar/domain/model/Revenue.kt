package tech.ideen.estapar.domain.model

import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.DateUpdated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

/**
 * Represents the daily revenue for each sector.
 */
@MappedEntity("revenues")
data class Revenue(
    @field:Id
    val id: UUID = UUID.randomUUID(),

    val revenueDate: LocalDate,

    var amount: BigDecimal = BigDecimal.ZERO,

    val currency: String = "BRL",

    @field:Relation(value = Relation.Kind.MANY_TO_ONE)
    val sector: Sector,

    @field:DateCreated
    val createdAt: LocalDateTime? = null,

    @field:DateUpdated
    val updatedAt: LocalDateTime? = null
) {
    /**
     * Adds the specified amount to the current revenue.
     *
     * @param amount The amount to add
     */
    fun addAmount(amount: BigDecimal) {
        this.amount = this.amount.add(amount)
    }
}