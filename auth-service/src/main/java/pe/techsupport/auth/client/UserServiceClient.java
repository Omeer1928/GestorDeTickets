package pe.techsupport.auth.client;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import pe.techsupport.auth.web.dto.AuthDtos.RegisterRequest;
import pe.techsupport.common.error.ApiError;
import pe.techsupport.common.error.ApiException;
import pe.techsupport.common.http.InternalRestClientFactory;

/**
 * Cliente del User Service usado durante el registro: Auth guarda la credencial y User Service el perfil.
 * Si guardar la credencial falla, se invoca la compensación (borrado del perfil) — patrón Saga.
 */
@Component
public class UserServiceClient {

    private static final Logger log = LoggerFactory.getLogger(UserServiceClient.class);

    private final RestClient client;
    private final ObjectMapper mapper;

    public UserServiceClient(InternalRestClientFactory factory, ObjectMapper mapper,
                             @Value("${app.services.user-service-url}") String baseUrl) {
        this.client = factory.create(baseUrl, Duration.ofSeconds(2), Duration.ofSeconds(4));
        this.mapper = mapper;
    }

    @CircuitBreaker(name = "userService")
    public String createProfile(RegisterRequest req) {
        Map<String, Object> body = new HashMap<>();
        body.put("firstName", req.firstName());
        body.put("lastName", req.lastName());
        body.put("email", req.email().trim().toLowerCase());
        body.put("phone", req.phone());
        String path;
        if ("TECNICO".equals(req.userType())) {
            path = "/users/technicians";
            body.put("supportLevel", req.supportLevel() == null || req.supportLevel().isBlank() ? "N1" : req.supportLevel());
            body.put("specialties", req.specialties() == null ? List.of() : req.specialties());
            body.put("maxConcurrentTickets", req.maxConcurrentTickets() == null ? 5 : req.maxConcurrentTickets());
        } else {
            path = "/users/clients";
            body.put("companyName", req.companyName());
            body.put("ruc", req.ruc());
        }
        JsonNode created = client.post()
                .uri(path)
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    ApiError err = mapper.readValue(response.getBody(), ApiError.class);
                    throw new ApiException(HttpStatus.valueOf(response.getStatusCode().value()),
                            err.error(), err.message());
                })
                .body(JsonNode.class);
        if (created == null || !created.hasNonNull("userId")) {
            throw ApiException.unavailable("user_service_error", "El User Service no devolvió el perfil creado");
        }
        return created.get("userId").asText();
    }

    /** Compensación de la saga de registro. Nunca lanza excepción: solo registra el resultado. */
    public void deleteProfile(String userType, String userId) {
        String path = "TECNICO".equals(userType) ? "/users/technicians/{id}" : "/users/clients/{id}";
        try {
            client.delete().uri(path, userId).retrieve().toBodilessEntity();
            log.info("Compensación aplicada: perfil {} eliminado", userId);
        } catch (Exception ex) {
            log.error("Compensación fallida: el perfil {} quedó huérfano en user_db ({})", userId, ex.getMessage());
        }
    }
}
