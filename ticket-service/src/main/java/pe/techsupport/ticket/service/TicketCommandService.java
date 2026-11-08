package pe.techsupport.ticket.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import pe.techsupport.common.audit.AuditLogger;
import pe.techsupport.common.error.ApiException;
import pe.techsupport.common.security.AuthUser;
import pe.techsupport.common.security.SecurityUtils;
import pe.techsupport.ticket.client.UserServiceClient;
import pe.techsupport.ticket.domain.Priority;
import pe.techsupport.ticket.domain.Ticket;
import pe.techsupport.ticket.domain.TicketEvent;
import pe.techsupport.ticket.domain.TicketStatus;
import pe.techsupport.ticket.repository.TicketEventRepository;
import pe.techsupport.ticket.repository.TicketRepository;
import pe.techsupport.ticket.web.dto.TicketDtos.AssignRequest;
import pe.techsupport.ticket.web.dto.TicketDtos.ClientValidation;
import pe.techsupport.ticket.web.dto.TicketDtos.CloseRequest;
import pe.techsupport.ticket.web.dto.TicketDtos.CreateTicketRequest;
import pe.techsupport.ticket.web.dto.TicketDtos.EscalateRequest;
import pe.techsupport.ticket.web.dto.TicketDtos.PriorityRequest;
import pe.techsupport.ticket.web.dto.TicketDtos.ReassignRequest;
import pe.techsupport.ticket.web.dto.TicketDtos.ResolveRequest;
import pe.techsupport.ticket.web.dto.TicketDtos.TechnicianProfile;
import pe.techsupport.ticket.web.dto.TicketDtos.TicketResponse;

/**
 * Comandos del ciclo de vida del ticket. El Ticket Service actúa como ORQUESTADOR: coordina
 * explícitamente las llamadas al User Service (validar cliente, buscar y reservar técnicos, liberar
 * cupos) y decide según sus respuestas (sección 9 del documento).
 */
@Service
public class TicketCommandService {

    private final TicketRepository tickets;
    private final TicketEventRepository events;
    private final TicketQueryService queries;
    private final AssignmentService assignment;
    private final PriorityEvaluator evaluator;
    private final UserServiceClient users;
    private final Db db;
    private final TicketMetrics metrics;
    private final AuditLogger audit;

    public TicketCommandService(TicketRepository tickets, TicketEventRepository events, TicketQueryService queries,
                                AssignmentService assignment, PriorityEvaluator evaluator, UserServiceClient users,
                                Db db, TicketMetrics metrics, AuditLogger audit) {
        this.tickets = tickets;
        this.events = events;
        this.queries = queries;
        this.assignment = assignment;
        this.evaluator = evaluator;
        this.users = users;
        this.db = db;
        this.metrics = metrics;
        this.audit = audit;
    }

    /** Proceso 4.1: validar cliente → registrar → evaluar criticidad → (si es urgente) asignar de inmediato. */
    public TicketResponse create(CreateTicketRequest req) {
        AuthUser actor = SecurityUtils.currentUser();
        String clientId;
        if (actor.isClient()) {
            clientId = actor.userId();
        } else if (actor.isAdmin()) {
            if (req.userId() == null || req.userId().isBlank()) {
                throw ApiException.badRequest("validation_error", "userId: es obligatorio cuando registra un ADMIN");
            }
            clientId = req.userId();
        } else {
            throw ApiException.forbidden("Solo un CLIENTE o un ADMIN pueden registrar incidencias");
        }

        ClientValidation validation = users.validateClient(clientId);
        if (!validation.valid()) {
            audit.failure("TICKET_CREATE", clientId, "Cliente no habilitado: " + validation.status());
            throw ApiException.unprocessable("client_not_enabled",
                    "La cuenta del cliente " + clientId + " está " + validation.status()
                            + " y no puede registrar incidencias");
        }

        Priority priority = evaluator.evaluate(req.category(), req.title(), req.description());
        Ticket created = db.write(() -> {
            Ticket t = tickets.save(Ticket.register(clientId, validation.companyName(), req.title().trim(),
                    req.description().trim(), req.category(), priority, evaluator.requiredLevel(priority)));
            t.assignCode();
            events.save(new TicketEvent(t.getId(), "CREATED", null, TicketStatus.REGISTRADO, actor.userId(),
                    actor.role(), "Prioridad evaluada: " + priority + " (" + req.category() + ")"));
            return t;
        });
        String ticketId = created.getTicketId();
        metrics.created(priority, req.category());
        metrics.transition(TicketStatus.REGISTRADO);
        queries.invalidate(ticketId);
        audit.success("TICKET_CREATED", ticketId, priority + " / " + req.category() + " / cliente " + clientId);

        if (priority == Priority.CRITICA || priority == Priority.ALTA) {
            // Regla de alta prioridad: búsqueda y asignación inmediata del especialista
            assignment.dispatch(ticketId);
        }
        return queries.fresh(ticketId);
    }

    public TicketResponse updatePriority(String ticketId, PriorityRequest req) {
        AuthUser actor = SecurityUtils.currentUser();
        Ticket t = db.write(() -> {
            Ticket ticket = queries.load(ticketId);
            Priority before = ticket.getPriority();
            ticket.changePriority(req.priority());
            events.save(new TicketEvent(ticket.getId(), "PRIORITY_CHANGED", ticket.getStatus(), ticket.getStatus(),
                    actor.userId(), actor.role(), before + " -> " + req.priority()
                            + (req.justification() == null ? "" : ": " + req.justification())));
            return ticket;
        });
        queries.invalidate(ticketId);
        audit.success("TICKET_PRIORITY", ticketId, "Nueva prioridad " + req.priority());
        if (t.getStatus().isWaiting() && (req.priority() == Priority.CRITICA || req.priority() == Priority.ALTA)) {
            assignment.dispatch(ticketId);
        }
        return queries.fresh(ticketId);
    }

    public TicketResponse assign(String ticketId, AssignRequest req) {
        if (req != null && req.technicianId() != null && !req.technicianId().isBlank()) {
            assignment.assignManually(ticketId, req.technicianId(), "Asignación manual por ADMIN");
        } else {
            TicketStatus status = db.read(() -> queries.load(ticketId).getStatus());
            if (!status.isWaiting()) {
                throw new ApiException(HttpStatus.CONFLICT, "invalid_transition",
                        "El ticket ya tiene técnico (estado " + status + ")");
            }
            assignment.dispatch(ticketId);
        }
        return queries.fresh(ticketId);
    }

    public TicketResponse start(String ticketId) {
        return simpleTransition(ticketId, "STARTED", Ticket::startWork, "Diagnóstico iniciado");
    }

    /** Proceso 4.3: escalamiento jerárquico/funcional + búsqueda de nuevo especialista. */
    public TicketResponse escalate(String ticketId, EscalateRequest req) {
        AuthUser actor = SecurityUtils.currentUser();
        String[] released = new String[1];
        boolean[] functional = new boolean[1];
        db.writeVoid(() -> {
            Ticket t = queries.load(ticketId);
            requireAssignedOrAdmin(t, actor);
            TicketStatus from = t.getStatus();
            functional[0] = req.newCategory() != null && req.newCategory() != t.getCategory();
            released[0] = t.escalate(req.reason(), req.newCategory());
            events.save(new TicketEvent(t.getId(), "ESCALATED", from, TicketStatus.ESCALADO, actor.userId(),
                    actor.role(), req.reason() + " | requiere " + t.getCategory() + " " + t.getRequiredLevel()));
        });
        metrics.transition(TicketStatus.ESCALADO);
        metrics.escalated(functional[0] ? "FUNCIONAL" : "JERARQUICO");
        queries.invalidate(ticketId);
        users.release(released[0]);
        audit.success("TICKET_ESCALATED", ticketId, req.reason());

        if (req.newTechId() != null && !req.newTechId().isBlank()) {
            assignment.assignManually(ticketId, req.newTechId(), "Escalado a técnico indicado");
        } else {
            assignment.dispatch(ticketId);
        }
        return queries.fresh(ticketId);
    }

    /** Reasignación directa a un técnico concreto, preservando la bitácora. */
    public TicketResponse reassign(String ticketId, ReassignRequest req) {
        AuthUser actor = SecurityUtils.currentUser();
        Ticket current = db.read(() -> queries.load(ticketId));
        requireAssignedOrAdmin(current, actor);
        if (req.newTechnicianId().equals(current.getAssignedTechId())) {
            throw ApiException.unprocessable("same_technician", "El ticket ya está asignado a ese técnico");
        }
        TechnicianProfile profile = users.getTechnician(req.newTechnicianId());
        if (!profile.specialties().contains(current.getCategory().name())
                || !profile.supportLevel().covers(current.getRequiredLevel())) {
            throw ApiException.unprocessable("technician_not_eligible",
                    "El técnico " + req.newTechnicianId() + " no cumple la especialidad/nivel requeridos");
        }
        if (!users.reserve(req.newTechnicianId())) {
            throw ApiException.conflict("technician_unavailable",
                    "El técnico " + req.newTechnicianId() + " no está disponible o alcanzó su límite");
        }
        String reason = req.reason() == null || req.reason().isBlank() ? "Reasignación" : req.reason();
        String released;
        try {
            released = db.write(() -> {
                Ticket t = queries.load(ticketId);
                TicketStatus from = t.getStatus();
                String previous = t.reassign(reason, profile.userId(), profile.fullName(), profile.supportLevel());
                events.save(new TicketEvent(t.getId(), "REASSIGNED", from, TicketStatus.REASIGNADO, actor.userId(),
                        actor.role(), reason + ": " + previous + " -> " + profile.userId()));
                return previous;
            });
        } catch (RuntimeException ex) {
            users.release(req.newTechnicianId());
            throw ex;
        }
        users.release(released);
        metrics.transition(TicketStatus.REASIGNADO);
        metrics.escalated("REASIGNACION");
        queries.invalidate(ticketId);
        audit.success("TICKET_REASSIGNED", ticketId, released + " -> " + profile.userId());
        return queries.fresh(ticketId);
    }

    public TicketResponse resolve(String ticketId, ResolveRequest req) {
        AuthUser actor = SecurityUtils.currentUser();
        String techId = db.write(() -> {
            Ticket t = queries.load(ticketId);
            requireAssignedOrAdmin(t, actor);
            TicketStatus from = t.getStatus();
            t.resolve(req.resolutionLog(), req.resolutionCode());
            events.save(new TicketEvent(t.getId(), "RESOLVED", from, TicketStatus.RESUELTO, actor.userId(),
                    actor.role(), req.resolutionCode()));
            return t.getAssignedTechId();
        });
        users.release(techId);
        metrics.transition(TicketStatus.RESUELTO);
        queries.invalidate(ticketId);
        audit.success("TICKET_RESOLVED", ticketId, "Código " + req.resolutionCode());
        return queries.fresh(ticketId);
    }

    public TicketResponse close(String ticketId, CloseRequest req) {
        AuthUser actor = SecurityUtils.currentUser();
        db.writeVoid(() -> {
            Ticket t = queries.load(ticketId);
            if (actor.isClient() && !actor.userId().equals(t.getClientId())) {
                throw ApiException.forbidden("Solo el cliente dueño del ticket puede confirmar el cierre");
            }
            t.close();
            events.save(new TicketEvent(t.getId(), "CLOSED", TicketStatus.RESUELTO, TicketStatus.CERRADO,
                    actor.userId(), actor.role(), req == null ? null : req.comment()));
        });
        metrics.transition(TicketStatus.CERRADO);
        queries.invalidate(ticketId);
        audit.success("TICKET_CLOSED", ticketId, "Cierre confirmado");
        return queries.fresh(ticketId);
    }

    private TicketResponse simpleTransition(String ticketId, String eventType,
                                            java.util.function.Consumer<Ticket> action, String detail) {
        AuthUser actor = SecurityUtils.currentUser();
        TicketStatus to = db.write(() -> {
            Ticket t = queries.load(ticketId);
            requireAssignedOrAdmin(t, actor);
            TicketStatus from = t.getStatus();
            action.accept(t);
            events.save(new TicketEvent(t.getId(), eventType, from, t.getStatus(), actor.userId(), actor.role(),
                    detail));
            return t.getStatus();
        });
        metrics.transition(to);
        queries.invalidate(ticketId);
        audit.success("TICKET_" + eventType, ticketId, detail);
        return queries.fresh(ticketId);
    }

    private static void requireAssignedOrAdmin(Ticket t, AuthUser actor) {
        if (actor.isAdmin()) {
            return;
        }
        if (actor.isTechnician() && t.isAssignedTo(actor.userId())) {
            return;
        }
        throw ApiException.forbidden("Solo el técnico asignado o un ADMIN pueden operar el ticket "
                + t.getTicketId());
    }
}
