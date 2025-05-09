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
import tech.ideen.estapar.api.dto.spot.SpotStatusRequestDTO
import tech.ideen.estapar.api.dto.spot.SpotStatusResponseDTO
import tech.ideen.estapar.exception.ErrorResponse
import tech.ideen.estapar.service.SpotService
import java.time.LocalDateTime

/**
 * Controller for checking spot status by location.
 */
@Controller("/spot-status")
@Tag(name = "Spot Status", description = "API for checking spot status by location")
class SpotStatusController(private val spotService: SpotService) {

    private val logger = LoggerFactory.getLogger(SpotStatusController::class.java)

    /**
     * Gets the status of a spot by its location.
     *
     * @param request The spot status request
     * @return HTTP response with the spot status
     */
    @Post(consumes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    @Operation(
        summary = "Get spot status",
        description = "Gets the status of a spot by its location"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Spot status retrieved successfully",
        content = [Content(
            mediaType = "application/json",
            schema = Schema(implementation = SpotStatusResponseDTO::class)
        )]
    )
    @ApiResponse(
        responseCode = "404",
        description = "Spot not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
    )
    @ApiResponse(
        responseCode = "500",
        description = "Internal server error",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
    )
    fun getSpotStatus(@Body request: SpotStatusRequestDTO): HttpResponse<SpotStatusResponseDTO> {
        logger.info("Getting status for spot at location: ${request.latitude}, ${request.longitude}")

        val status = spotService.getSpotStatus(request.latitude, request.longitude)

        val response = SpotStatusResponseDTO(
            occupied = status["occupied"] as Boolean,
            entryTime = status["entry_time"] as? LocalDateTime,
            timeParked = status["time_parked"] as? LocalDateTime
        )

        return HttpResponse.ok(response)

    }
}