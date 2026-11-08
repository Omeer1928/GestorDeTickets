package pe.techsupport.ticket.service;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import pe.techsupport.common.error.ApiException;
import pe.techsupport.common.resilience.ResilientCache;
import pe.techsupport.common.security.AuthUser;
import pe.techsupport.common.security.SecurityUtils;
import pe.techsupport.ticket.config.TicketProperties;
import pe.techsupport.ticket.domain.Ticket;
import pe.techsupport.ticket.domain.TicketStatus;
import pe.techsupport.ticket.repository.TicketEventRepository;
import pe.techsupport.ticket.repository.TicketRepository;
import pe.techsupport.ticket.web.dto.TicketDtos.TicketEventResponse;
import pe.techsupport.ticket.web.dto.TicketDtos.TicketResponse;
import pe.techsupport.ticket.web.dto.TicketDtos.TicketSummary;

/**
 * Consultas (lado de lectura). Todas pasan por la caché resiliente: con la BD de tickets caída se
 * responde con la última copia conocida y la cabecera X-Cache: STALE.
 */
@Service
public class TicketQueryService {

    static final String CACHE_TICKET = "ticket";
    static final String CACHE_EVENTS = "ticket-events";
    static final String[] LIST_CACHES = {"tickets-client", "tickets-tech", "tickets-status", "tickets-all",
            "tickets-queue"};

    private final TicketRepository tickets;
    private final TicketEventRepository events;
    private final TicketMapper mapper;
    private final ResilientCache cache;
    private final Db db;
    private final TicketProperties props;

    public TicketQueryService(TicketRepository tickets, TicketEventRepository events, TicketMapper mapper,
                              ResilientCache cache, Db db, TicketProperties props) {
        this.tickets = tickets;
        this.events = events;
        this.mapper = mapper;
        this.cache = cache;
        this.db = db;
        this.props = props;
    }

    public TicketResponse get(String ticketId) {
        TicketResponse view = cache.get(CACHE_TICKET, ticketId, props.getViewCacheTtl(), TicketResponse.class,
                () -> db.read(() -> mapper.toResponse(load(ticketId))));
        TicketAccess.requireCanView(view);
        return view;
    }

    /** Lectura directa sin caché (usada justo después de una escritura). */
    public TicketResponse fresh(String ticketId) {
        TicketResponse view = db.read(() -> mapper.toResponse(load(ticketId)));
        cache.put(CACHE_TICKET, ticketId, view, props.getViewCacheTtl());
        return view;
    }

    public List<TicketEventResponse> events(String ticketId) {
        get(ticketId); // valida existencia y permisos
        return cache.get(CACHE_EVENTS, ticketId, props.getViewCacheTtl(),
                new TypeReference<List<TicketEventResponse>>() { },
                () -> db.read(() -> events.findByTicketPkOrderByCreatedAtAsc(load(ticketId).getId()).stream()
                        .map(TicketMapper::toEvent).toList()));
    }

    public List<TicketSummary> byClient(String clientId) {
        TicketAccess.requireSelf(clientId);
        return list("tickets-client", clientId,
                () -> tickets.findByClientIdOrderByRegisteredAtDesc(clientId));
    }

    public List<TicketSummary> byTechnician(String techId) {
        TicketAccess.requireSelf(techId);
        return list("tickets-tech", techId,
                () -> tickets.findByAssignedTechIdOrderByRegisteredAtDesc(techId));
    }

    public List<TicketSummary> byStatus(TicketStatus status) {
        List<TicketSummary> all = list("tickets-status", status.name(),
                () -> tickets.findByStatusOrderByPriorityRankDescRegisteredAtAsc(status));
        AuthUser u = SecurityUtils.currentUser();
        if (u.isAdmin() || u.isSystem()) {
            return all;
        }
        // CLIENTE ve solo los suyos; TECNICO solo los que tiene asignados
        return all.stream()
                .filter(t -> u.isClient() ? u.userId().equals(t.userId()) : u.userId().equals(t.assignedTechnicianId()))
                .toList();
    }

    public List<TicketSummary> latest() {
        return list("tickets-all", "latest", () -> tickets.findAllByOrderByRegisteredAtDesc(PageRequest.of(0, 200)));
    }

    public List<TicketSummary> queue() {
        return list("tickets-queue", "all",
                () -> tickets.findWaiting(TicketMapper.WAITING, PageRequest.of(0, 200)));
    }

    public void invalidate(String ticketId) {
        cache.evict(CACHE_TICKET, ticketId);
        cache.evict(CACHE_EVENTS, ticketId);
        for (String c : LIST_CACHES) {
            cache.evictAll(c);
        }
    }

    private List<TicketSummary> list(String cacheName, String key, java.util.function.Supplier<List<Ticket>> query) {
        return cache.get(cacheName, key, props.getListCacheTtl(), new TypeReference<List<TicketSummary>>() { },
                () -> db.read(() -> query.get().stream().map(TicketMapper::toSummary).toList()));
    }

    Ticket load(String ticketId) {
        return tickets.findByTicketId(ticketId)
                .orElseThrow(() -> ApiException.notFound("ticket_not_found", "No existe el ticket " + ticketId));
    }
}
