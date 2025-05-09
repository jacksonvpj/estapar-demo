package tech.ideen.estapar.service

import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import tech.ideen.estapar.domain.model.Revenue
import tech.ideen.estapar.domain.model.Sector
import tech.ideen.estapar.domain.repository.RevenueRepository
import tech.ideen.estapar.domain.repository.SectorRepository
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional

/**
 * Service for managing sectors and their revenue.
 */
@Singleton
class SectorService(
    private val sectorRepository: SectorRepository,
    private val revenueRepository: RevenueRepository
) {
    private val logger = LoggerFactory.getLogger(SectorService::class.java)

    /**
     * Gets a sector by its code.
     *
     * @param code The sector code
     * @return The sector
     * @throws IllegalStateException if the sector is not found
     */
    fun getSectorByCode(code: String): Sector {
        logger.info("Getting sector by code: $code")
        return sectorRepository.findByCode(code)
            .orElseThrow { NoSuchElementException("Sector not found: $code") }
    }

    /**
     * Gets a sector by its code with spots eagerly loaded.
     *
     * @param code The sector code
     * @return The sector with spots
     * @throws IllegalStateException if the sector is not found
     */
    fun getSectorByCodeWithSpots(code: String): Sector {
        logger.info("Getting sector by code with spots: $code")
        return sectorRepository.findByCode(code)
            .orElseThrow { NoSuchElementException("Sector not found: $code") }
    }

    /**
     * Calculates the occupancy percentage of a sector.
     *
     * @param code The sector code
     * @return The occupancy percentage (0.0 to 1.0)
     */
    fun calculateOccupancyPercentage(code: String): Double {
        logger.info("Calculating occupancy percentage for sector: $code")
        val sector = getSectorByCodeWithSpots(code)
        return sector.calculateOccupancyPercentage()
    }

    /**
     * Checks if a sector is full.
     *
     * @return true if the sector is full, false otherwise
     */
    fun isSectorFull(code: String): Boolean {
        logger.info("Checking if sector is full: $code")
        return calculateOccupancyPercentage(code) >= 1.0
    }

    /**
     * Checks if all sectors are at full occupancy.
     *
     * @return true if all sectors are full, false otherwise
     */
    fun areAllSectorsFull(): Boolean {
        logger.info("Checking if all sectors are full")
        return sectorRepository.findAll().all { sector ->
            sector.spots.count { spot -> spot.occupied } == sector.maxCapacity
        }
    }


    /**
     * Records revenue for a sector.
     *
     * @param sectorCode The sector code
     * @param amount The amount to record
     * @param date The date to record the revenue for
     * @return The updated revenue record
     */
    fun recordRevenue(sectorCode: String, amount: BigDecimal, date: LocalDate): Revenue {
        logger.info("Recording revenue for sector: $sectorCode, amount: $amount, date: $date")

        val sector = getSectorByCode(sectorCode)

        // Find or create a revenue record for the sector and date
        val revenue = revenueRepository.findBySectorAndRevenueDate(sector, date)
            .orElseGet {
                val newRevenue = Revenue(
                    revenueDate = date,
                    sector = sector
                )
                revenueRepository.save(newRevenue)
            }

        // Add the amount to the revenue
        revenue.addAmount(amount)

        // Save and return the updated revenue
        return revenueRepository.update(revenue)
    }

    /**
     * Gets the revenue for a sector on a specific date.
     *
     * @param sectorCode The sector code
     * @param date The date to get revenue for
     * @return The revenue record, or empty if none exists
     */
    fun getRevenue(sectorCode: String, date: LocalDate): Optional<Revenue> {
        logger.info("Getting revenue for sector: $sectorCode, date: $date")

        val sector = getSectorByCode(sectorCode)
        return revenueRepository.findBySectorAndRevenueDate(sector, date)
    }

    /**
     * Gets the revenue for a sector for a date range.
     *
     * @param sectorCode The sector code
     * @param startDate The start date
     * @param endDate The end date
     * @return List of revenue records
     */
    fun getRevenueForDateRange(sectorCode: String, startDate: LocalDate, endDate: LocalDate): List<Revenue> {
        logger.info("Getting revenue for sector: $sectorCode, date range: $startDate to $endDate")

        val sector = getSectorByCode(sectorCode)
        return revenueRepository.findBySectorAndRevenueDateBetween(sector, startDate, endDate)
    }
}