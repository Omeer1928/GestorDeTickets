package pe.techsupport.ticket.service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import org.springframework.stereotype.Component;

import pe.techsupport.ticket.domain.Priority;
import pe.techsupport.ticket.domain.Specialty;
import pe.techsupport.ticket.domain.TicketStatus;

/** Métricas de negocio para los dashboards de auditoría (MTTA, volumen por prioridad, escalamientos). */
@Component
public class TicketMetrics {

    private final MeterRegistry meters;
    private final AtomicLong waitingPriority = new AtomicLong();
    private final AtomicLong waitingStandard = new AtomicLong();

    public TicketMetrics(MeterRegistry meters) {
        this.meters = meters;
        Gauge.builder("tickets.waiting", waitingPriority, AtomicLong::get)
                .description("Tickets en espera de técnico").tag("queue", "PRIORITY_QUEUE").register(meters);
        Gauge.builder("tickets.waiting", waitingStandard, AtomicLong::get)
                .description("Tickets en espera de técnico").tag("queue", "STANDARD_QUEUE").register(meters);
        preRegisterCounters();
    }

    /**
     * Crea todos los contadores en 0 al arrancar. Si un contador nace recién con el primer evento,
     * Prometheus lo ve por primera vez valiendo 1 y increase() calcula 1 → 1 = 0, así que el primer
     * ticket no se contaría en los dashboards.
     */
    private void preRegisterCounters() {
        for (Priority p : Priority.values()) {
            for (Specialty c : Specialty.values()) {
                meters.counter("tickets.registered", "priority", p.name(), "category", c.name());
            }
        }
        for (TicketStatus st : TicketStatus.values()) {
            meters.counter("tickets.transitions", "status", st.name());
        }
        for (String type : new String[]{"JERARQUICO", "FUNCIONAL", "REASIGNACION"}) {
            meters.counter("tickets.escalations", "type", type);
        }
        for (String result : new String[]{"assigned", "no_candidates", "dependency_unavailable", "conflict", "error"}) {
            meters.counter("tickets.dispatch", "result", result);
        }
    }

    /**
     * Nota: el nombre NO puede terminar en ".created": la librería de Prometheus reserva el sufijo
     * "_created" y lo elimina (se publicaría como tickets_total). Por eso se usa "tickets.registered".
     */
    public void created(Priority priority, Specialty category) {
        meters.counter("tickets.registered", "priority", priority.name(), "category", category.name()).increment();
    }

    public void transition(TicketStatus to) {
        meters.counter("tickets.transitions", "status", to.name()).increment();
    }

    public void escalated(String type) {
        meters.counter("tickets.escalations", "type", type).increment();
    }

    /** Tiempo desde el registro hasta la asignación del técnico (Mean Time To Assign). */
    public void assigned(Priority priority, Duration sinceRegistration) {
        Timer.builder("tickets.assignment")
                .description("Tiempo desde el registro hasta la asignación del técnico")
                .tag("priority", priority.name())
                .register(meters)
                .record(sinceRegistration);
    }

    public void dispatch(String result) {
        meters.counter("tickets.dispatch", "result", result).increment();
    }

    public void updateWaiting(long priority, long standard) {
        waitingPriority.set(priority);
        waitingStandard.set(standard);
    }
}
