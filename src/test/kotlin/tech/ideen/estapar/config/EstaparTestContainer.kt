package tech.ideen.estapar.config

import io.micronaut.test.support.TestPropertyProvider
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.wiremock.integrations.testcontainers.WireMockContainer

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class EstaparTestContainer : TestPropertyProvider {

    companion object {
        @Container
        @JvmStatic
        val postgresContainer = PostgreSQLContainer<Nothing>("postgres:15-alpine").apply {
            withDatabaseName("estapar")
            withUsername("postgres")
            withPassword("postgres")
            start()
        }

        @Container
        var wireMockServer = WireMockContainer("wiremock/wiremock").apply {
            withMappingFromResource("garage", "garage-mapping.json")
            start()
        }

    }

    override fun getProperties(): Map<String, String> {
        return mapOf(
            "datasources.default.url" to postgresContainer.jdbcUrl,
            "datasources.default.username" to postgresContainer.username,
            "datasources.default.password" to postgresContainer.password,
            "datasources.default.driverClassName" to "org.postgresql.Driver",
            "garage-simulator.url" to wireMockServer.baseUrl
        )
    }

}