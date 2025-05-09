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
import tech.ideen.estapar.api.dto.revenue.RevenueRequestDTO
import tech.ideen.estapar.api.dto.revenue.RevenueResponseDTO
import tech.ideen.estapar.exception.ErrorResponse
import tech.ideen.estapar.service.SectorService
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Controller for getting revenue information.
 */
@Controller("/revenue")
@Tag(name = "Revenue", description = "API for getting revenue information")
class RevenueController(private val sectorService: SectorService) {

    private val logger = LoggerFactory.getLogger(RevenueController::class.java)

    /**
     * Gets the revenue for a sector on a specific date.
     *
     * @param request The revenue request
     * @return HTTP response with the revenue information
     */
    @Post(consumes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    @Operation(
        summary = "Get revenue",
        description = "Gets the revenue for a sector on a specific date"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Revenue retrieved successfully",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = RevenueResponseDTO::class))]
    )
    @ApiResponse(
        responseCode = "404",
        description = "Sector not found or no revenue for the specified date",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
    )
    @ApiResponse(
        responseCode = "500",
        description = "Internal server error",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
    )
    fun getRevenue(@Body request: RevenueRequestDTO): HttpResponse<RevenueResponseDTO> {
        logger.info("Getting revenue for sector: ${request.sector}, date: ${request.date}")

        val date = LocalDate.parse(request.date, DateTimeFormatter.ISO_DATE)
        val revenueOpt = sectorService.getRevenue(request.sector, date)

        if (revenueOpt.isPresent) {
            val revenue = revenueOpt.get()
            return HttpResponse.ok(
                RevenueResponseDTO(
                    amount = revenue.amount,
                    currency = revenue.currency
                )
            )
        } else {
            // No revenue found for the specified date, return zero
            return HttpResponse.ok(
                RevenueResponseDTO(
                    amount = BigDecimal.ZERO,
                    currency = "BRL"
                )
            )
        }

    }
}