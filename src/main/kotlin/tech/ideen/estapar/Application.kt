package tech.ideen.estapar

import io.micronaut.runtime.Micronaut.run
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Contact
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.info.License

@OpenAPIDefinition(
    info = Info(
        title = "Estapar API",
        version = "1.0",
        description = "API para o sistema Estapar",
        contact = Contact(
            name = "Jackson Valadares",
            email = "jackson.valadares@ideen.tech"
        ),
        license = License(
            name = "Apache 2.0",
            url = "https://www.apache.org/licenses/LICENSE-2.0"
        )
    )
)
object Application {
}

fun main(args: Array<String>) {
    run(*args)
}

