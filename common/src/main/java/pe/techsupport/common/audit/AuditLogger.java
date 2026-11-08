package pe.techsupport.common.audit;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import pe.techsupport.common.security.AuthUser;
import pe.techsupport.common.security.SecurityUtils;

/**
 * Pista de auditoría: cada acción relevante se registra como un log JSON con el logger "AUDIT"
 * (Loki la indexa y Grafana la muestra) y además incrementa la métrica audit_events_total
 * (Prometheus) para tener conteos por evento y resultado.
 */
@Component
public class AuditLogger {

    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");

    private final MeterRegistry meters;

    public AuditLogger(MeterRegistry meters) {
        this.meters = meters;
    }

    public void success(String event, String resource, String detail) {
        log(event, resource, "SUCCESS", detail, null);
    }

    public void failure(String event, String resource, String detail) {
        log(event, resource, "FAILURE", detail, null);
    }

    /** Variante para eventos sin usuario autenticado todavía (p. ej. login). */
    public void log(String event, String resource, String outcome, String detail, AuthUser actorOverride) {
        AuthUser actor = actorOverride != null ? actorOverride : SecurityUtils.currentUserOptional().orElse(null);
        String actorId = actor != null ? actor.userId() : "anonymous";
        String role = actor != null ? actor.role() : "NONE";
        try {
            MDC.put("audit_event", event);
            MDC.put("audit_actor", actorId);
            MDC.put("audit_role", role);
            MDC.put("audit_resource", resource == null ? "-" : resource);
            MDC.put("audit_outcome", outcome);
            AUDIT.info("{} {} por {} ({}) sobre {}: {}", event, outcome, actorId, role, resource, detail);
        } finally {
            MDC.remove("audit_event");
            MDC.remove("audit_actor");
            MDC.remove("audit_role");
            MDC.remove("audit_resource");
            MDC.remove("audit_outcome");
        }
        Counter.builder("audit.events")
                .description("Eventos de auditoría de negocio")
                .tag("event", event)
                .tag("outcome", outcome)
                .tag("role", role)
                .register(meters)
                .increment();
    }
}
