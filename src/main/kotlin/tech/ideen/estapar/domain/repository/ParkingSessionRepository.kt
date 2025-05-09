package tech.ideen.estapar.domain.repository

import io.micronaut.data.annotation.Join
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import tech.ideen.estapar.domain.model.ParkingSession
import tech.ideen.estapar.domain.model.Spot
import tech.ideen.estapar.domain.model.Vehicle
import java.util.Optional
import java.util.UUID

/**
 * Repository for managing ParkingSession entities.
 */
@JdbcRepository(dialect = Dialect.POSTGRES)
interface ParkingSessionRepository : CrudRepository<ParkingSession, UUID> {

    /**
     * Finds the active session for the specified vehicle.
     *
     * @param vehicle The vehicle to find the active session for
     * @param active The active status to filter by (true for active sessions)
     * @return An Optional containing the active session, or empty if none exists
     */
    @Join(value = "parkedSpot", type = Join.Type.LEFT_FETCH)
    @Join(value = "parkedSpot.sector", type = Join.Type.LEFT_FETCH)
    fun findByVehicleAndActive(vehicle: Vehicle, active: Boolean): Optional<ParkingSession>

    /**
     * Finds all sessions where the vehicle is parked in the specified spot.
     *
     * @param parkedSpot The spot to find sessions for
     * @return List of sessions
     */
    fun findByParkedSpot(parkedSpot: Spot): List<ParkingSession>

}