package tech.ideen.estapar.api.dto.revenue

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import io.micronaut.serde.annotation.Serdeable
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * DTO for revenue request.
 */
@Introspected
@Schema(description = "Request for revenue information")
@Serdeable
data class RevenueRequestDTO(
    @JsonProperty("date")
    @Schema(description = "Date to get revenue for", required = true, example = "2025-01-01")
    val date: String,

    @JsonProperty("sector")
    @Schema(description = "Sector code to get revenue for", required = true, example = "A")
    val sector: String
)

/**
 * DTO for revenue response.
 */
@Introspected
@Schema(description = "Response with revenue information")
@Serdeable.Serializable
@Serdeable.Deserializable
data class RevenueResponseDTO(
    @JsonProperty("amount")
    @Schema(description = "Revenue amount", required = true, example = "1250.50")
    val amount: BigDecimal,

    @JsonProperty("currency")
    @Schema(description = "Currency code", required = true, example = "BRL")
    val currency: String,

    @JsonProperty("timestamp")
    @Schema(description = "Timestamp of the response", required = true, example = "2025-01-01T12:00:00.000Z")
    val timestamp: LocalDateTime = LocalDateTime.now(),

    @JsonProperty("message")
    @Schema(description = "Error message if any", required = false, example = "Sector not found")
    val message: String? = null
)