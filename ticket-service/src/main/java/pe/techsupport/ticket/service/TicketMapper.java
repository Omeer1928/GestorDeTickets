package pe.techsupport.ticket.service;

import java.util.EnumSet;
import java.util.List;

import org.springframework.stereotype.Component;

import pe.techsupport.ticket.config.TicketProperties;
import pe.techsupport.ticket.domain.Ticket;
import pe.techsupport.ticket.domain.TicketEvent;
import pe.techsupport.ticket.domain.TicketStatus;
import pe.techsupport.ticket.repository.TicketRepository;
import pe.techsupport.ticket.web.dto.TicketDtos.AssignedTechnician;
import pe.techsupport.ticket.web.dto.TicketDtos.EscalationEntry;
import pe.techsupport.ticket.web.dto.TicketDtos.IncidentData;
import pe.techsupport.ticket.web.dto.TicketDtos.QueueInfo;
import pe.techsupport.ticket.web.dto.TicketDtos.TicketEventResponse;
import pe.techsupport.ticket.web.dto.TicketDtos.TicketResponse;
import pe.techsupport.ticket.web.dto.TicketDtos.TicketSummary;

/** Traduce el agregado a los contratos JSON. Debe invocarse dentro de una transacción. */
@Component
public class TicketMapper {

    static final EnumSet<TicketStatus> WAITING = EnumSet.of(TicketStatus.REGISTRADO, TicketStatus.ESCALADO);

    private final TicketRepository tickets;
    private final TicketProperties props;

    public TicketMapper(TicketRepository tickets, TicketProperties props) {
        this.tickets = tickets;
        this.props = props;
    }

    public TicketResponse toResponse(Ticket t) {
        AssignedTechnician tech = t.getAssignedTechId() == null ? null
                : new AssignedTechnician(t.getAssignedTechId(), t.getAssignedTechName(), t.getAssignedTechLevel());
        QueueInfo queue = null;
        if (t.getStatus().isWaiting() && t.getAssignedTechId() == null && t.getQueueName() != null) {
            long position = tickets.countAhead(WAITING, t.getId(), t.getPriorityRank(), t.getRegisteredAt()) + 1;
            queue = new QueueInfo(position, (position * props.getAvgAttentionMinutes()) + " minutos",
                    t.getQueueName().name());
        }
        List<EscalationEntry> history = t.getEscalations().stream()
                .map(e -> new EscalationEntry(e.getType().name(), e.getPreviousTechId(), e.getNewTechId(),
                        e.getReason(), e.getPreviousCategory(), e.getNewCategory(), e.getPreviousLevel(),
                        e.getNewLevel(), e.getTransferDate()))
                .toList();
        return new TicketResponse(t.getTicketId(), t.getClientId(), t.getCompanyName(),
                new IncidentData(t.getTitle(), t.getDescription(), t.getCategory(), t.getRegisteredAt(),
                        t.getUpdatedAt()),
                t.getPriority(), t.getStatus(), t.getRequiredLevel(), tech, queue, history, t.getResolutionLog(),
                t.getResolutionCode(), t.getAssignedAt(), t.getResolvedAt(), t.getClosedAt());
    }

    public static TicketSummary toSummary(Ticket t) {
        return new TicketSummary(t.getTicketId(), t.getClientId(), t.getTitle(), t.getCategory(), t.getPriority(),
                t.getStatus(), t.getAssignedTechId(), t.getQueueName() == null ? null : t.getQueueName().name(),
                t.getRegisteredAt(), t.getUpdatedAt());
    }

    public static TicketEventResponse toEvent(TicketEvent e) {
        return new TicketEventResponse(e.getEventType(), e.getFromStatus(), e.getToStatus(), e.getActorId(),
                e.getActorRole(), e.getDetail(), e.getCreatedAt());
    }
}
