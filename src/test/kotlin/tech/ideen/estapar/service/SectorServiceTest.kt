package tech.ideen.estapar.service

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import tech.ideen.estapar.domain.model.Revenue
import tech.ideen.estapar.domain.model.Sector
import tech.ideen.estapar.domain.model.Spot
import tech.ideen.estapar.domain.repository.RevenueRepository
import tech.ideen.estapar.domain.repository.SectorRepository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.Optional

@MicronautTest
class SectorServiceTest {

    private lateinit var sectorRepository: SectorRepository
    private lateinit var revenueRepository: RevenueRepository
    private lateinit var sectorService: SectorService

    @BeforeEach
    fun setup() {
        sectorRepository = mock(SectorRepository::class.java)
        revenueRepository = mock(RevenueRepository::class.java)
        sectorService = SectorService(sectorRepository, revenueRepository)
    }

    @Test
    fun `getDefaultSector should return first sector when sectors exist`() {
        // Arrange
        val sector = createSampleSector()
        whenever(sectorRepository.findAll()).thenReturn(listOf(sector))

        // Act
        val result = sectorService.getDefaultSector()

        // Assert
        assertEquals(sector, result)
    }

    @Test
    fun `getDefaultSector should throw exception when no sectors exist`() {
        // Arrange
        whenever(sectorRepository.findAll()).thenReturn(emptyList())

        // Act & Assert
        assertThrows<IllegalStateException> { sectorService.getDefaultSector() }
    }

    @Test
    fun `getSectorByCode should return sector when it exists`() {
        // Arrange
        val sector = createSampleSector()
        whenever(sectorRepository.findByCode("A")).thenReturn(Optional.of(sector))

        // Act
        val result = sectorService.getSectorByCode("A")

        // Assert
        assertEquals(sector, result)
    }

    @Test
    fun `getSectorByCode should throw exception when sector does not exist`() {
        // Arrange
        whenever(sectorRepository.findByCode("A")).thenReturn(Optional.empty())

        // Act & Assert
        assertThrows<IllegalStateException> { sectorService.getSectorByCode("A") }
    }

    @Test
    fun `calculateOccupancyPercentage should return correct percentage`() {
        // Arrange
        val sector = createSampleSector()
        val spots = listOf(
            createSpot(1, true, sector),
            createSpot(2, false, sector),
            createSpot(3, true, sector),
            createSpot(4, false, sector)
        )
        sector.spots.addAll(spots)

        whenever(sectorRepository.findByCode("A")).thenReturn(Optional.of(sector))

        // Act
        val result = sectorService.calculateOccupancyPercentage("A")

        // Assert
        assertEquals(0.5, result) // 2 out of 4 spots are occupied
    }

    @Test
    fun `recordRevenue should create new revenue record when none exists`() {
        // Arrange
        val sector = createSampleSector()
        val date = LocalDate.now()
        val amount = BigDecimal("100.00")
        val newRevenue = Revenue(
            revenueDate = date,
            amount = BigDecimal.ZERO,
            sector = sector
        )

        whenever(sectorRepository.findByCode("A")).thenReturn(Optional.of(sector))
        whenever(revenueRepository.findBySectorAndRevenueDate(sector, date)).thenReturn(Optional.empty())
        whenever(revenueRepository.save(any<Revenue>())).thenReturn(newRevenue)
        whenever(revenueRepository.update(any())).thenAnswer { invocation -> invocation.getArgument(0) }

        // Act
        val result = sectorService.recordRevenue("A", amount, date)

        // Assert
        assertEquals(amount, result.amount)
        assertEquals(date, result.revenueDate)
        assertEquals(sector, result.sector)
        verify(revenueRepository).save(any())
        verify(revenueRepository).update(any())
    }

    @Test
    fun `recordRevenue should update existing revenue record when it exists`() {
        // Arrange
        val sector = createSampleSector()
        val date = LocalDate.now()
        val existingAmount = BigDecimal("50.00")
        val additionalAmount = BigDecimal("100.00")
        val expectedAmount = BigDecimal("150.00")

        val existingRevenue = Revenue(
            revenueDate = date,
            amount = existingAmount,
            sector = sector
        )

        whenever(sectorRepository.findByCode("A")).thenReturn(Optional.of(sector))
        whenever(revenueRepository.findBySectorAndRevenueDate(sector, date)).thenReturn(Optional.of(existingRevenue))
        whenever(revenueRepository.update(any())).thenAnswer { invocation -> invocation.getArgument(0) }

        // Act
        val result = sectorService.recordRevenue("A", additionalAmount, date)

        // Assert
        assertEquals(expectedAmount, result.amount)
        assertEquals(date, result.revenueDate)
        assertEquals(sector, result.sector)
        verify(revenueRepository, never()).save(any())
        verify(revenueRepository).update(any())
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

    private fun createSpot(number: Int, occupied: Boolean, sector: Sector): Spot {
        return Spot(
            id = number,
            latitude = -23.561684 + (number * 0.00001),
            longitude = -46.655981 + (number * 0.00001),
            sector = sector,
            occupied = occupied
        )
    }
}