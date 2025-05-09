package tech.ideen.estapar.controller

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.ideen.estapar.api.dto.plate.PlateStatusRequestDTO
import tech.ideen.estapar.api.dto.plate.PlateStatusResponseDTO
import tech.ideen.estapar.api.dto.webhook.EntryEventDTO
import tech.ideen.estapar.api.dto.webhook.ParkedEventDTO
import tech.ideen.estapar.domain.model.EventType

@MicronautTest
class PlateStatusControllerTest : EstaparControllerTestContainer(){

    @Inject
    @field:Client("/")
    lateinit var client: HttpClient

    @Test
    fun `should return vehicle status when vehicle exists`() {
        // Arrange
        val licensePlate = "TEST123"

        val entryEvent = EntryEventDTO(
            licensePlate = licensePlate,
            entryTime = "2025-01-01T12:00:00.000Z",
            eventType = EventType.ENTRY
        )

        val entry = HttpRequest.POST("/webhook", entryEvent)
        client.toBlocking().exchange(entry, Map::class.java)
        val request = HttpRequest.POST(
            "/plate-status",
            PlateStatusRequestDTO(licensePlate = licensePlate)
        )

        // Act
        val response = client.toBlocking().exchange(request, PlateStatusResponseDTO::class.java)

        // Assert
        assertEquals(HttpStatus.OK, response.status)

        val responseBody = response.body()
        assertNotNull(responseBody)
        assertEquals(licensePlate, responseBody.licensePlate)

    }

    @Test
    fun `should return vehicle status with parking details when vehicle is parked`() {
        // Arrange
        val licensePlate = "PARKED123"

        val entryEvent = EntryEventDTO(
            licensePlate = licensePlate,
            entryTime = "2025-01-01T12:00:00.000Z",
            eventType = EventType.ENTRY
        )

        val entry = HttpRequest.POST("/webhook", entryEvent)
        client.toBlocking().exchange(entry, Map::class.java)

        val parkedEvent = ParkedEventDTO(
            licensePlate = licensePlate,
            latitude = -23.561664,
            longitude = -46.655961,
            eventType = EventType.PARKED
        )

        val parked = HttpRequest.POST("/webhook", parkedEvent)
        client.toBlocking().exchange(parked, Map::class.java)

        val request = HttpRequest.POST(
            "/plate-status",
            PlateStatusRequestDTO(licensePlate = licensePlate)
        )

        // Act
        val response = client.toBlocking().exchange(request, PlateStatusResponseDTO::class.java)

        // Assert
        assertEquals(HttpStatus.OK, response.status)

        val responseBody = response.body()
        assertNotNull(responseBody)
        assertEquals(licensePlate, responseBody.licensePlate)
        assertNotNull(responseBody.priceUntilNow)
        assertNotNull(responseBody.entryTime)
        assertNotNull(responseBody.timeParked)
        assertNotNull(responseBody.latitude)
        assertNotNull(responseBody.longitude)
        assertNull(responseBody.message)
    }

    @Test
    fun `should return 404 when vehicle does not exist`() {
        // Arrange
        val nonExistentLicensePlate = "NONEXISTENT"

        val request = HttpRequest.POST(
            "/plate-status",
            PlateStatusRequestDTO(licensePlate = nonExistentLicensePlate)
        )

        // Act & Assert
        val exception = assertThrows<HttpClientResponseException> {
            client.toBlocking().exchange(request, PlateStatusResponseDTO::class.java)
        }

        assertEquals(HttpStatus.NOT_FOUND, exception.status)
    }
}
