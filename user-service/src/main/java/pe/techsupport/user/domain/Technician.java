package pe.techsupport.user.domain;

import java.time.Instant;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "technician")
public class Technician {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seq;

    @Column(name = "user_id", unique = true, length = 20)
    private String userId;

    @Column(name = "first_name", nullable = false, length = 60)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 80)
    private String lastName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "support_level", nullable = false, length = 2)
    private SupportLevel supportLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TechnicianStatus status = TechnicianStatus.ACTIVO;

    @Column(name = "is_available", nullable = false)
    private boolean available = true;

    @Column(name = "current_tickets", nullable = false)
    private int currentTickets;

    @Column(name = "max_concurrent_tickets", nullable = false)
    private int maxConcurrentTickets;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "technician_specialty", joinColumns = @JoinColumn(name = "technician_seq"))
    @Enumerated(EnumType.STRING)
    @Column(name = "specialty", length = 40)
    private Set<Specialty> specialties = new HashSet<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected Technician() {
    }

    public Technician(String firstName, String lastName, String email, String phone, SupportLevel supportLevel,
                      Set<Specialty> specialties, int maxConcurrentTickets) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.supportLevel = supportLevel;
        this.specialties = specialties.isEmpty() ? new HashSet<>() : EnumSet.copyOf(specialties);
        this.maxConcurrentTickets = maxConcurrentTickets;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void assignUserId() {
        this.userId = String.format("USR-T%04d", seq);
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    /** DISPONIBLE si está activo, marcado disponible y con capacidad libre; OCUPADO en otro caso. */
    public String availabilityState() {
        return status == TechnicianStatus.ACTIVO && available && currentTickets < maxConcurrentTickets
                ? "DISPONIBLE" : "OCUPADO";
    }

    public String fullName() {
        return firstName + " " + lastName;
    }

    public Long getSeq() { return seq; }
    public String getUserId() { return userId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public SupportLevel getSupportLevel() { return supportLevel; }
    public TechnicianStatus getStatus() { return status; }
    public boolean isAvailable() { return available; }
    public int getCurrentTickets() { return currentTickets; }
    public int getMaxConcurrentTickets() { return maxConcurrentTickets; }
    public Set<Specialty> getSpecialties() { return specialties; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
