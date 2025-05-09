package tech.ideen.estapar.api.dto.spot

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/**
 * DTO for spot status request.
 */
@Introspected
@Schema(description = "Request for spot status by location")
data class SpotStatusRequestDTO(
    @JsonProperty("lat")
    @Schema(description = "Latitude of the parking spot", required = true, example = "-23.561684")
    val latitude: Double,

    @JsonProperty("lng")
    @Schema(description = "Longitude of the parking spot", required = true, example = "-46.655981")
    val longitude: Double
)

/**
 * DTO for spot status response.
 */
@Introspected
@Schema(description = "Response with spot status information")
data class SpotStatusResponseDTO(
    @JsonProperty("occupied")
    @Schema(description = "Whether the spot is occupied", required = true, example = "true")
    val occupied: Boolean,

    @JsonProperty("entry_time")
    @Schema(
        description = "Time when the vehicle entered the spot",
        required = false,
        example = "2025-01-01T12:00:00.000Z"
    )
    val entryTime: LocalDateTime? = null,

    @JsonProperty("time_parked")
    @Schema(
        description = "Duration the vehicle has been parked",
        required = false,
        example = "2025-01-01T12:00:00.000Z"
    )
    val timeParked: LocalDateTime? = null,

    @JsonProperty("message")
    @Schema(description = "Error message if any", required = false, example = "Spot not found")
    val message: String? = null
)