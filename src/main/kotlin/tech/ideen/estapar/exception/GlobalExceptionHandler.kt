package tech.ideen.estapar.exception

import io.micronaut.context.annotation.Requires
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.server.exceptions.ExceptionHandler
import io.micronaut.http.server.exceptions.response.ErrorResponseProcessor
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

@Singleton
@Requires(classes = [Exception::class, ExceptionHandler::class])
class GlobalExceptionHandler(
    private val errorResponseProcessor: ErrorResponseProcessor<Any>
) : ExceptionHandler<Exception, HttpResponse<*>> {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    override fun handle(request: HttpRequest<*>, exception: Exception): HttpResponse<*> {


        val status = when (exception) {
            is EstaparException -> HttpStatus.UNPROCESSABLE_ENTITY
            is IllegalArgumentException -> HttpStatus.BAD_REQUEST
            is NoSuchElementException -> HttpStatus.NOT_FOUND
            // Adicione outros mapeamentos de exceção conforme necessário
            else -> HttpStatus.INTERNAL_SERVER_ERROR
        }

        if (status == HttpStatus.NOT_FOUND || status == HttpStatus.UNPROCESSABLE_ENTITY) {
            logger.warn("Exception caught: ${exception.message}")
        } else {
            logger.error("Exception caught: ${exception.message}", exception)
        }

        val errorResponse = ErrorResponse(
            status = status.code,
            error = status.reason,
            message = exception.message ?: "Ocorreu um erro inesperado",
            path = request.path
        )

        return HttpResponse.status<ErrorResponse>(status).body(errorResponse)
    }
}