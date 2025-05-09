package tech.ideen.estapar.controller

import io.micronaut.context.annotation.Replaces
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.serde.annotation.SerdeImport
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.ideen.estapar.api.dto.plate.PlateStatusRequestDTO
import tech.ideen.estapar.api.dto.plate.PlateStatusResponseDTO
import tech.ideen.estapar.service.VehicleService
import java.math.BigDecimal
import java.time.LocalDateTime

@SerdeImport(PlateStatusRequestDTO::class)
@SerdeImport(PlateStatusResponseDTO::class)

@MicronautTest
class PlateStatusControllerTest {

    @Inject
    @field:Client("/")
    lateinit var client: HttpClient

    @Test
    fun `should return vehicle status when vehicle exists`() {
        // Arrange
        val licensePlate = "TEST123"

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
        // Since the vehicle is not parked, we expect a message and no parking details
        assertNotNull(responseBody.message)
        assertNull(responseBody.entryTime)
        assertNull(responseBody.timeParked)
        assertNull(responseBody.priceUntilNow)
        assertNull(responseBody.latitude)
        assertNull(responseBody.longitude)
    }

    @Test
    fun `should return vehicle status with parking details when vehicle is parked`() {
        // Arrange
        val licensePlate = "PARKED123"

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

@Singleton
@Replaces(VehicleService::class)
class MockVehicleService {

    fun getVehicleStatus(licensePlate: String): Map<String, Any?> {
        return when (licensePlate) {
            "TEST123" -> mapOf(
                "license_plate" to licensePlate,
                "message" to "Vehicle is not currently parked"
            )

            "PARKED123" -> {
                val entryTime = LocalDateTime.now().minusHours(1)
                val timeParked = LocalDateTime.now()
                mapOf(
                    "license_plate" to licensePlate,
                    "price_until_now" to BigDecimal("15.50"),
                    "entry_time" to entryTime,
                    "time_parked" to timeParked,
                    "lat" to 12.345,
                    "lng" to 67.890
                )
            }

            else -> throw NoSuchElementException("Vehicle not found: $licensePlate")
        }
    }

    // Stub methods for other VehicleService methods that might be called
    fun processEntryEvent(event: Any): Any? = null
    fun processParkedEvent(event: Any): Any? = null
    fun processExitEvent(event: Any): Any? = null
}
