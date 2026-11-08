package pe.techsupport.common.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuración común. El servidor OpenAPI es "/" para que "Try it out" de Swagger UI pase siempre
 * por el API Gateway (único punto de entrada).
 */
@Configuration
@EnableConfigurationProperties(CommonProperties.class)
@EnableScheduling
@OpenAPIDefinition(
        servers = @Server(url = "/", description = "API Gateway"),
        security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class CommonConfig {
}
