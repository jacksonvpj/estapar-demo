package tech.ideen.estapar.api.controller

import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import tech.ideen.estapar.api.dto.plate.PlateStatusRequestDTO
import tech.ideen.estapar.api.dto.plate.PlateStatusResponseDTO
import tech.ideen.estapar.exception.ErrorResponse
import tech.ideen.estapar.service.VehicleService
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Controller for checking vehicle status by license plate.
 */
@Controller("/plate-status")
@Tag(name = "Plate Status", description = "API for checking vehicle status by license plate")
class PlateStatusController(private val vehicleService: VehicleService) {

    private val logger = LoggerFactory.getLogger(PlateStatusController::class.java)

    /**
     * Gets the status of a vehicle by its license plate.
     *
     * @param request The plate status request
     * @return HTTP response with the vehicle status
     */
    @Post(consumes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    @Operation(
        summary = "Get vehicle status",
        description = "Gets the status of a vehicle by its license plate"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Vehicle status retrieved successfully",
        content = [Content(
            mediaType = "application/json",
            schema = Schema(implementation = PlateStatusResponseDTO::class)
        )]
    )
    @ApiResponse(
        responseCode = "404",
        description = "Vehicle not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
    )
    @ApiResponse(
        responseCode = "500",
        description = "Internal server error",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
    )
    fun getPlateStatus(@Body request: PlateStatusRequestDTO): HttpResponse<PlateStatusResponseDTO> {
        logger.info("Getting status for vehicle: ${request.licensePlate}")

        val status = vehicleService.getVehicleStatus(request.licensePlate)

        val response = PlateStatusResponseDTO(
            licensePlate = status["license_plate"] as String,
            priceUntilNow = status["price_until_now"] as? BigDecimal,
            entryTime = status["entry_time"] as? LocalDateTime,
            timeParked = status["time_parked"] as? LocalDateTime,
            latitude = status["lat"] as? Double,
            longitude = status["lng"] as? Double,
            message = status["message"] as? String
        )

        return HttpResponse.ok(response)
    }
}