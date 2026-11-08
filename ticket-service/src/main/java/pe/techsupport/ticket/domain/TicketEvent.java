package pe.techsupport.ticket.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Evento inmutable de la bitácora del ticket (no tiene setters). */
@Entity
@Table(name = "ticket_event")
public class TicketEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_pk", nullable = false)
    private Long ticketPk;

    @Column(name = "event_type", nullable = false, length = 40)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 15)
    private TicketStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", length = 15)
    private TicketStatus toStatus;

    @Column(name = "actor_id", nullable = false, length = 40)
    private String actorId;

    @Column(name = "actor_role", nullable = false, length = 20)
    private String actorRole;

    @Column(length = 500)
    private String detail;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TicketEvent() {
    }

    public TicketEvent(Long ticketPk, String eventType, TicketStatus fromStatus, TicketStatus toStatus,
                       String actorId, String actorRole, String detail) {
        this.ticketPk = ticketPk;
        this.eventType = eventType;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.actorId = actorId;
        this.actorRole = actorRole;
        this.detail = detail == null ? null : detail.substring(0, Math.min(detail.length(), 500));
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getTicketPk() { return ticketPk; }
    public String getEventType() { return eventType; }
    public TicketStatus getFromStatus() { return fromStatus; }
    public TicketStatus getToStatus() { return toStatus; }
    public String getActorId() { return actorId; }
    public String getActorRole() { return actorRole; }
    public String getDetail() { return detail; }
    public Instant getCreatedAt() { return createdAt; }
}
