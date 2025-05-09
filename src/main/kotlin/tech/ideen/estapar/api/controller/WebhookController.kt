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
import tech.ideen.estapar.api.dto.webhook.EntryEventDTO
import tech.ideen.estapar.api.dto.webhook.ExitEventDTO
import tech.ideen.estapar.api.dto.webhook.ParkedEventDTO
import tech.ideen.estapar.api.dto.webhook.WebhookEventDTO
import tech.ideen.estapar.exception.ErrorResponse
import tech.ideen.estapar.service.VehicleService

/**
 * Controller for handling webhook events from the garage simulator.
 */
@Controller("/webhook")
@Tag(name = "Webhook", description = "Webhook API for receiving events from the garage simulator")
class WebhookController(private val vehicleService: VehicleService) {

    private val logger = LoggerFactory.getLogger(WebhookController::class.java)

    /**
     * Handles webhook events from the garage simulator.
     *
     * @param event The webhook event
     * @return HTTP response
     */
    @Post(consumes = [MediaType.APPLICATION_JSON])
    @Operation(
        summary = "Handle webhook event",
        description = "Receives and processes events from the garage simulator"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Event processed successfully"
    )
    @ApiResponse(
        responseCode = "400",
        description = "Invalid event data",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
    )
    @ApiResponse(
        responseCode = "500",
        description = "Internal server error",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
    )
    fun handleWebhook(@Body event: WebhookEventDTO): HttpResponse<Map<String, String>> {
        logger.info("Received webhook event: ${event.eventType}")
        when (event) {
            is EntryEventDTO -> {
                logger.info("Processing entry event for vehicle: ${event.licensePlate}")
                vehicleService.processEntryEvent(event)
            }

            is ParkedEventDTO -> {
                logger.info("Processing parked event for vehicle: ${event.licensePlate}")
                vehicleService.processParkedEvent(event)
            }

            is ExitEventDTO -> {
                logger.info("Processing exit event for vehicle: ${event.licensePlate}")
                vehicleService.processExitEvent(event)
            }

            else -> {
                logger.warn("Unknown event type: ${event.eventType}")
                return HttpResponse.badRequest(mapOf("error" to "Unknown event type: ${event.eventType}"))
            }
        }

        return HttpResponse.ok(mapOf("status" to "Event processed successfully"))

    }
}