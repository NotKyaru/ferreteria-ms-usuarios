package cl.duoc.ferreteria.usuarios.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/** Título de Swagger y botón "Authorize" para pegar el access token. */
@Configuration
@OpenAPIDefinition(info = @Info(title = "ms-usuarios · Ferretería Los Maestros", version = "v1",
        description = "Registro, login, renovación de JWT y administración de usuarios"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig { }
