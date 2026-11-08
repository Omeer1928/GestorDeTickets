package pe.techsupport.ticket.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.springframework.http.HttpStatus;

import pe.techsupport.common.error.ApiException;

/**
 * Agregado Ticket: concentra las reglas del ciclo de vida (transiciones válidas, asignación,
 * escalamiento con historial, resolución y cierre).
 */
@Entity
@Table(name = "ticket")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_id", unique = true, length = 20)
    private String ticketId;

    @Column(name = "client_id", nullable = false, length = 20)
    private String clientId;

    @Column(name = "company_name", length = 150)
    private String companyName;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Specialty category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Priority priority;

    @Column(name = "priority_rank", nullable = false)
    private int priorityRank;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TicketStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "required_level", nullable = false, length = 2)
    private SupportLevel requiredLevel;

    @Column(name = "assigned_tech_id", length = 20)
    private String assignedTechId;

    @Column(name = "assigned_tech_name", length = 150)
    private String assignedTechName;

    @Enumerated(EnumType.STRING)
    @Column(name = "assigned_tech_level", length = 2)
    private SupportLevel assignedTechLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "queue_name", length = 20)
    private QueueName queueName;

    @Column(name = "resolution_log", columnDefinition = "TEXT")
    private String resolutionLog;

    @Column(name = "resolution_code", length = 40)
    private String resolutionCode;

    @Column(name = "registered_at", nullable = false)
    private Instant registeredAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "assigned_at")
    private Instant assignedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Version
    private long version;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("transferDate ASC")
    private List<EscalationRecord> escalations = new ArrayList<>();

    protected Ticket() {
    }

    public static Ticket register(String clientId, String companyName, String title, String description,
                                  Specialty category, Priority priority, SupportLevel requiredLevel) {
        Ticket t = new Ticket();
        t.clientId = clientId;
        t.companyName = companyName;
        t.title = title;
        t.description = description;
        t.category = category;
        t.priority = priority;
        t.priorityRank = priority.rank();
        t.requiredLevel = requiredLevel;
        t.status = TicketStatus.REGISTRADO;
        t.queueName = priority.queue();
        return t;
    }

    @PrePersist
    void onCreate() {
        registeredAt = Instant.now();
        updatedAt = registeredAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    /** TKT_102030: código legible derivado del secuencial (AUTO_INCREMENT inicia en 102001). */
    public void assignCode() {
        this.ticketId = String.format("TKT_%06d", id);
    }

    // ------------------------------------------------------------------ reglas de negocio

    private void moveTo(TicketStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new ApiException(HttpStatus.CONFLICT, "invalid_transition",
                    "No se puede pasar de " + status + " a " + target);
        }
        status = target;
    }

    /** Vincula al técnico: ASIGNADO si es la primera vez, REASIGNADO si viene de escalamiento/reasignación. */
    public TicketStatus assignTechnician(String techId, String techName, SupportLevel techLevel) {
        TicketStatus target = status == TicketStatus.REGISTRADO ? TicketStatus.ASIGNADO : TicketStatus.REASIGNADO;
        moveTo(target);
        this.assignedTechId = techId;
        this.assignedTechName = techName;
        this.assignedTechLevel = techLevel;
        this.assignedAt = Instant.now();
        this.queueName = null;
        lastOpenEscalation().ifPresent(e -> e.completeWith(techId));
        return target;
    }

    public void startWork() {
        moveTo(TicketStatus.EN_PROCESO);
    }

    /**
     * Escalamiento: funcional (cambia la especialidad) o jerárquico (sube de nivel N1→N2→N3).
     * El técnico actual se libera y el caso vuelve a la cola prioritaria preservando la bitácora.
     * @return id del técnico que se libera (puede ser null)
     */
    public String escalate(String reason, Specialty newCategory) {
        Specialty previousCategory = category;
        SupportLevel previousLevel = requiredLevel;
        boolean functional = newCategory != null && newCategory != category;
        if (functional) {
            category = newCategory;
        } else {
            SupportLevel base = SupportLevel.max(requiredLevel, assignedTechLevel);
            requiredLevel = base.next().orElseThrow(() -> ApiException.unprocessable("max_level_reached",
                    "El ticket ya está en el nivel máximo (N3). Indique otra especialidad o reasigne manualmente."));
        }
        moveTo(TicketStatus.ESCALADO);
        String released = assignedTechId;
        escalations.add(new EscalationRecord(this, EscalationRecord.Type.ESCALAMIENTO, released, reason,
                previousCategory, category, previousLevel, requiredLevel));
        clearTechnician();
        queueName = QueueName.PRIORITY_QUEUE;
        return released;
    }

    /** Reasignación directa a otro técnico idóneo (sin pasar por la cola). */
    public String reassign(String reason, String newTechId, String newTechName, SupportLevel newTechLevel) {
        if (!status.canTransitionTo(TicketStatus.REASIGNADO)) {
            throw new ApiException(HttpStatus.CONFLICT, "invalid_transition",
                    "No se puede reasignar un ticket en estado " + status);
        }
        String released = assignedTechId;
        if (status != TicketStatus.ESCALADO) {
            escalations.add(new EscalationRecord(this, EscalationRecord.Type.REASIGNACION, released, reason,
                    category, category, requiredLevel, requiredLevel));
        }
        status = TicketStatus.REASIGNADO;
        assignedTechId = newTechId;
        assignedTechName = newTechName;
        assignedTechLevel = newTechLevel;
        assignedAt = Instant.now();
        queueName = null;
        lastOpenEscalation().ifPresent(e -> e.completeWith(newTechId));
        return released;
    }

    public void resolve(String log, String code) {
        moveTo(TicketStatus.RESUELTO);
        this.resolutionLog = log;
        this.resolutionCode = code;
        this.resolvedAt = Instant.now();
    }

    public void close() {
        moveTo(TicketStatus.CERRADO);
        this.closedAt = Instant.now();
    }

    public void changePriority(Priority newPriority) {
        if (status == TicketStatus.RESUELTO || status == TicketStatus.CERRADO) {
            throw new ApiException(HttpStatus.CONFLICT, "invalid_transition",
                    "No se puede cambiar la prioridad de un ticket " + status);
        }
        this.priority = newPriority;
        this.priorityRank = newPriority.rank();
        if (newPriority == Priority.CRITICA && requiredLevel == SupportLevel.N1) {
            requiredLevel = SupportLevel.N2;
        }
        if (status.isWaiting()) {
            queueName = status == TicketStatus.ESCALADO ? QueueName.PRIORITY_QUEUE : newPriority.queue();
        }
    }

    private void clearTechnician() {
        assignedTechId = null;
        assignedTechName = null;
        assignedTechLevel = null;
        assignedAt = null;
    }

    private Optional<EscalationRecord> lastOpenEscalation() {
        for (int i = escalations.size() - 1; i >= 0; i--) {
            EscalationRecord e = escalations.get(i);
            if (e.getNewTechId() == null) {
                return Optional.of(e);
            }
        }
        return Optional.empty();
    }

    /** Técnico que tenía el caso antes del último escalamiento (para no reasignárselo). */
    public Optional<String> lastEscalatedFrom() {
        return escalations.isEmpty() ? Optional.empty()
                : Optional.ofNullable(escalations.get(escalations.size() - 1).getPreviousTechId());
    }

    public boolean isAssignedTo(String techId) {
        return techId != null && techId.equals(assignedTechId);
    }

    public Long getId() { return id; }
    public String getTicketId() { return ticketId; }
    public String getClientId() { return clientId; }
    public String getCompanyName() { return companyName; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Specialty getCategory() { return category; }
    public Priority getPriority() { return priority; }
    public int getPriorityRank() { return priorityRank; }
    public TicketStatus getStatus() { return status; }
    public SupportLevel getRequiredLevel() { return requiredLevel; }
    public String getAssignedTechId() { return assignedTechId; }
    public String getAssignedTechName() { return assignedTechName; }
    public SupportLevel getAssignedTechLevel() { return assignedTechLevel; }
    public QueueName getQueueName() { return queueName; }
    public String getResolutionLog() { return resolutionLog; }
    public String getResolutionCode() { return resolutionCode; }
    public Instant getRegisteredAt() { return registeredAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getAssignedAt() { return assignedAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public Instant getClosedAt() { return closedAt; }
    public List<EscalationRecord> getEscalations() { return escalations; }
}
