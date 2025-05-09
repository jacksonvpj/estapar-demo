package tech.ideen.estapar.service

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import tech.ideen.estapar.domain.model.ParkingSession
import tech.ideen.estapar.domain.model.Sector
import tech.ideen.estapar.domain.model.Spot
import tech.ideen.estapar.domain.model.Vehicle
import tech.ideen.estapar.domain.repository.ParkingSessionRepository
import tech.ideen.estapar.domain.repository.SpotRepository
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional

@MicronautTest
class SpotServiceTest {

    private lateinit var spotRepository: SpotRepository
    private lateinit var parkingSessionRepository: ParkingSessionRepository
    private lateinit var spotService: SpotService

    @BeforeEach
    fun setup() {
        spotRepository = mock(SpotRepository::class.java)
        parkingSessionRepository = mock(ParkingSessionRepository::class.java)
        spotService = SpotService(spotRepository, parkingSessionRepository)
    }

    @Test
    fun `getSpotByLocation should return spot when it exists`() {
        // Arrange
        val spot = createSampleSpot()
        val latitude = spot.latitude
        val longitude = spot.longitude
        whenever(spotRepository.findByLatitudeAndLongitude(latitude, longitude)).thenReturn(Optional.of(spot))

        // Act
        val result = spotService.getSpotByLocation(latitude, longitude)

        // Assert
        assertTrue(result.isPresent)
        assertEquals(spot, result.get())
    }

    @Test
    fun `getSpotByLocation should return empty when spot does not exist`() {
        // Arrange
        val latitude = 10.0
        val longitude = 20.0
        whenever(spotRepository.findByLatitudeAndLongitude(latitude, longitude)).thenReturn(Optional.empty())

        // Act
        val result = spotService.getSpotByLocation(latitude, longitude)

        // Assert
        assertFalse(result.isPresent)
    }

    @Test
    fun `getSpotStatus should return correct status for unoccupied spot`() {
        // Arrange
        val spot = createSampleSpot(occupied = false)
        val latitude = spot.latitude
        val longitude = spot.longitude
        whenever(spotRepository.findByLatitudeAndLongitude(latitude, longitude)).thenReturn(Optional.of(spot))

        // Act
        val result = spotService.getSpotStatus(latitude, longitude)

        // Assert
        assertEquals(false, result["occupied"])
        assertNull(result["entry_time"])
        assertNull(result["time_parked"])
    }

    @Test
    fun `getSpotStatus should return correct status for occupied spot with active session`() {
        // Arrange
        val spot = createSampleSpot(occupied = true)
        val latitude = spot.latitude
        val longitude = spot.longitude
        val entryTime = LocalDateTime.now().minusHours(1)
        val session = createSampleSession(spot, entryTime)

        whenever(spotRepository.findByLatitudeAndLongitude(latitude, longitude)).thenReturn(Optional.of(spot))
        whenever(parkingSessionRepository.findByParkedSpot(spot)).thenReturn(listOf(session))

        // Act
        val result = spotService.getSpotStatus(latitude, longitude)

        // Assert
        assertEquals(true, result["occupied"])
        assertEquals(entryTime, result["entry_time"])
        assertNotNull(result["time_parked"])
    }

    @Test
    fun `getSpotStatus should return correct status for occupied spot without active session`() {
        // Arrange
        val spot = createSampleSpot(occupied = true)
        val latitude = spot.latitude
        val longitude = spot.longitude

        whenever(spotRepository.findByLatitudeAndLongitude(latitude, longitude)).thenReturn(Optional.of(spot))
        whenever(parkingSessionRepository.findByParkedSpot(spot)).thenReturn(emptyList())

        // Act
        val result = spotService.getSpotStatus(latitude, longitude)

        // Assert
        assertEquals(true, result["occupied"])
        assertNull(result["entry_time"])
        assertNull(result["time_parked"])
    }

    @Test
    fun `getSpotStatus should throw exception when spot does not exist`() {
        // Arrange
        val latitude = 10.0
        val longitude = 20.0
        whenever(spotRepository.findByLatitudeAndLongitude(latitude, longitude)).thenReturn(Optional.empty())

        // Act & Assert
        assertThrows<IllegalStateException> { spotService.getSpotStatus(latitude, longitude) }
    }

    @Test
    fun `setSpotOccupied should update spot status to occupied`() {
        // Arrange
        val spot = createSampleSpot(occupied = false)
        val latitude = spot.latitude
        val longitude = spot.longitude
        val updatedSpot = spot.copy(occupied = true)

        whenever(spotRepository.findByLatitudeAndLongitude(latitude, longitude)).thenReturn(Optional.of(spot))
        whenever(spotRepository.update(any())).thenReturn(updatedSpot)

        // Act
        val result = spotService.setSpotOccupied(latitude, longitude, true)

        // Assert
        assertTrue(result.occupied)
        verify(spotRepository).update(any())
    }

    @Test
    fun `setSpotOccupied should update spot status to unoccupied`() {
        // Arrange
        val spot = createSampleSpot(occupied = true)
        val latitude = spot.latitude
        val longitude = spot.longitude
        val updatedSpot = spot.copy(occupied = false)

        whenever(spotRepository.findByLatitudeAndLongitude(latitude, longitude)).thenReturn(Optional.of(spot))
        whenever(spotRepository.update(any())).thenReturn(updatedSpot)

        // Act
        val result = spotService.setSpotOccupied(latitude, longitude, false)

        // Assert
        assertFalse(result.occupied)
        verify(spotRepository).update(any())
    }

    @Test
    fun `setSpotOccupied should throw exception when spot does not exist`() {
        // Arrange
        val latitude = 10.0
        val longitude = 20.0
        whenever(spotRepository.findByLatitudeAndLongitude(latitude, longitude)).thenReturn(Optional.empty())

        // Act & Assert
        assertThrows<IllegalStateException> { spotService.setSpotOccupied(latitude, longitude, true) }
    }

    private fun createSampleSector(): Sector {
        return Sector(
            code = "A",
            basePrice = BigDecimal("10.00"),
            maxCapacity = 100,
            openHour = LocalTime.of(8, 0),
            closeHour = LocalTime.of(22, 0),
            durationLimitMinutes = 240,
            garage = mock()
        )
    }

    private fun createSampleSpot(occupied: Boolean = false): Spot {
        return Spot(
            id = 1,
            latitude = -23.561684,
            longitude = -46.655981,
            sector = createSampleSector(),
            occupied = occupied
        )
    }

    private fun createSampleSession(spot: Spot, entryTime: LocalDateTime): ParkingSession {
        return ParkingSession(
            entryTime = entryTime,
            active = true,
            vehicle = mock(Vehicle::class.java),
            parkedSpot = spot
        )
    }
}