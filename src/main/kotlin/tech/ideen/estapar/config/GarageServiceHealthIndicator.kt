package tech.ideen.estapar.config

import io.micronaut.core.async.publisher.Publishers
import io.micronaut.health.HealthStatus
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientException
import io.micronaut.management.health.indicator.HealthIndicator
import io.micronaut.management.health.indicator.HealthResult
import jakarta.inject.Singleton
import org.reactivestreams.Publisher

@Singleton
class GarageServiceHealthIndicator(
    @Client("http://0.0.0.0:3000") private val httpClient: HttpClient
) : HealthIndicator {

    override fun getResult(): Publisher<HealthResult> {
        return Publishers.just(
            try {
                val response = httpClient.toBlocking().exchange("/garage", String::class.java)
                if (response.status == HttpStatus.OK) {
                    HealthResult.builder("garage-service")
                        .status(HealthStatus.UP)
                        .build()
                } else {
                    HealthResult.builder("garage-service")
                        .status(HealthStatus.DOWN)
                        .details(mapOf("error" to "Status Service: ${response.status}"))
                        .build()
                }
            } catch (e: HttpClientException) {
                HealthResult.builder("garage-service")
                    .status(HealthStatus.DOWN)
                    .details(mapOf("error" to e.message))
                    .build()
            }
        )
    }
}