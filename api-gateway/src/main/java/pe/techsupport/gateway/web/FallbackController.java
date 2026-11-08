package pe.techsupport.gateway.web;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import io.micrometer.core.instrument.MeterRegistry;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Respuesta degradada cuando un servicio no responde (caído, timeout o circuito abierto):
 * el cliente recibe un 503 claro y rápido en lugar de un error de conexión.
 */
@RestController
public class FallbackController {

    private final MeterRegistry meters;

    public FallbackController(MeterRegistry meters) {
        this.meters = meters;
    }

    @RequestMapping("/fallback/{service}")
    public Mono<ResponseEntity<Map<String, Object>>> fallback(@PathVariable String service) {
        meters.counter("gateway.fallbacks", "service", service).increment();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "service_unavailable");
        body.put("message", "El servicio " + service + " no está disponible en este momento. Reintente en unos segundos.");
        body.put("status", 503);
        body.put("service", service);
        body.put("timestamp", Instant.now().toString());
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(HttpHeaders.RETRY_AFTER, "10")
                .body(body));
    }
}
