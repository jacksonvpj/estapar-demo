package tech.ideen.estapar.service

import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import tech.ideen.estapar.domain.model.Spot
import tech.ideen.estapar.domain.repository.ParkingSessionRepository
import tech.ideen.estapar.domain.repository.SpotRepository
import java.time.LocalDateTime
import java.util.Optional

/**
 * Service for managing spots and their status.
 */
@Singleton
class SpotService(
    private val spotRepository: SpotRepository,
    private val parkingSessionRepository: ParkingSessionRepository
) {
    private val logger = LoggerFactory.getLogger(SpotService::class.java)

    /**
     * Gets a spot by its latitude and longitude.
     *
     * @param latitude The latitude
     * @param longitude The longitude
     * @return An Optional containing the spot, or empty if none exists
     */
    fun getSpotByLocation(latitude: Double, longitude: Double): Optional<Spot> {
        logger.info("Getting spot by location: $latitude, $longitude")
        return spotRepository.findByLatitudeAndLongitude(latitude, longitude)
    }

    /**
     * Gets the status of a spot.
     *
     * @param latitude The latitude
     * @param longitude The longitude
     * @return A map containing the spot status information
     * @throws NoSuchElementException if the spot is not found
     */
    fun getSpotStatus(latitude: Double, longitude: Double): Map<String, Any?> {
        logger.info("Getting status for spot at location: $latitude, $longitude")

        val spot = getSpotByLocation(latitude, longitude)
            .orElseThrow { NoSuchElementException("Spot not found at location: $latitude, $longitude") }

        val result = mutableMapOf<String, Any?>(
            "occupied" to spot.occupied
        )

        if (spot.occupied) {
            // Find the active session for this spot
            val activeSessions = parkingSessionRepository.findByParkedSpot(spot)
                .filter { it.active }

            if (activeSessions.isNotEmpty()) {
                val session = activeSessions.first()
                result["entry_time"] = session.entryTime
                result["time_parked"] = LocalDateTime.now()
            }
        }

        return result
    }

    /**
     * Sets the occupied status of a spot.
     *
     * @param latitude The latitude
     * @param longitude The longitude
     * @param occupied The new occupied status
     * @return The updated spot
     * @throws NoSuchElementException if the spot is not found
     */
    fun setSpotOccupied(latitude: Double, longitude: Double, occupied: Boolean): Spot {
        logger.info("Setting spot at location: $latitude, $longitude to occupied: $occupied")

        val spot = getSpotByLocation(latitude, longitude)
            .orElseThrow { NoSuchElementException("Spot not found at location: $latitude, $longitude") }

        spot.occupied = occupied
        return spotRepository.update(spot)
    }
}
