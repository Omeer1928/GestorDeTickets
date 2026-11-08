package pe.techsupport.ticket.client;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.MeterRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import pe.techsupport.common.audit.AuditLogger;
import pe.techsupport.common.error.ApiError;
import pe.techsupport.common.error.ApiException;
import pe.techsupport.common.http.InternalRestClientFactory;
import pe.techsupport.common.resilience.InfraFailures;
import pe.techsupport.common.resilience.ResilientCache;
import pe.techsupport.ticket.domain.Specialty;
import pe.techsupport.ticket.domain.SupportLevel;
import pe.techsupport.ticket.web.dto.TicketDtos.AvailableTechnicians;
import pe.techsupport.ticket.web.dto.TicketDtos.ClientValidation;
import pe.techsupport.ticket.web.dto.TicketDtos.TechnicianCandidate;
import pe.techsupport.ticket.web.dto.TicketDtos.TechnicianProfile;

/**
 * Integración síncrona REST con el User Service (orquestación desde el Ticket Service).
 * Lecturas: caché propia con respaldo stale → si el User Service o su BD caen, el Ticket Service
 * sigue validando clientes y viendo candidatos con el último dato conocido.
 * Escrituras (reservar/liberar cupo): circuit breaker; la liberación además reintenta (es idempotente
 * a nivel de negocio porque nunca baja de 0), la reserva NO se reintenta para no duplicar cupos.
 */
@Component
public class UserServiceClient {

    private static final Logger log = LoggerFactory.getLogger(UserServiceClient.class);
    private static final String CIRCUIT = "userService";

    private final RestClient client;
    private final ObjectMapper mapper;
    private final ResilientCache cache;
    private final CircuitBreaker circuit;
    private final Retry retry;
    private final AuditLogger audit;
    private final MeterRegistry meters;

    public UserServiceClient(InternalRestClientFactory factory, ObjectMapper mapper, ResilientCache cache,
                             CircuitBreakerRegistry circuits, RetryRegistry retries, AuditLogger audit,
                             MeterRegistry meters, @Value("${app.services.user-service-url}") String baseUrl) {
        this.client = factory.create(baseUrl, Duration.ofSeconds(2), Duration.ofSeconds(4));
        this.mapper = mapper;
        this.cache = cache;
        this.circuit = circuits.circuitBreaker(CIRCUIT);
        this.retry = retries.retry(CIRCUIT);
        this.audit = audit;
        this.meters = meters;
    }

    public ClientValidation validateClient(String clientId) {
        return cache.get("client-valid", clientId, Duration.ofSeconds(30), ClientValidation.class, CIRCUIT,
                () -> client.get().uri("/users/clients/{id}/validate", clientId)
                        .retrieve()
                        .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                            throw toApiException(res.getStatusCode(), mapper.readValue(res.getBody(), ApiError.class));
                        })
                        .body(ClientValidation.class));
    }

    public List<TechnicianCandidate> findAvailable(Specialty specialty, SupportLevel minLevel) {
        AvailableTechnicians result = cache.get("tech-available", specialty + ":" + minLevel, Duration.ofSeconds(10),
                new TypeReference<AvailableTechnicians>() { }, CIRCUIT,
                () -> client.get()
                        .uri(b -> b.path("/users/technicians/available")
                                .queryParam("specialty", specialty.name())
                                .queryParam("level", minLevel.name())
                                .build())
                        .retrieve()
                        .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                            throw toApiException(res.getStatusCode(), mapper.readValue(res.getBody(), ApiError.class));
                        })
                        .body(AvailableTechnicians.class));
        return result == null || result.technicians() == null ? List.of() : result.technicians();
    }

    public TechnicianProfile getTechnician(String techId) {
        return cache.get("tech", techId, Duration.ofMinutes(2), TechnicianProfile.class, CIRCUIT,
                () -> client.get().uri("/users/technicians/{id}", techId)
                        .retrieve()
                        .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                            throw toApiException(res.getStatusCode(), mapper.readValue(res.getBody(), ApiError.class));
                        })
                        .body(TechnicianProfile.class));
    }

    /**
     * Reserva atómica de un cupo del técnico en el User Service.
     * @return false si el técnico ya no está disponible (409) o no existe (404)
     */
    public boolean reserve(String techId) {
        try {
            circuit.executeRunnable(() -> adjust(techId, 1));
            cache.evictAll("tech-available");
            cache.evict("tech", techId);
            return true;
        } catch (ApiException ex) {
            if (ex.getStatus() == HttpStatus.CONFLICT || ex.getStatus() == HttpStatus.NOT_FOUND) {
                return false;
            }
            throw ex;
        } catch (RuntimeException ex) {
            if (InfraFailures.isInfrastructureFailure(ex)) {
                // Resultado incierto (p. ej. timeout tras aplicar el +1): se compensa liberando el cupo.
                // releaseSlot nunca baja de 0, así que es seguro aunque la reserva no se haya aplicado.
                release(techId);
            }
            throw ex;
        }
    }

    /** Libera el cupo del técnico. Nunca propaga errores: si falla, queda auditado para conciliación. */
    public void release(String techId) {
        if (techId == null) {
            return;
        }
        try {
            retry.executeRunnable(() -> circuit.executeRunnable(() -> adjust(techId, -1)));
            cache.evictAll("tech-available");
            cache.evict("tech", techId);
        } catch (RuntimeException ex) {
            meters.counter("tickets.workload.release.failures").increment();
            audit.failure("WORKLOAD_RELEASE", techId, "No se pudo liberar el cupo del técnico: " + ex.getMessage());
            log.error("No se pudo liberar el cupo de {}: {}", techId, ex.getMessage());
        }
    }

    private void adjust(String techId, int delta) {
        client.patch().uri("/users/technicians/{id}/workload", techId)
                .body(Map.of("delta", delta))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw toApiException(res.getStatusCode(), mapper.readValue(res.getBody(), ApiError.class));
                })
                .toBodilessEntity();
    }

    private static ApiException toApiException(HttpStatusCode status, ApiError err) {
        return new ApiException(HttpStatus.valueOf(status.value()), err.error(), err.message());
    }
}
