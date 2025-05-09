package tech.ideen.estapar.api.dto.webhook

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.micronaut.core.annotation.Introspected
import io.micronaut.serde.annotation.Serdeable
import io.swagger.v3.oas.annotations.media.Schema
import tech.ideen.estapar.domain.model.EventType

/**
 * Base class for all webhook event DTOs.
 */
@Introspected
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "event_type",
    visible = true
)
@JsonSubTypes(
    JsonSubTypes.Type(value = EntryEventDTO::class, name = "ENTRY"),
    JsonSubTypes.Type(value = ParkedEventDTO::class, name = "PARKED"),
    JsonSubTypes.Type(value = ExitEventDTO::class, name = "EXIT")
)
@Schema(
    description = "Base class for all webhook events",
    oneOf = [EntryEventDTO::class, ParkedEventDTO::class, ExitEventDTO::class]
)
abstract class WebhookEventDTO {

    @get:JsonProperty("event_type")
    @get:Schema(description = "Type of event", required = true, example = "ENTRY")
    abstract val eventType: EventType
}

/**
 * DTO for vehicle entry events.
 */
@Introspected
@Schema(description = "Event for vehicle entering the garage")
@Serdeable.Serializable
@Serdeable.Deserializable
data class EntryEventDTO(
    @JsonProperty("license_plate")
    @Schema(description = "Vehicle license plate", required = true, example = "ZUL0001")
    val licensePlate: String,

    @JsonProperty("entry_time")
    @Schema(description = "Time when the vehicle entered", required = true, example = "2025-01-01T12:00:00.000Z")
    val entryTime: String,

    @JsonProperty("event_type")
    @Schema(description = "Type of event", required = true, example = "ENTRY")
    override val eventType: EventType = EventType.ENTRY
) : WebhookEventDTO()

/**
 * DTO for vehicle parked events.
 */
@Introspected
@Schema(description = "Event for vehicle parked in a spot")
@Serdeable.Serializable
@Serdeable.Deserializable
data class ParkedEventDTO(
    @JsonProperty("license_plate")
    @Schema(description = "Vehicle license plate", required = true, example = "ZUL0001")
    val licensePlate: String,

    @JsonProperty("lat")
    @Schema(description = "Latitude of the parking spot", required = true, example = "-23.561684")
    val latitude: Double,

    @JsonProperty("lng")
    @Schema(description = "Longitude of the parking spot", required = true, example = "-46.655981")
    val longitude: Double,

    @JsonProperty("event_type")
    @Schema(description = "Type of event", required = true, example = "PARKED")
    override val eventType: EventType = EventType.PARKED
) : WebhookEventDTO()

/**
 * DTO for vehicle exit events.
 */
@Introspected
@Schema(description = "Event for vehicle exiting the garage")
@Serdeable.Serializable
@Serdeable.Deserializable
data class ExitEventDTO(
    @JsonProperty("license_plate")
    @Schema(description = "Vehicle license plate", required = true, example = "ZUL0001")
    val licensePlate: String,

    @JsonProperty("exit_time")
    @Schema(description = "Time when the vehicle exited", required = true, example = "2025-01-01T12:00:00.000Z")
    val exitTime: String,

    @JsonProperty("event_type")
    @Schema(description = "Type of event", required = true, example = "EXIT")
    override val eventType: EventType = EventType.EXIT
) : WebhookEventDTO()