package tech.ideen.estapar.domain.repository

import io.micronaut.data.annotation.Join
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import tech.ideen.estapar.domain.model.Sector
import java.util.Optional

/**
 * Repository for managing Sector entities.
 */
@JdbcRepository(dialect = Dialect.POSTGRES)
interface SectorRepository : CrudRepository<Sector, String> {

    /**
     * Finds a sector by its code.
     *
     * @param code The sector code
     * @return An Optional containing the sector, or empty if none exists
     */
    @Join(value = "garage", type = Join.Type.INNER)
    @Join(value = "spots", type = Join.Type.LEFT_FETCH)
    fun findByCode(code: String): Optional<Sector>

    /**
     * Retrieves all Sector entities from the repository, including related Spot entities
     * via a left-fetch join.
     *
     * @return A list of all Sector entities with their associated Spot entities.
     */
    @Join(value = "spots", type = Join.Type.LEFT_FETCH)
    override fun findAll(): List<Sector>
}