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
import tech.ideen.estapar.service.VehicleService

@MicronautTest
class WebhookControllerTest {

    @Inject
    @field:Client("/")
    lateinit var client: HttpClient

    @Test
    fun `webhook should process entry event successfully`() {
        // Arrange
        val entryEvent = EntryEventDTO(
            licensePlate = "TEST123",
            entryTime = "2025-01-01T12:00:00.000Z",
            eventType = "ENTRY"
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
        // Arrange
        val parkedEvent = ParkedEventDTO(
            licensePlate = "PARKED456",
            latitude = 12.345,
            longitude = 67.890,
            eventType = "PARKED"
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
        // Arrange
        val exitEvent = ExitEventDTO(
            licensePlate = "EXIT789",
            exitTime = "2025-01-01T14:00:00.000Z",
            eventType = "EXIT"
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

    @Test
    fun `webhook should return bad request for unknown event type`() {
        // Arrange
        val unknownEvent = """
            {
                "license_plate": "UNKNOWN123",
                "event_type": "UNKNOWN_TYPE",
                "some_data": "some value"
            }
        """.trimIndent()

        val request = HttpRequest.POST("/webhook", unknownEvent)
            .header("Content-Type", "application/json")

        // Act
        val response = client.toBlocking().exchange(request, Map::class.java)

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.status)

        val responseBody = response.body()
        assertTrue(responseBody.containsKey("error"))
        assertTrue((responseBody["error"] as String).contains("Unknown event type"))
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
