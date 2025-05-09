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
import org.junit.jupiter.api.AfterAll
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
class RevenueControllerTest: EstaparControllerTestContainer() {

    @Inject
    @field:Client("/")
    lateinit var client: HttpClient

    @AfterAll
    fun cleanup() {
        postgresContainer.stop()
        wireMockServer.stop()
    }

    @Test
    fun `should return revenue when sector and date exist with revenue`() {
        // Arrange
        val sectorCode = "A" // Using sector A from the garage data
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
        // Sector A has base_price 40.5 and we're assuming 5 occupied spots
        assertEquals(BigDecimal("202.5"), responseBody.amount)
        assertEquals("BRL", responseBody.currency)
    }

    @Test
    fun `should return zero revenue when sector exists but no revenue for date`() {
        // Arrange
        val sectorCode = "B" // Using sector B from the garage data
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
        val nonExistentSectorCode = "NONEXISTENT" // This sector doesn't exist in the garage data
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
    // Hardcoded garage data based on the JSON provided in the issue description
    private val garageData = mapOf(
        "A" to GarageSector("A", BigDecimal("40.5"), 10),
        "B" to GarageSector("B", BigDecimal("4.1"), 20)
    )

    fun getRevenue(sectorCode: String, date: LocalDate): Optional<MockRevenue> {
        println("[DEBUG_LOG] Getting revenue for sector: $sectorCode, date: $date")

        val sector = garageData[sectorCode]
        println("[DEBUG_LOG] Found sector: $sector")

        return when {
            sector != null && !date.isAfter(LocalDate.now()) -> {
                // Calculate revenue based on sector data
                val basePrice = sector.basePrice
                val occupiedSpots = 5 // Assuming 5 spots were occupied for this test
                val revenue = basePrice.multiply(BigDecimal(occupiedSpots))

                println("[DEBUG_LOG] Returning revenue: $revenue BRL")
                Optional.of(MockRevenue(revenue, "BRL"))
            }

            sector != null -> {
                // Return empty for future dates
                println("[DEBUG_LOG] Future date, returning empty revenue")
                Optional.empty()
            }

            else -> {
                // Throw exception for non-existent sectors
                println("[DEBUG_LOG] Sector not found: $sectorCode")
                throw NoSuchElementException("Sector not found: $sectorCode")
            }
        }
    }

    // Stub methods for other SectorService methods that might be called
    fun getDefaultSector(): Any? = null
    fun getSectorByCode(code: String): Any? = garageData[code]
    fun getSectorByCodeWithSpots(code: String): Any? = null
    fun calculateOccupancyPercentage(code: String): Double = 0.0
    fun isSectorFull(code: String): Boolean = false
    fun recordRevenue(sectorCode: String, amount: BigDecimal, date: LocalDate): Any? = null
    fun getRevenueForDateRange(sectorCode: String, startDate: LocalDate, endDate: LocalDate): List<Any> = emptyList()
}

// Simple class to mock Revenue
class MockRevenue(val amount: BigDecimal, val currency: String)

// Class to represent a garage sector
data class GarageSector(
    val code: String,
    val basePrice: BigDecimal,
    val maxCapacity: Int
)
