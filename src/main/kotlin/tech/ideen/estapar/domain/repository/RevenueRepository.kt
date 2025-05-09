package tech.ideen.estapar.domain.repository

import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import tech.ideen.estapar.domain.model.Revenue
import tech.ideen.estapar.domain.model.Sector
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

/**
 * Repository for managing Revenue entities.
 */
@JdbcRepository(dialect = Dialect.POSTGRES)
interface RevenueRepository : CrudRepository<Revenue, UUID> {

    /**
     * Finds the revenue record for the specified sector and date.
     *
     * @param sector The sector to find revenue for
     * @param revenueDate The date to find revenue for
     * @return An Optional containing the revenue record, or empty if none exists
     */
    fun findBySectorAndRevenueDate(sector: Sector, revenueDate: LocalDate): Optional<Revenue>

    /**
     * Finds all revenue records for the specified sector and date range.
     *
     * @param sector The sector to find revenue for
     * @param startDate The start date
     * @param endDate The end date
     * @return List of revenue records
     */
    fun findBySectorAndRevenueDateBetween(
        sector: Sector,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<Revenue>
}