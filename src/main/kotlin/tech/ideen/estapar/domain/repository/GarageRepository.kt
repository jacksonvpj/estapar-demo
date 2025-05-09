package tech.ideen.estapar.domain.repository

import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import tech.ideen.estapar.domain.model.Garage
import java.util.UUID

/**
 * Repository for managing Garage entities.
 */
@JdbcRepository(dialect = Dialect.POSTGRES)
interface GarageRepository : CrudRepository<Garage, UUID>