package tech.ideen.estapar.domain.model

/**
 * Represents the type of parking event.
 */
enum class EventType {
    /**
     * Vehicle entered the garage through a gate.
     */
    ENTRY,

    /**
     * Vehicle parked in a specific spot.
     */
    PARKED,

    /**
     * Vehicle exited the garage through a gate.
     */
    EXIT
}