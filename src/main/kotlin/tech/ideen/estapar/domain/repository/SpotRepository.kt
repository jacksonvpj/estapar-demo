package tech.ideen.estapar.domain.repository

import io.micronaut.data.annotation.Join
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import tech.ideen.estapar.domain.model.Spot
import java.util.Optional

/**
 * Repository for managing Spot entities.
 */
@JdbcRepository(dialect = Dialect.POSTGRES)
interface SpotRepository : CrudRepository<Spot, Int> {

    /**
     * Finds a spot by its latitude and longitude.
     *
     * @param latitude The latitude
     * @param longitude The longitude
     * @return An Optional containing the spot, or empty if none exists
     */
    @Join(value = "sector", type = Join.Type.FETCH)
    fun findByLatitudeAndLongitude(latitude: Double, longitude: Double): Optional<Spot>

}