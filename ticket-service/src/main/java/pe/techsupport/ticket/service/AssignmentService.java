package pe.techsupport.ticket.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import pe.techsupport.common.audit.AuditLogger;
import pe.techsupport.common.error.ApiException;
import pe.techsupport.common.resilience.InfraFailures;
import pe.techsupport.common.security.AuthUser;
import pe.techsupport.common.security.SecurityUtils;
import pe.techsupport.ticket.client.UserServiceClient;
import pe.techsupport.ticket.domain.Specialty;
import pe.techsupport.ticket.domain.SupportLevel;
import pe.techsupport.ticket.domain.Ticket;
import pe.techsupport.ticket.domain.TicketEvent;
import pe.techsupport.ticket.domain.TicketStatus;
import pe.techsupport.ticket.repository.TicketEventRepository;
import pe.techsupport.ticket.web.dto.TicketDtos.TechnicianCandidate;
import pe.techsupport.ticket.web.dto.TicketDtos.TechnicianProfile;

/**
 * Orquestación del proceso "Asignación de técnicos" (4.2): consulta al User Service los candidatos
 * por especialidad y nivel, reserva un cupo de forma atómica y vincula el técnico al ticket.
 * Si la vinculación falla, compensa liberando el cupo reservado.
 */
@Service
public class AssignmentService {

    private static final Logger log = LoggerFactory.getLogger(AssignmentService.class);

    private final UserServiceClient users;
    private final Db db;
    private final TicketQueryService queries;
    private final TicketEventRepository events;
    private final TicketMetrics metrics;
    private final AuditLogger audit;

    public AssignmentService(UserServiceClient users, Db db, TicketQueryService queries, TicketEventRepository events,
                             TicketMetrics metrics, AuditLogger audit) {
        this.users = users;
        this.db = db;
        this.queries = queries;
        this.events = events;
        this.metrics = metrics;
        this.audit = audit;
    }

    record WaitingTicket(String ticketId, Specialty category, SupportLevel level, Set<String> excluded) {
    }

    /**
     * Intenta asignar automáticamente un ticket en espera (REGISTRADO o ESCALADO).
     * @return true si quedó asignado; false si no hay técnicos o una dependencia no responde (sigue en cola)
     */
    public boolean dispatch(String ticketId) {
        WaitingTicket waiting;
        try {
            waiting = db.read(() -> {
                Ticket t = queries.load(ticketId);
                if (!t.getStatus().isWaiting() || t.getAssignedTechId() != null) {
                    return null;
                }
                return new WaitingTicket(t.getTicketId(), t.getCategory(), t.getRequiredLevel(),
                        t.lastEscalatedFrom().map(Set::of).orElse(Set.of()));
            });
        } catch (RuntimeException ex) {
            return handleDispatchError(ticketId, ex);
        }
        if (waiting == null) {
            return false;
        }
        Optional<TechnicianCandidate> reserved;
        try {
            reserved = findAndReserve(waiting.category(), waiting.level(), waiting.excluded());
        } catch (RuntimeException ex) {
            return handleDispatchError(ticketId, ex);
        }
        if (reserved.isEmpty()) {
            metrics.dispatch("no_candidates");
            log.info("Sin técnicos disponibles para {} ({} {}): queda en cola", ticketId, waiting.category(),
                    waiting.level());
            return false;
        }
        TechnicianCandidate tech = reserved.get();
        try {
            bind(ticketId, tech.userId(), tech.fullName(), tech.supportLevel(), "Asignación automática");
            metrics.dispatch("assigned");
            return true;
        } catch (RuntimeException ex) {
            users.release(tech.userId());
            return handleDispatchError(ticketId, ex);
        }
    }

    /** Asignación manual a un técnico concreto (ADMIN, o técnico al reasignar/escalar a un colega). */
    public void assignManually(String ticketId, String techId, String reason) {
        TechnicianProfile profile = users.getTechnician(techId);
        Ticket snapshot = db.read(() -> queries.load(ticketId));
        if (!profile.specialties().contains(snapshot.getCategory().name())
                || !profile.supportLevel().covers(snapshot.getRequiredLevel())) {
            throw ApiException.unprocessable("technician_not_eligible",
                    "El técnico " + techId + " no tiene la especialidad " + snapshot.getCategory()
                            + " o el nivel " + snapshot.getRequiredLevel() + " requerido");
        }
        if (!users.reserve(techId)) {
            throw ApiException.conflict("technician_unavailable",
                    "El técnico " + techId + " no está disponible o alcanzó su límite de tickets");
        }
        try {
            bind(ticketId, techId, profile.fullName(), profile.supportLevel(), reason);
        } catch (RuntimeException ex) {
            users.release(techId);
            throw ex;
        }
    }

    private Optional<TechnicianCandidate> findAndReserve(Specialty category, SupportLevel level, Set<String> excluded) {
        List<TechnicianCandidate> candidates = users.findAvailable(category, level);
        for (TechnicianCandidate c : candidates) {
            if (excluded.contains(c.userId())) {
                continue;
            }
            if (users.reserve(c.userId())) {
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }

    private void bind(String ticketId, String techId, String techName, SupportLevel techLevel, String reason) {
        AuthUser actor = SecurityUtils.currentUserOptional()
                .orElse(new AuthUser("system:dispatcher", null, "SYSTEM"));
        Ticket saved = db.write(() -> {
            Ticket t = queries.load(ticketId);
            if (!t.getStatus().isWaiting() || t.getAssignedTechId() != null) {
                throw ApiException.conflict("already_assigned",
                        "El ticket " + ticketId + " ya no está en espera (estado " + t.getStatus() + ")");
            }
            TicketStatus from = t.getStatus();
            TicketStatus to = t.assignTechnician(techId, techName, techLevel);
            events.save(new TicketEvent(t.getId(), to == TicketStatus.ASIGNADO ? "ASSIGNED" : "REASSIGNED", from, to,
                    actor.userId(), actor.role(), reason + ": " + techId));
            return t;
        });
        metrics.transition(saved.getStatus());
        metrics.assigned(saved.getPriority(), Duration.between(saved.getRegisteredAt(), Instant.now()));
        queries.invalidate(ticketId);
        audit.log("TICKET_ASSIGNED", ticketId, "SUCCESS",
                saved.getStatus() + " a " + techId + " (" + saved.getPriority() + ", " + saved.getCategory() + ")", actor);
    }

    private boolean handleDispatchError(String ticketId, RuntimeException ex) {
        if (InfraFailures.isInfrastructureFailure(ex)) {
            metrics.dispatch("dependency_unavailable");
            log.warn("No se pudo despachar {} (dependencia caída): {}. Queda en cola para reintento.", ticketId,
                    InfraFailures.rootMessage(ex));
            return false;
        }
        if (ex instanceof ApiException api && api.getStatus() == org.springframework.http.HttpStatus.CONFLICT
                || ex instanceof org.springframework.dao.OptimisticLockingFailureException) {
            metrics.dispatch("conflict");
            log.info("Despacho de {} descartado: el ticket cambió concurrentemente", ticketId);
            return false;
        }
        metrics.dispatch("error");
        if (ex instanceof ApiException) {
            log.warn("Despacho de {} no completado: {}", ticketId, ex.getMessage());
            return false; // el ticket queda en cola; el despachador lo reintentará
        }
        throw ex;
    }
}
