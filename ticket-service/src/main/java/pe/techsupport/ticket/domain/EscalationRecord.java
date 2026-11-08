package pe.techsupport.ticket.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "escalation_record")
public class EscalationRecord {

    public enum Type { ESCALAMIENTO, REASIGNACION }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_pk")
    private Ticket ticket;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Type type;

    @Column(name = "previous_tech_id", length = 20)
    private String previousTechId;

    @Column(name = "new_tech_id", length = 20)
    private String newTechId;

    @Column(nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_category", length = 40)
    private Specialty previousCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_category", length = 40)
    private Specialty newCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_level", length = 2)
    private SupportLevel previousLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_level", length = 2)
    private SupportLevel newLevel;

    @Column(name = "transfer_date", nullable = false)
    private Instant transferDate;

    protected EscalationRecord() {
    }

    EscalationRecord(Ticket ticket, Type type, String previousTechId, String reason, Specialty previousCategory,
                     Specialty newCategory, SupportLevel previousLevel, SupportLevel newLevel) {
        this.ticket = ticket;
        this.type = type;
        this.previousTechId = previousTechId;
        this.reason = reason;
        this.previousCategory = previousCategory;
        this.newCategory = newCategory;
        this.previousLevel = previousLevel;
        this.newLevel = newLevel;
        this.transferDate = Instant.now();
    }

    void completeWith(String techId) {
        this.newTechId = techId;
    }

    public Long getId() { return id; }
    public Type getType() { return type; }
    public String getPreviousTechId() { return previousTechId; }
    public String getNewTechId() { return newTechId; }
    public String getReason() { return reason; }
    public Specialty getPreviousCategory() { return previousCategory; }
    public Specialty getNewCategory() { return newCategory; }
    public SupportLevel getPreviousLevel() { return previousLevel; }
    public SupportLevel getNewLevel() { return newLevel; }
    public Instant getTransferDate() { return transferDate; }
}
