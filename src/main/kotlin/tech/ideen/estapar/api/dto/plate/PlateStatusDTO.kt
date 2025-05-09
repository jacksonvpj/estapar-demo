package tech.ideen.estapar.api.dto.plate

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * DTO for plate status request.
 */
@Introspected
@Schema(description = "Request for vehicle status by license plate")
data class PlateStatusRequestDTO(
    @JsonProperty("license_plate")
    @Schema(description = "Vehicle license plate", required = true, example = "ZUL0001")
    val licensePlate: String
)

/**
 * DTO for plate status response.
 */
@Introspected
@Schema(description = "Response with vehicle status information")
data class PlateStatusResponseDTO(
    @JsonProperty("license_plate")
    @Schema(description = "Vehicle license plate", required = true, example = "ZUL0001")
    val licensePlate: String,

    @JsonProperty("price_until_now")
    @Schema(description = "Current price for the parking session", required = false, example = "10.50")
    val priceUntilNow: BigDecimal? = null,

    @JsonProperty("entry_time")
    @Schema(description = "Time when the vehicle entered", required = false, example = "2025-01-01T12:00:00.000Z")
    val entryTime: LocalDateTime? = null,

    @JsonProperty("time_parked")
    @Schema(description = "Time when the vehicle was parked", required = false, example = "2025-01-01T12:05:00.000Z")
    val timeParked: LocalDateTime? = null,

    @JsonProperty("lat")
    @Schema(description = "Latitude of the parking spot", required = false, example = "-23.561684")
    val latitude: Double? = null,

    @JsonProperty("lng")
    @Schema(description = "Longitude of the parking spot", required = false, example = "-46.655981")
    val longitude: Double? = null,

    @JsonProperty("message")
    @Schema(
        description = "Message when vehicle is not parked",
        required = false,
        example = "Vehicle is not currently parked"
    )
    val message: String? = null
)