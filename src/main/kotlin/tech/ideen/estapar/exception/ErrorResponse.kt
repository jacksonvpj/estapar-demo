package tech.ideen.estapar.exception

import com.fasterxml.jackson.annotation.JsonFormat
import io.micronaut.serde.annotation.Serdeable
import java.time.LocalDateTime

@Serdeable
data class ErrorResponse(
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val timestamp: LocalDateTime = LocalDateTime.now()
)