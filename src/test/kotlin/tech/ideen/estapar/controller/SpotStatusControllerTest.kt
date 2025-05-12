package tech.ideen.estapar.controller

import io.micronaut.context.annotation.Replaces
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.ideen.estapar.api.dto.spot.SpotStatusRequestDTO
import tech.ideen.estapar.api.dto.spot.SpotStatusResponseDTO
import tech.ideen.estapar.api.dto.webhook.EntryEventDTO
import tech.ideen.estapar.api.dto.webhook.ParkedEventDTO
import tech.ideen.estapar.config.EstaparTestContainer
import tech.ideen.estapar.domain.model.EventType
import tech.ideen.estapar.service.SpotService
import java.time.LocalDateTime

@MicronautTest
class SpotStatusControllerTest : EstaparTestContainer(){

    @Inject
    @field:Client("/")
    lateinit var client: HttpClient

    @Test
    fun `should return spot status when spot exists and is unoccupied`() {
        // Arrange
        val latitude = -23.561664
        val longitude = -46.655961

        val request = HttpRequest.POST(
            "/spot-status",
            SpotStatusRequestDTO(latitude = latitude, longitude = longitude)
        )

        // Act
        val response = client.toBlocking().exchange(request, SpotStatusResponseDTO::class.java)

        // Assert
        assertEquals(HttpStatus.OK, response.status)

        val responseBody = response.body()
        assertNotNull(responseBody)
        assertEquals(false, responseBody.occupied)
        assertNull(responseBody.entryTime)
        assertNull(responseBody.timeParked)
    }

    @Test
    fun `should return spot status when spot exists and is occupied`() {
        // Arrange
        val latitude = -23.561664
        val longitude = -46.655961

        val entryEvent = EntryEventDTO(
            licensePlate = "PARKED456",
            entryTime = "2025-01-01T12:00:00.000Z",
            eventType = EventType.ENTRY
        )

        val entry = HttpRequest.POST("/webhook", entryEvent)
        client.toBlocking().exchange(entry, Map::class.java)
        // Arrange
        val parkedEvent = ParkedEventDTO(
            licensePlate = "PARKED456",
            latitude = latitude,
            longitude = longitude,
            eventType = EventType.PARKED
        )
        val parked = HttpRequest.POST("/webhook", parkedEvent)
        client.toBlocking().exchange(parked, Map::class.java)

        val request = HttpRequest.POST(
            "/spot-status",
            SpotStatusRequestDTO(latitude = latitude, longitude = longitude)
        )

        // Act
        val response = client.toBlocking().exchange(request, SpotStatusResponseDTO::class.java)

        // Assert
        assertEquals(HttpStatus.OK, response.status)

        val responseBody = response.body()
        assertNotNull(responseBody)
        assertEquals(true, responseBody.occupied)
        assertNotNull(responseBody.entryTime)
        assertNotNull(responseBody.timeParked)
    }

    @Test
    fun `should return 404 when spot does not exist`() {
        // Arrange
        val nonExistentLatitude = 99.999
        val nonExistentLongitude = 99.999

        val request = HttpRequest.POST(
            "/spot-status",
            SpotStatusRequestDTO(latitude = nonExistentLatitude, longitude = nonExistentLongitude)
        )

        // Act & Assert
        val exception = assertThrows<HttpClientResponseException> {
            client.toBlocking().exchange(request, SpotStatusResponseDTO::class.java)
        }

        assertEquals(HttpStatus.NOT_FOUND, exception.status)
    }
}

@Singleton
@Replaces(SpotService::class)
class MockSpotService {

    fun getSpotStatus(latitude: Double, longitude: Double): Map<String, Any?> {
        return when {
            latitude == 12.345 && longitude == 67.890 -> mapOf(
                "occupied" to false
            )

            latitude == 23.456 && longitude == 78.901 -> {
                val entryTime = LocalDateTime.now().minusHours(1)
                val timeParked = LocalDateTime.now()
                mapOf(
                    "occupied" to true,
                    "entry_time" to entryTime,
                    "time_parked" to timeParked
                )
            }

            else -> throw IllegalStateException("Spot not found at location: $latitude, $longitude")
        }
    }

}