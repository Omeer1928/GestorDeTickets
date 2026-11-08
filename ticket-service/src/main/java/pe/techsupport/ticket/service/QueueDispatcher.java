package pe.techsupport.ticket.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import pe.techsupport.common.resilience.InfraFailures;
import pe.techsupport.ticket.domain.QueueName;
import pe.techsupport.ticket.domain.Ticket;
import pe.techsupport.ticket.repository.TicketRepository;

/**
 * Reintento periódico de la cola de espera ("el ticket permanece en cola hasta la liberación de
 * recursos para un nuevo reintento de búsqueda"). Procesa primero la cola prioritaria y, dentro de
 * cada prioridad, el ticket más antiguo. Nota: con varias réplicas se debería usar un lock
 * distribuido (p. ej. ShedLock sobre Redis) para que solo una instancia despache.
 */
@Component
public class QueueDispatcher {

    private static final Logger log = LoggerFactory.getLogger(QueueDispatcher.class);

    private final TicketRepository tickets;
    private final AssignmentService assignment;
    private final TicketMetrics metrics;
    private final Db db;

    public QueueDispatcher(TicketRepository tickets, AssignmentService assignment, TicketMetrics metrics, Db db) {
        this.tickets = tickets;
        this.assignment = assignment;
        this.metrics = metrics;
        this.db = db;
    }

    @Scheduled(initialDelayString = "${app.tickets.dispatch-interval-ms:20000}",
            fixedDelayString = "${app.tickets.dispatch-interval-ms:20000}")
    public void dispatchWaitingTickets() {
        List<String> waiting;
        try {
            waiting = db.read(() -> {
                metrics.updateWaiting(tickets.countByQueueNameAndAssignedTechIdIsNull(QueueName.PRIORITY_QUEUE),
                        tickets.countByQueueNameAndAssignedTechIdIsNull(QueueName.STANDARD_QUEUE));
                return tickets.findWaiting(TicketMapper.WAITING, PageRequest.of(0, 50)).stream()
                        .map(Ticket::getTicketId).toList();
            });
        } catch (RuntimeException ex) {
            if (InfraFailures.isInfrastructureFailure(ex)) {
                log.warn("Despachador en pausa: BD de tickets no disponible ({})", InfraFailures.rootMessage(ex));
                return;
            }
            throw ex;
        }
        int assigned = 0;
        for (String ticketId : waiting) {
            try {
                if (assignment.dispatch(ticketId)) {
                    assigned++;
                }
            } catch (RuntimeException ex) {
                log.warn("Error despachando {}: {}", ticketId, ex.getMessage());
            }
        }
        if (!waiting.isEmpty()) {
            log.info("Despachador: {} de {} tickets en espera asignados", assigned, waiting.size());
        }
    }
}
