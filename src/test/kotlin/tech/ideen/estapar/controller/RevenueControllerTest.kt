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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.ideen.estapar.api.dto.revenue.RevenueRequestDTO
import tech.ideen.estapar.api.dto.revenue.RevenueResponseDTO
import tech.ideen.estapar.service.SectorService
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Optional

@MicronautTest
class RevenueControllerTest {

    @Inject
    @field:Client("/")
    lateinit var client: HttpClient

    @Test
    fun `should return revenue when sector and date exist with revenue`() {
        // Arrange
        val sectorCode = "TEST"
        val date = LocalDate.now()
        val dateStr = date.format(DateTimeFormatter.ISO_DATE)

        val request = HttpRequest.POST(
            "/revenue",
            RevenueRequestDTO(sector = sectorCode, date = dateStr)
        )

        // Act
        val response = client.toBlocking().exchange(request, RevenueResponseDTO::class.java)

        // Assert
        assertEquals(HttpStatus.OK, response.status)

        val responseBody = response.body()
        assertNotNull(responseBody)
        assertEquals(BigDecimal("150.75"), responseBody.amount)
        assertEquals("BRL", responseBody.currency)
    }

    @Test
    fun `should return zero revenue when sector exists but no revenue for date`() {
        // Arrange
        val sectorCode = "TEST"
        val date = LocalDate.now().plusDays(30) // Future date with no revenue
        val dateStr = date.format(DateTimeFormatter.ISO_DATE)

        val request = HttpRequest.POST(
            "/revenue",
            RevenueRequestDTO(sector = sectorCode, date = dateStr)
        )

        // Act
        val response = client.toBlocking().exchange(request, RevenueResponseDTO::class.java)

        // Assert
        assertEquals(HttpStatus.OK, response.status)

        val responseBody = response.body()
        assertNotNull(responseBody)
        assertEquals(BigDecimal.ZERO, responseBody.amount)
        assertEquals("BRL", responseBody.currency)
    }

    @Test
    fun `should return 404 when sector does not exist`() {
        // Arrange
        val nonExistentSectorCode = "NONEXISTENT"
        val date = LocalDate.now()
        val dateStr = date.format(DateTimeFormatter.ISO_DATE)

        val request = HttpRequest.POST(
            "/revenue",
            RevenueRequestDTO(sector = nonExistentSectorCode, date = dateStr)
        )

        // Act & Assert
        val exception = assertThrows<HttpClientResponseException> {
            client.toBlocking().exchange(request, RevenueResponseDTO::class.java)
        }

        assertEquals(HttpStatus.NOT_FOUND, exception.status)
    }
}

@Singleton
@Replaces(SectorService::class)
class MockSectorService {

    fun getRevenue(sectorCode: String, date: LocalDate): Optional<MockRevenue> {
        return when {
            sectorCode == "TEST" && !date.isAfter(LocalDate.now()) -> {
                // Return revenue for current date or past dates
                Optional.of(MockRevenue(BigDecimal("150.75"), "BRL"))
            }

            sectorCode == "TEST" -> {
                // Return empty for future dates
                Optional.empty()
            }

            else -> {
                // Throw exception for non-existent sectors
                throw NoSuchElementException("Sector not found: $sectorCode")
            }
        }
    }

    // Stub methods for other SectorService methods that might be called
    fun getDefaultSector(): Any? = null
    fun getSectorByCode(code: String): Any? = null
    fun getSectorByCodeWithSpots(code: String): Any? = null
    fun calculateOccupancyPercentage(code: String): Double = 0.0
    fun isSectorFull(code: String): Boolean = false
    fun recordRevenue(sectorCode: String, amount: BigDecimal, date: LocalDate): Any? = null
    fun getRevenueForDateRange(sectorCode: String, startDate: LocalDate, endDate: LocalDate): List<Any> = emptyList()
}

// Simple class to mock Revenue
class MockRevenue(val amount: BigDecimal, val currency: String)