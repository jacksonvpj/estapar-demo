package tech.ideen.estapar.domain.repository

import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import tech.ideen.estapar.domain.model.Vehicle
import java.util.Optional
import java.util.UUID

/**
 * Repository for managing Vehicle entities.
 */
@JdbcRepository(dialect = Dialect.POSTGRES)
interface VehicleRepository : CrudRepository<Vehicle, UUID> {

    /**
     * Finds a vehicle by its license plate.
     *
     * @param licensePlate The license plate to search for
     * @return An Optional containing the vehicle, or empty if none exists
     */
    fun findByLicensePlate(licensePlate: String): Optional<Vehicle>
}