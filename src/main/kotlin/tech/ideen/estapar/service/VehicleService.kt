package tech.ideen.estapar.service

import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import tech.ideen.estapar.api.dto.webhook.EntryEventDTO
import tech.ideen.estapar.api.dto.webhook.ExitEventDTO
import tech.ideen.estapar.api.dto.webhook.ParkedEventDTO
import tech.ideen.estapar.domain.model.EventType
import tech.ideen.estapar.domain.model.ParkingEvent
import tech.ideen.estapar.domain.model.ParkingSession
import tech.ideen.estapar.domain.model.Vehicle
import tech.ideen.estapar.domain.repository.ParkingEventRepository
import tech.ideen.estapar.domain.repository.ParkingSessionRepository
import tech.ideen.estapar.domain.repository.SpotRepository
import tech.ideen.estapar.domain.repository.VehicleRepository
import tech.ideen.estapar.exception.EstaparException
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Service for managing vehicles and their parking events.
 */
@Singleton
class VehicleService(
    private val vehicleRepository: VehicleRepository,
    private val parkingEventRepository: ParkingEventRepository,
    private val parkingSessionRepository: ParkingSessionRepository,
    private val spotRepository: SpotRepository,
    private val sectorService: SectorService
) {
    private val logger = LoggerFactory.getLogger(VehicleService::class.java)

    /**
     * Processes a vehicle entry event.
     *
     * @param event The entry event
     * @return The created parking event
     */
    fun processEntryEvent(event: EntryEventDTO): ParkingEvent {
        logger.info("Processing entry event for vehicle: ${event.licensePlate}")

        if (sectorService.areAllSectorsFull()) {
            throw EstaparException("Vehicle: ${event.licensePlate}. All sectors are full at the moment ${event.entryTime}. Please try again later.")
        }

        // Find or create the vehicle
        val vehicle = findOrCreateVehicle(event.licensePlate)

        // Parse the entry time
        val entryTime = parseDateTime(event.entryTime)

        // Create a parking event
        val parkingEvent = ParkingEvent(
            eventType = EventType.ENTRY,
            eventTime = entryTime,
            vehicle = vehicle
        )

        // Save the event
        val savedEvent = parkingEventRepository.save(parkingEvent)

        // Create a new parking session
        val parkingSession = ParkingSession(
            entryTime = entryTime,
            vehicle = vehicle
        )

        // Save the session
        parkingSessionRepository.save(parkingSession)

        return savedEvent
    }

    /**
     * Processes a vehicle parked event.
     *
     * @param event The parked event
     * @return The created parking event
     */
    fun processParkedEvent(event: ParkedEventDTO): ParkingEvent {
        logger.info("Processing parked event for vehicle: ${event.licensePlate}")

        // Find the vehicle
        val vehicle = findOrCreateVehicle(event.licensePlate)

        // Find the spot by latitude and longitude
        val spot = spotRepository.findByLatitudeAndLongitude(event.latitude, event.longitude)
            .orElseThrow { IllegalStateException("Spot not found at location: ${event.latitude}, ${event.longitude}") }

        // Mark the spot as occupied
        spot.occupied = true
        spotRepository.update(spot)

        // Create a parking event
        val parkingEvent = ParkingEvent(
            eventType = EventType.PARKED,
            eventTime = LocalDateTime.now(),
            vehicle = vehicle,
            spot = spot
        )

        // Save the event
        val savedEvent = parkingEventRepository.save(parkingEvent)

        // Update the active parking session
        val activeSession = parkingSessionRepository.findByVehicleAndActive(vehicle, true)
            .orElseThrow { IllegalStateException("No active session found for vehicle: ${event.licensePlate}") }

        activeSession.parkedSpot = spot
        activeSession.parkedTime = LocalDateTime.now()

        val sector = spot.sector
        // Calculate the price factor based on current occupancy
        val priceFactor = sector.calculatePriceFactor()
        activeSession.appliedPriceFactor = BigDecimal(priceFactor)

        parkingSessionRepository.update(activeSession)

        return savedEvent
    }

    /**
     * Processes a vehicle exit event.
     *
     * @param event The exit event
     * @return The created parking event
     */
    fun processExitEvent(event: ExitEventDTO): ParkingEvent {
        logger.info("Processing exit event for vehicle: ${event.licensePlate}")

        // Find the vehicle
        val vehicle = vehicleRepository.findByLicensePlate(event.licensePlate)
            .orElseThrow { NoSuchElementException("Vehicle not found: ${event.licensePlate}") }

        // Parse the exit time
        val exitTime = parseDateTime(event.exitTime)

        // Create a parking event
        val parkingEvent = ParkingEvent(
            eventType = EventType.EXIT,
            eventTime = exitTime,
            vehicle = vehicle
        )

        // Save the event
        val savedEvent = parkingEventRepository.save(parkingEvent)

        // Find the active session
        val activeSession = parkingSessionRepository.findByVehicleAndActive(vehicle, true)
            .orElseThrow { NoSuchElementException("No active session found for vehicle: ${event.licensePlate}") }

        // If the vehicle was parked in a spot, mark it as unoccupied
        activeSession.parkedSpot?.let { spot ->
            spot.occupied = false
            spotRepository.update(spot)
        }

        // Close the session
        activeSession.close(exitTime)
        parkingSessionRepository.update(activeSession)

        // Record the revenue
        sectorService.recordRevenue(
            activeSession.parkedSpot!!.sector.code!!,
            activeSession.price!!,
            exitTime.toLocalDate()
        )

        return savedEvent
    }

    /**
     * Gets the status of a vehicle.
     *
     * @param licensePlate The license plate of the vehicle
     * @return The vehicle status information
     */
    fun getVehicleStatus(licensePlate: String): Map<String, Any?> {
        logger.info("Getting status for vehicle: $licensePlate")

        // Find the vehicle
        val vehicle = vehicleRepository.findByLicensePlate(licensePlate)
            .orElseThrow { NoSuchElementException("Vehicle not found: $licensePlate") }

        // Find the active session
        val activeSession = parkingSessionRepository.findByVehicleAndActive(vehicle, true)

        return if (activeSession.isPresent) {
            val session = activeSession.get()
            mapOf(
                "license_plate" to vehicle.licensePlate,
                "price_until_now" to session.calculatePrice(),
                "entry_time" to session.entryTime,
                "time_parked" to session.parkedSpot?.let { session.entryTime },
                "lat" to session.parkedSpot?.latitude,
                "lng" to session.parkedSpot?.longitude
            )
        } else {
            mapOf(
                "license_plate" to vehicle.licensePlate,
                "message" to "Vehicle is not currently parked"
            )
        }
    }

    /**
     * Finds a vehicle by its license plate or creates a new one if not found.
     *
     * @param licensePlate The license plate to search for
     * @return The found or created vehicle
     */
    private fun findOrCreateVehicle(licensePlate: String): Vehicle {
        return vehicleRepository.findByLicensePlate(licensePlate)
            .orElseGet {
                val newVehicle = Vehicle(licensePlate = licensePlate)
                vehicleRepository.save(newVehicle)
            }
    }

    /**
     * Parses a date-time string in ISO format.
     *
     * @param dateTimeString The date-time string to parse
     * @return The parsed LocalDateTime
     */
    private fun parseDateTime(dateTimeString: String): LocalDateTime {
        return try {
            // First try parsing as ZonedDateTime if zone information is present
            ZonedDateTime.parse(dateTimeString, DateTimeFormatter.ISO_DATE_TIME)
                .toLocalDateTime()
        } catch (e: DateTimeParseException) {
            // Fall back to parsing as LocalDateTime if no zone information
            LocalDateTime.parse(dateTimeString, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        }
    }

}