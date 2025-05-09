package tech.ideen.estapar.controller

import io.micronaut.context.annotation.Replaces
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.ideen.estapar.api.dto.webhook.EntryEventDTO
import tech.ideen.estapar.api.dto.webhook.ExitEventDTO
import tech.ideen.estapar.api.dto.webhook.ParkedEventDTO
import tech.ideen.estapar.config.EstaparTestContainer
import tech.ideen.estapar.domain.model.EventType
import tech.ideen.estapar.service.VehicleService

@MicronautTest
class WebhookControllerTest : EstaparTestContainer(){

    @Inject
    @field:Client("/")
    lateinit var client: HttpClient

    @Test
    fun `webhook should process entry event successfully`() {
        // Arrange
        val entryEvent = EntryEventDTO(
            licensePlate = "TEST123",
            entryTime = "2025-01-01T12:00:00.000Z",
            eventType = EventType.ENTRY
        )

        val request = HttpRequest.POST("/webhook", entryEvent)

        // Act
        val response = client.toBlocking().exchange(request, Map::class.java)

        // Assert
        assertEquals(HttpStatus.OK, response.status)

        val responseBody = response.body()
        assertTrue(responseBody.containsKey("status"))
        assertEquals("Event processed successfully", responseBody["status"])
    }

    @Test
    fun `webhook should process parked event successfully`() {
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
            latitude = -23.561664,
            longitude = -46.655961,
            eventType = EventType.PARKED
        )

        val request = HttpRequest.POST("/webhook", parkedEvent)

        // Act
        val response = client.toBlocking().exchange(request, Map::class.java)

        // Assert
        assertEquals(HttpStatus.OK, response.status)

        val responseBody = response.body()
        assertTrue(responseBody.containsKey("status"))
        assertEquals("Event processed successfully", responseBody["status"])
    }

    @Test
    fun `webhook should process exit event successfully`() {

        val entryEvent = EntryEventDTO(
            licensePlate = "EXIT789",
            entryTime = "2025-01-01T12:00:00.000Z",
            eventType = EventType.ENTRY
        )

        val entry = HttpRequest.POST("/webhook", entryEvent)
        client.toBlocking().exchange(entry, Map::class.java)
        // Arrange
        val parkedEvent = ParkedEventDTO(
            licensePlate = "EXIT789",
            latitude = -23.561664,
            longitude = -46.655961,
            eventType = EventType.PARKED
        )

        val parked = HttpRequest.POST("/webhook", parkedEvent)
        client.toBlocking().exchange(parked, Map::class.java)

        // Arrange
        val exitEvent = ExitEventDTO(
            licensePlate = "EXIT789",
            exitTime = "2025-01-01T14:00:00.000Z",
            eventType = EventType.EXIT
        )

        val request = HttpRequest.POST("/webhook", exitEvent)

        // Act
        val response = client.toBlocking().exchange(request, Map::class.java)

        // Assert
        assertEquals(HttpStatus.OK, response.status)

        val responseBody = response.body()
        assertTrue(responseBody.containsKey("status"))
        assertEquals("Event processed successfully", responseBody["status"])
    }

}

@Singleton
@Replaces(VehicleService::class)
class MockWebhookVehicleService {

    fun processEntryEvent(event: Any): Any? {
        // Just return success, no need to actually process the event
        return null
    }

    fun processParkedEvent(event: Any): Any? {
        // Just return success, no need to actually process the event
        return null
    }

    fun processExitEvent(event: Any): Any? {
        // Just return success, no need to actually process the event
        return null
    }

    // Stub methods for other VehicleService methods that might be called
    fun getVehicleStatus(licensePlate: String): Map<String, Any?> = emptyMap()
}
