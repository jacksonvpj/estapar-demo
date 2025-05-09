package tech.ideen.estapar.service

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import tech.ideen.estapar.api.dto.webhook.EntryEventDTO
import tech.ideen.estapar.api.dto.webhook.ExitEventDTO
import tech.ideen.estapar.api.dto.webhook.ParkedEventDTO
import tech.ideen.estapar.config.EstaparTestContainer
import tech.ideen.estapar.domain.model.EventType
import tech.ideen.estapar.domain.model.ParkingEvent
import tech.ideen.estapar.domain.model.ParkingSession
import tech.ideen.estapar.domain.model.Sector
import tech.ideen.estapar.domain.model.Spot
import tech.ideen.estapar.domain.model.Vehicle
import tech.ideen.estapar.domain.repository.ParkingEventRepository
import tech.ideen.estapar.domain.repository.ParkingSessionRepository
import tech.ideen.estapar.domain.repository.SpotRepository
import tech.ideen.estapar.domain.repository.VehicleRepository
import tech.ideen.estapar.exception.EstaparException
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Optional
import java.util.UUID

@MicronautTest
class VehicleServiceTest: EstaparTestContainer() {

    private lateinit var vehicleRepository: VehicleRepository
    private lateinit var parkingEventRepository: ParkingEventRepository
    private lateinit var parkingSessionRepository: ParkingSessionRepository
    private lateinit var spotRepository: SpotRepository
    private lateinit var sectorService: SectorService
    private lateinit var vehicleService: VehicleService

    @BeforeEach
    fun setup() {
        vehicleRepository = mock(VehicleRepository::class.java)
        parkingEventRepository = mock(ParkingEventRepository::class.java)
        parkingSessionRepository = mock(ParkingSessionRepository::class.java)
        spotRepository = mock(SpotRepository::class.java)
        sectorService = mock(SectorService::class.java)

        vehicleService = VehicleService(
            vehicleRepository,
            parkingEventRepository,
            parkingSessionRepository,
            spotRepository,
            sectorService
        )
    }

    @Test
    fun `processEntryEvent should create parking event and session`() {
        // Arrange
        val licensePlate = "ZUL0001"
        val entryTimeStr = "2025-01-01T12:00:00.000Z"
        val entryTime = ZonedDateTime.parse(entryTimeStr, DateTimeFormatter.ISO_DATE_TIME).toLocalDateTime()
        val entryEvent = EntryEventDTO(licensePlate = licensePlate, entryTime = entryTimeStr, eventType = EventType.ENTRY)

        val vehicle = createSampleVehicle(licensePlate)

        // Create a mock sector instead of a real one
        val sector = mock(Sector::class.java)
        val priceFactor = 0.9 // 10% discount for low occupancy
        whenever(sector.calculatePriceFactor()).thenReturn(priceFactor)
        whenever(sector.code).thenReturn("A")

        val parkingEvent = createSampleParkingEvent(EventType.ENTRY, entryTime, vehicle)

        whenever(vehicleRepository.findByLicensePlate(licensePlate)).thenReturn(Optional.empty())
        whenever(vehicleRepository.save(any())).thenReturn(vehicle)
        whenever(parkingEventRepository.save(any())).thenReturn(parkingEvent)
        whenever(parkingSessionRepository.save(any())).thenAnswer { invocation -> invocation.getArgument(0) }

        // Act
        val result = vehicleService.processEntryEvent(entryEvent)

        // Assert
        assertEquals(EventType.ENTRY, result.eventType)
        assertEquals(entryTime, result.eventTime)
        assertEquals(vehicle, result.vehicle)

        verify(vehicleRepository).save(any())
        verify(parkingEventRepository).save(any())
        verify(parkingSessionRepository).save(any())
    }

    @Test
    fun `processParkedEvent should update parking session with spot`() {
        // Arrange
        val licensePlate = "ZUL0001"
        val latitude = -23.561684
        val longitude = -46.655981
        val parkedEvent = ParkedEventDTO(
            licensePlate = licensePlate,
            latitude = latitude,
            longitude = longitude,
            eventType = EventType.PARKED
        )

        val vehicle = createSampleVehicle(licensePlate)
        val spot = createSampleSpot(latitude, longitude)
        val parkingEvent = createSampleParkingEvent(EventType.PARKED, LocalDateTime.now(), vehicle, spot)
        val parkingSession = createSampleSession(vehicle, spot)

        whenever(vehicleRepository.findByLicensePlate(licensePlate)).thenReturn(Optional.of(vehicle))
        whenever(spotRepository.findByLatitudeAndLongitude(latitude, longitude)).thenReturn(Optional.of(spot))
        whenever(parkingEventRepository.save(any())).thenReturn(parkingEvent)
        whenever(parkingSessionRepository.findByVehicleAndActive(vehicle, true)).thenReturn(Optional.of(parkingSession))
        whenever(parkingSessionRepository.update(any())).thenReturn(parkingSession)
        whenever(spotRepository.update(any())).thenReturn(spot)

        // Act
        val result = vehicleService.processParkedEvent(parkedEvent)

        // Assert
        assertEquals(EventType.PARKED, result.eventType)
        assertEquals(vehicle, result.vehicle)
        assertEquals(spot, result.spot)

        verify(spotRepository).update(any())
        verify(parkingEventRepository).save(any())
        verify(parkingSessionRepository).update(any())
    }

    @Test
    fun `processExitEvent should close parking session and record revenue`() {
        // Arrange
        val licensePlate = "ZUL0001"
        val exitTimeStr = "2025-01-01T14:00:00.000Z"
        val exitTime = ZonedDateTime.parse(exitTimeStr, DateTimeFormatter.ISO_DATE_TIME).toLocalDateTime()
        val exitEvent = ExitEventDTO(licensePlate = licensePlate, exitTime = exitTimeStr, eventType = EventType.EXIT)

        val vehicle = createSampleVehicle(licensePlate)
        val spot = createSampleSpot()
        val sector = spot.sector
        val parkingEvent = createSampleParkingEvent(EventType.EXIT, exitTime, vehicle)

        // Create a session with appliedPriceFactor set
        val entryTime = exitTime.minusHours(2) // 2 hours parking duration
        val parkingSession = createSampleSession(vehicle, spot, entryTime)
        parkingSession.appliedPriceFactor = BigDecimal("1.0") // Set a default price factor

        // Mock the calculatePrice method to return a fixed value
        val price = BigDecimal("25.00")

        // Use doAnswer to mock the close method behavior
        whenever(parkingSessionRepository.findByVehicleAndActive(vehicle, true)).thenReturn(Optional.of(parkingSession))
        whenever(parkingSessionRepository.update(any())).thenAnswer { invocation ->
            val session = invocation.getArgument<ParkingSession>(0)
            // Verify that the session is closed
            assertEquals(false, session.active)
            assertEquals(exitTime, session.exitTime)
            // Set the price manually since we can't mock the calculatePrice method directly
            session.price = price
            session
        }

        whenever(vehicleRepository.findByLicensePlate(licensePlate)).thenReturn(Optional.of(vehicle))
        whenever(parkingEventRepository.save(any())).thenReturn(parkingEvent)
        whenever(spotRepository.update(any())).thenReturn(spot)

        // Act
        val result = vehicleService.processExitEvent(exitEvent)

        // Assert
        assertEquals(EventType.EXIT, result.eventType)
        assertEquals(exitTime, result.eventTime)
        assertEquals(vehicle, result.vehicle)

        verify(parkingEventRepository).save(any())
        verify(parkingSessionRepository).update(any())
        verify(spotRepository).update(any())
        verify(sectorService).recordRevenue(eq(sector.code!!), eq(price), eq(exitTime.toLocalDate()))
    }

    @Test
    fun `getVehicleStatus should return active session details when vehicle is parked`() {
        // Arrange
        val licensePlate = "ZUL0001"
        val vehicle = createSampleVehicle(licensePlate)
        val spot = createSampleSpot()
        val entryTime = LocalDateTime.now().minusHours(2)
        val price = BigDecimal("25.00")

        // Create a mock session instead of a real one
        val parkingSession = mock(ParkingSession::class.java)
        whenever(parkingSession.entryTime).thenReturn(entryTime)
        whenever(parkingSession.parkedSpot).thenReturn(spot)
        whenever(parkingSession.calculatePrice()).thenReturn(price)

        whenever(vehicleRepository.findByLicensePlate(licensePlate)).thenReturn(Optional.of(vehicle))
        whenever(parkingSessionRepository.findByVehicleAndActive(vehicle, true)).thenReturn(Optional.of(parkingSession))

        // Act
        val result = vehicleService.getVehicleStatus(licensePlate)

        // Assert
        assertEquals(licensePlate, result["license_plate"])
        assertEquals(price, result["price_until_now"])
        assertEquals(entryTime, result["entry_time"])
        assertEquals(entryTime, result["time_parked"])
        assertEquals(spot.latitude, result["lat"])
        assertEquals(spot.longitude, result["lng"])
    }

    @Test
    fun `getVehicleStatus should return message when vehicle is not parked`() {
        // Arrange
        val licensePlate = "ZUL0001"
        val vehicle = createSampleVehicle(licensePlate)

        whenever(vehicleRepository.findByLicensePlate(licensePlate)).thenReturn(Optional.of(vehicle))
        whenever(parkingSessionRepository.findByVehicleAndActive(vehicle, true)).thenReturn(Optional.empty())

        // Act
        val result = vehicleService.getVehicleStatus(licensePlate)

        // Assert
        assertEquals(licensePlate, result["license_plate"])
        assertEquals("Vehicle is not currently parked", result["message"])
    }

    @Test
    fun `getVehicleStatus should throw exception when vehicle does not exist`() {
        // Arrange
        val licensePlate = "ZUL0001"

        whenever(vehicleRepository.findByLicensePlate(licensePlate)).thenReturn(Optional.empty())

        // Act & Assert
        assertThrows<NoSuchElementException> { vehicleService.getVehicleStatus(licensePlate) }
    }

    @Test
    fun `processParkedEvent should apply 10 percent discount when occupancy is below 25 percent`() {
        // Arrange
        val licensePlate = "ZUL0001"
        val latitude = -23.561684
        val longitude = -46.655981
        val parkedEvent = ParkedEventDTO(
            licensePlate = licensePlate,
            latitude = latitude,
            longitude = longitude,
            eventType = EventType.PARKED
        )

        val vehicle = createSampleVehicle(licensePlate)

        // Create a sector with a mock to control the calculatePriceFactor method
        val sector = createSampleSector()
        val priceFactor = 0.9 // 10% discount
        val sectorMock = mock(Sector::class.java)
        whenever(sectorMock.calculatePriceFactor()).thenReturn(priceFactor)
        whenever(sectorMock.code).thenReturn(sector.code)

        // Create a spot with the mocked sector
        val spot = Spot(
            id = 1,
            latitude = latitude,
            longitude = longitude,
            sector = sectorMock,
            occupied = false
        )

        val parkingEvent = createSampleParkingEvent(EventType.PARKED, LocalDateTime.now(), vehicle, spot)

        // Create a different spot for the initial session
        val initialSpot = createSampleSpot(-23.561685, -46.655982)
        val parkingSession = createSampleSession(vehicle, initialSpot)

        whenever(vehicleRepository.findByLicensePlate(licensePlate)).thenReturn(Optional.of(vehicle))
        whenever(spotRepository.findByLatitudeAndLongitude(latitude, longitude)).thenReturn(Optional.of(spot))
        whenever(parkingEventRepository.save(any())).thenReturn(parkingEvent)
        whenever(parkingSessionRepository.findByVehicleAndActive(vehicle, true)).thenReturn(Optional.of(parkingSession))
        whenever(parkingSessionRepository.update(any())).thenAnswer { invocation ->
            val session = invocation.getArgument<ParkingSession>(0)
            assertEquals(BigDecimal(priceFactor), session.appliedPriceFactor)
            session
        }
        whenever(spotRepository.update(any())).thenReturn(spot)

        // Act
        val result = vehicleService.processParkedEvent(parkedEvent)

        // Assert
        assertEquals(EventType.PARKED, result.eventType)
        assertEquals(vehicle, result.vehicle)
        assertEquals(spot, result.spot)

        verify(spotRepository).update(any())
        verify(parkingEventRepository).save(any())
        verify(parkingSessionRepository).update(any())
    }

    @Test
    fun `processParkedEvent should apply 10 percent increase when occupancy is between 50 and 75 percent`() {
        // Arrange
        val licensePlate = "ZUL0001"
        val latitude = -23.561684
        val longitude = -46.655981
        val parkedEvent = ParkedEventDTO(
            licensePlate = licensePlate,
            latitude = latitude,
            longitude = longitude,
            eventType = EventType.PARKED
        )

        val vehicle = createSampleVehicle(licensePlate)

        // Create a sector with a mock to control the calculatePriceFactor method
        val sector = createSampleSector()
        val priceFactor = 1.1 // 10% increase
        val sectorMock = mock(Sector::class.java)
        whenever(sectorMock.calculatePriceFactor()).thenReturn(priceFactor)
        whenever(sectorMock.code).thenReturn(sector.code)

        // Create a spot with the mocked sector
        val spot = Spot(
            id = 1,
            latitude = latitude,
            longitude = longitude,
            sector = sectorMock,
            occupied = false
        )

        val parkingEvent = createSampleParkingEvent(EventType.PARKED, LocalDateTime.now(), vehicle, spot)

        // Create a different spot for the initial session
        val initialSpot = createSampleSpot(-23.561685, -46.655982)
        val parkingSession = createSampleSession(vehicle, initialSpot)

        whenever(vehicleRepository.findByLicensePlate(licensePlate)).thenReturn(Optional.of(vehicle))
        whenever(spotRepository.findByLatitudeAndLongitude(latitude, longitude)).thenReturn(Optional.of(spot))
        whenever(parkingEventRepository.save(any())).thenReturn(parkingEvent)
        whenever(parkingSessionRepository.findByVehicleAndActive(vehicle, true)).thenReturn(Optional.of(parkingSession))
        whenever(parkingSessionRepository.update(any())).thenAnswer { invocation ->
            val session = invocation.getArgument<ParkingSession>(0)
            assertEquals(BigDecimal(priceFactor), session.appliedPriceFactor)
            session
        }
        whenever(spotRepository.update(any())).thenReturn(spot)

        // Act
        val result = vehicleService.processParkedEvent(parkedEvent)

        // Assert
        assertEquals(EventType.PARKED, result.eventType)
        assertEquals(vehicle, result.vehicle)
        assertEquals(spot, result.spot)

        verify(spotRepository).update(any())
        verify(parkingEventRepository).save(any())
        verify(parkingSessionRepository).update(any())
    }

    @Test
    fun `processParkedEvent should apply 25 percent increase when occupancy is between 75 and 100 percent`() {
        // Arrange
        val licensePlate = "ZUL0001"
        val latitude = -23.561684
        val longitude = -46.655981
        val parkedEvent = ParkedEventDTO(
            licensePlate = licensePlate,
            latitude = latitude,
            longitude = longitude,
            eventType = EventType.PARKED
        )

        val vehicle = createSampleVehicle(licensePlate)

        // Create a sector with a mock to control the calculatePriceFactor method
        val sector = createSampleSector()
        val priceFactor = 1.25 // 25% increase
        val sectorMock = mock(Sector::class.java)
        whenever(sectorMock.calculatePriceFactor()).thenReturn(priceFactor)
        whenever(sectorMock.code).thenReturn(sector.code)

        // Create a spot with the mocked sector
        val spot = Spot(
            id = 1,
            latitude = latitude,
            longitude = longitude,
            sector = sectorMock,
            occupied = false
        )

        val parkingEvent = createSampleParkingEvent(EventType.PARKED, LocalDateTime.now(), vehicle, spot)

        // Create a different spot for the initial session
        val initialSpot = createSampleSpot(-23.561685, -46.655982)
        val parkingSession = createSampleSession(vehicle, initialSpot)

        whenever(vehicleRepository.findByLicensePlate(licensePlate)).thenReturn(Optional.of(vehicle))
        whenever(spotRepository.findByLatitudeAndLongitude(latitude, longitude)).thenReturn(Optional.of(spot))
        whenever(parkingEventRepository.save(any())).thenReturn(parkingEvent)
        whenever(parkingSessionRepository.findByVehicleAndActive(vehicle, true)).thenReturn(Optional.of(parkingSession))
        whenever(parkingSessionRepository.update(any())).thenAnswer { invocation ->
            val session = invocation.getArgument<ParkingSession>(0)
            assertEquals(BigDecimal(priceFactor), session.appliedPriceFactor)
            session
        }
        whenever(spotRepository.update(any())).thenReturn(spot)

        // Act
        val result = vehicleService.processParkedEvent(parkedEvent)

        // Assert
        assertEquals(EventType.PARKED, result.eventType)
        assertEquals(vehicle, result.vehicle)
        assertEquals(spot, result.spot)

        verify(spotRepository).update(any())
        verify(parkingEventRepository).save(any())
        verify(parkingSessionRepository).update(any())
    }

    @Test
    fun `processParkedEvent should throw exception when spot is not found`() {
        // Arrange
        val licensePlate = "ZUL0001"
        val latitude = -23.561684
        val longitude = -46.655981
        val parkedEvent = ParkedEventDTO(
            licensePlate = licensePlate,
            latitude = latitude,
            longitude = longitude,
            eventType = EventType.PARKED
        )

        val vehicle = createSampleVehicle(licensePlate)

        whenever(vehicleRepository.findByLicensePlate(licensePlate)).thenReturn(Optional.of(vehicle))
        whenever(spotRepository.findByLatitudeAndLongitude(latitude, longitude)).thenReturn(Optional.empty())

        // Act & Assert
        assertThrows<IllegalStateException> { vehicleService.processParkedEvent(parkedEvent) }
    }

    @Test
    fun `processParkedEvent should throw exception when no active session is found`() {
        // Arrange
        val licensePlate = "ZUL0001"
        val latitude = -23.561684
        val longitude = -46.655981
        val parkedEvent = ParkedEventDTO(
            licensePlate = licensePlate,
            latitude = latitude,
            longitude = longitude,
            eventType = EventType.PARKED
        )

        val vehicle = createSampleVehicle(licensePlate)
        val spot = createSampleSpot(latitude, longitude)

        whenever(vehicleRepository.findByLicensePlate(licensePlate)).thenReturn(Optional.of(vehicle))
        whenever(spotRepository.findByLatitudeAndLongitude(latitude, longitude)).thenReturn(Optional.of(spot))
        whenever(parkingSessionRepository.findByVehicleAndActive(vehicle, true)).thenReturn(Optional.empty())

        // Act & Assert
        assertThrows<IllegalStateException> { vehicleService.processParkedEvent(parkedEvent) }
    }

    @Test
    fun `processExitEvent should throw exception when vehicle is not found`() {
        // Arrange
        val licensePlate = "ZUL0001"
        val exitTimeStr = "2025-01-01T14:00:00.000Z"
        val exitEvent = ExitEventDTO(licensePlate = licensePlate, exitTime = exitTimeStr, eventType = EventType.EXIT)

        whenever(vehicleRepository.findByLicensePlate(licensePlate)).thenReturn(Optional.empty())

        // Act & Assert
        assertThrows<NoSuchElementException> { vehicleService.processExitEvent(exitEvent) }
    }

    @Test
    fun `processExitEvent should throw exception when no active session is found`() {
        // Arrange
        val licensePlate = "ZUL0001"
        val exitTimeStr = "2025-01-01T14:00:00.000Z"
        val exitEvent = ExitEventDTO(licensePlate = licensePlate, exitTime = exitTimeStr, eventType = EventType.EXIT)

        val vehicle = createSampleVehicle(licensePlate)

        whenever(vehicleRepository.findByLicensePlate(licensePlate)).thenReturn(Optional.of(vehicle))
        whenever(parkingSessionRepository.findByVehicleAndActive(vehicle, true)).thenReturn(Optional.empty())

        // Act & Assert
        assertThrows<NoSuchElementException> { vehicleService.processExitEvent(exitEvent) }
    }

    @Test
    fun `processEntryEvent should throw exception when all sectors are full`() {
        // Arrange
        val licensePlate = "ZUL0001"
        val entryTimeStr = "2025-01-01T12:00:00.000Z"
        val entryEvent = EntryEventDTO(licensePlate = licensePlate, entryTime = entryTimeStr, eventType = EventType.ENTRY)

        // Mock sectorService to return true for areAllSectorsFull
        whenever(sectorService.areAllSectorsFull()).thenReturn(true)

        // Act & Assert
        val exception = assertThrows<EstaparException> { vehicleService.processEntryEvent(entryEvent) }
        assertEquals("Vehicle: $licensePlate. All sectors are full at the moment $entryTimeStr. Please try again later.", exception.message)
    }

    @Test
    fun `parseDateTime should parse ZonedDateTime with zone information`() {
        // Arrange
        val dateTimeStr = "2025-01-01T12:00:00.000Z"
        val expectedDateTime = ZonedDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_DATE_TIME).toLocalDateTime()

        // Create a test entry event to use the private parseDateTime method
        val entryEvent = EntryEventDTO(licensePlate = "ZUL0001", entryTime = dateTimeStr, eventType = EventType.ENTRY)

        // Mock dependencies to avoid NullPointerExceptions
        whenever(sectorService.areAllSectorsFull()).thenReturn(false)
        whenever(vehicleRepository.findByLicensePlate(any())).thenReturn(Optional.empty())
        whenever(vehicleRepository.save(any())).thenReturn(createSampleVehicle("ZUL0001"))
        whenever(parkingEventRepository.save(any())).thenReturn(
            createSampleParkingEvent(EventType.ENTRY, expectedDateTime, createSampleVehicle("ZUL0001"))
        )

        // Act
        val result = vehicleService.processEntryEvent(entryEvent)

        // Assert
        assertEquals(expectedDateTime, result.eventTime)
    }

    @Test
    fun `parseDateTime should parse LocalDateTime without zone information`() {
        // Arrange
        val dateTimeStr = "2025-01-01T12:00:00"
        val expectedDateTime = LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME)

        // Create a test entry event to use the private parseDateTime method
        val entryEvent = EntryEventDTO(licensePlate = "ZUL0001", entryTime = dateTimeStr, eventType = EventType.ENTRY)

        // Mock dependencies to avoid NullPointerExceptions
        whenever(sectorService.areAllSectorsFull()).thenReturn(false)
        whenever(vehicleRepository.findByLicensePlate(any())).thenReturn(Optional.empty())
        whenever(vehicleRepository.save(any())).thenReturn(createSampleVehicle("ZUL0001"))
        whenever(parkingEventRepository.save(any())).thenReturn(
            createSampleParkingEvent(EventType.ENTRY, expectedDateTime, createSampleVehicle("ZUL0001"))
        )

        // Act
        val result = vehicleService.processEntryEvent(entryEvent)

        // Assert
        assertEquals(expectedDateTime, result.eventTime)
    }

    private fun createSampleVehicle(licensePlate: String): Vehicle {
        return Vehicle(licensePlate = licensePlate).apply { id = UUID.randomUUID() }
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

    private fun createSampleSpot(latitude: Double = -23.561684, longitude: Double = -46.655981): Spot {
        return Spot(
            id = 1,
            latitude = latitude,
            longitude = longitude,
            sector = createSampleSector(),
            occupied = false
        )
    }

    private fun createSampleParkingEvent(
        eventType: EventType,
        eventTime: LocalDateTime,
        vehicle: Vehicle,
        spot: Spot? = null
    ): ParkingEvent {
        return ParkingEvent(
            eventType = eventType,
            eventTime = eventTime,
            vehicle = vehicle,
            spot = spot
        )
    }

    private fun createSampleSession(
        vehicle: Vehicle,
        parkedSpot: Spot,
        entryTime: LocalDateTime = LocalDateTime.now()
    ): ParkingSession {
        return ParkingSession(
            entryTime = entryTime,
            active = true,
            vehicle = vehicle,
            parkedSpot = parkedSpot
        )
    }
}
