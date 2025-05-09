package tech.ideen.estapar.config

import io.micronaut.context.event.ApplicationEventListener
import io.micronaut.context.event.StartupEvent
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.json.JsonMapper
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import tech.ideen.estapar.domain.model.Garage
import tech.ideen.estapar.domain.model.Sector
import tech.ideen.estapar.domain.model.Spot
import tech.ideen.estapar.domain.repository.GarageRepository
import tech.ideen.estapar.domain.repository.SectorRepository
import tech.ideen.estapar.domain.repository.SpotRepository
import java.math.BigDecimal
import java.time.LocalTime
import java.util.concurrent.TimeUnit

@Singleton
class StartupServiceCaller(
    @Client("\${garage-simulator.url}") private val httpClient: HttpClient,
    private val jsonMapper: JsonMapper,
    private val garageRepository: GarageRepository,
    private val sectorRepository: SectorRepository,
    private val spotRepository: SpotRepository
) : ApplicationEventListener<StartupEvent> {

    private val logger = LoggerFactory.getLogger(StartupServiceCaller::class.java)
    private val maxRetries = 5
    private val retryDelayMs = 2000L

    @ExecuteOn(TaskExecutors.IO)
    override fun onApplicationEvent(event: StartupEvent) {
        var retries = 0
        var success = false

        while (!success && retries < maxRetries) {
            try {
                logger.info("Tentativa ${retries + 1} de chamada para o serviço garage...")
                val response = httpClient.toBlocking().exchange(
                    HttpRequest.GET<Any>("/garage"),
                    String::class.java
                )
                logger.info("Resposta do serviço garage: ${response.status}")

                // Process the response body
                val responseBody = response.body()
                if (responseBody != null) {
                    processGarageData(responseBody)
                }

                success = true
            } catch (e: Exception) {
                retries++
                logger.error("Erro ao chamar o serviço garage (tentativa $retries): ${e.message}")

                if (retries < maxRetries) {
                    logger.info("Aguardando ${retryDelayMs}ms antes da próxima tentativa...")
                    TimeUnit.MILLISECONDS.sleep(retryDelayMs)
                }
            }
        }

        if (!success) {
            logger.error("Falha ao chamar o serviço garage após $maxRetries tentativas.")
        }
    }

    private fun processGarageData(responseBody: String) {
        try {
            // Parse the JSON response
            val garageData = jsonMapper.readValue(responseBody, Map::class.java) as Map<String, Any>

            // Get or create the garage
            val garage = getOrCreateGarage()

            // Process sectors data
            @Suppress("UNCHECKED_CAST")
            val sectorsData = garageData["garage"] as? List<Map<String, Any>> ?: emptyList()
            processSectorsData(sectorsData, garage)

            // Process spots data
            @Suppress("UNCHECKED_CAST")
            val spotsData = garageData["spots"] as? List<Map<String, Any>> ?: emptyList()
            processSpotsData(spotsData)

            logger.info("Garage data processing completed successfully.")
        } catch (e: Exception) {
            logger.error("Error processing garage data: ${e.message}", e)
        }
    }

    private fun getOrCreateGarage(): Garage {
        // For simplicity, we'll use the first garage or create a new one if none exists
        val garages = garageRepository.findAll()
        return if (garages.isNotEmpty()) {
            garages.first()
        } else {
            val newGarage = Garage()
            garageRepository.save(newGarage)
        }
    }

    private fun processSectorsData(sectorsData: List<Map<String, Any>>, garage: Garage) {
        for (sectorData in sectorsData) {
            val sectorCode = sectorData["sector"] as String

            // Check if sector already exists
            val existingSector = sectorRepository.findByCode(sectorCode)

            if (existingSector.isPresent) {
                val sector = existingSector.get()
                sector.basePrice = BigDecimal((sectorData["base_price"] as Number).toString())
                sector.maxCapacity = (sectorData["max_capacity"] as Number).toInt()
                sector.openHour = LocalTime.parse(sectorData["open_hour"] as String)
                sector.closeHour = LocalTime.parse(sectorData["close_hour"] as String)
                sector.durationLimitMinutes = (sectorData["duration_limit_minutes"] as Number).toInt()
                sectorRepository.update(sector)
                logger.info("Sector with code $sectorCode updated successfully.")

                continue
            }

            // Create new sector
            val basePrice = BigDecimal((sectorData["base_price"] as Number).toString())
            val maxCapacity = (sectorData["max_capacity"] as Number).toInt()
            val openHour = LocalTime.parse(sectorData["open_hour"] as String)
            val closeHour = LocalTime.parse(sectorData["close_hour"] as String)
            val durationLimitMinutes = (sectorData["duration_limit_minutes"] as Number).toInt()

            val newSector = Sector(
                code = sectorCode,
                basePrice = basePrice,
                maxCapacity = maxCapacity,
                openHour = openHour,
                closeHour = closeHour,
                durationLimitMinutes = durationLimitMinutes,
                garage = garage
            )

            sectorRepository.save(newSector)
            logger.info("Sector with code $sectorCode created successfully.")
        }
    }

    private fun processSpotsData(spotsData: List<Map<String, Any>>) {
        for (spotData in spotsData) {
            val id = (spotData["id"] as Number).toInt()
            val sectorCode = spotData["sector"] as String
            val latitude = (spotData["lat"] as Number).toDouble()
            val longitude = (spotData["lng"] as Number).toDouble()
            val occupied = spotData["occupied"] as Boolean

            // Check if spot already exists by latitude and longitude
            val existingSpot = spotRepository.findByLatitudeAndLongitude(latitude, longitude)

            if (existingSpot.isPresent) {
                val spot = existingSpot.get()
                spot.id = id
                spot.occupied = occupied
                spot.latitude = latitude
                spot.longitude = longitude

                // Get the sector
                val sectorOptional = sectorRepository.findByCode(sectorCode)
                if (sectorOptional.isPresent) {
                    spot.sector = sectorOptional.get()
                }

                spotRepository.update(spot)
                logger.info("Spot lat $latitude e lon $longitude updated successfully.")
                continue
            }

            // Get the sector
            val sectorOptional = sectorRepository.findByCode(sectorCode)

            if (sectorOptional.isEmpty) {
                logger.error("Sector with code $sectorCode not found, unable to create spot.")
                continue
            }

            val sector = sectorOptional.get()

            // Create new spot
            val newSpot = Spot(
                id = id,
                latitude = latitude,
                longitude = longitude,
                sector = sector,
                occupied = occupied
            )

            spotRepository.save(newSpot)
            logger.info("Spot Id $id created successfully.")
        }
    }
}
