package pe.techsupport.ticket.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import pe.techsupport.ticket.domain.Priority;
import pe.techsupport.ticket.domain.Specialty;
import pe.techsupport.ticket.domain.SupportLevel;
import pe.techsupport.ticket.domain.TicketStatus;

/**
 * Contratos del Ticket Service (Tablas 4 y 7, Fig. 12-16 del documento).
 */
public final class TicketDtos {

    private TicketDtos() {
    }

    // ------------------------------------------------------------------ entradas

    /** userId es obligatorio solo cuando registra un ADMIN; para un CLIENTE se toma del token. */
    public record CreateTicketRequest(
            @Size(max = 20) String userId,
            @NotBlank @Size(max = 150) String title,
            @NotBlank @Size(max = 4000) String description,
            @NotNull Specialty category) {
    }

    public record PriorityRequest(@NotNull Priority priority, @Size(max = 300) String justification) {
    }

    public record AssignRequest(@Size(max = 20) String technicianId) {
    }

    public record EscalateRequest(
            @NotBlank @Size(max = 500) String reason,
            Specialty newCategory,
            @Size(max = 20) String newTechId) {
    }

    public record ReassignRequest(
            @NotBlank @Size(max = 20) String newTechnicianId,
            @Size(max = 500) String reason) {
    }

    public record ResolveRequest(
            @NotBlank @Size(max = 4000) String resolutionLog,
            @NotBlank @Size(max = 40) String resolutionCode) {
    }

    public record CloseRequest(@Size(max = 500) String comment) {
    }

    // ------------------------------------------------------------------ salidas

    public record IncidentData(String title, String description, Specialty category, Instant registeredAt,
                               Instant updatedAt) {
    }

    public record AssignedTechnician(String userId, String fullName, SupportLevel supportLevel) {
    }

    public record QueueInfo(long position, String estimatedWaitTime, String queueName) {
    }

    public record EscalationEntry(String type, String previousTechId, String newTechId, String reason,
                                  Specialty previousCategory, Specialty newCategory, SupportLevel previousLevel,
                                  SupportLevel newLevel, Instant transferDate) {
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record TicketResponse(String ticketId, String userId, String companyName, IncidentData incidentData,
                                 Priority priority, TicketStatus status, SupportLevel requiredLevel,
                                 AssignedTechnician assignedTechnician, QueueInfo queueInfo,
                                 List<EscalationEntry> escalationHistory, String resolutionLog,
                                 String resolutionCode, Instant assignedAt, Instant resolvedAt, Instant closedAt) {
    }

    public record TicketSummary(String ticketId, String userId, String title, Specialty category, Priority priority,
                                TicketStatus status, String assignedTechnicianId, String queueName,
                                Instant registeredAt, Instant updatedAt) {
    }

    public record TicketEventResponse(String eventType, TicketStatus fromStatus, TicketStatus toStatus,
                                      String actorId, String actorRole, String detail, Instant createdAt) {
    }

    // ------------------------------------------------------------------ contratos del User Service

    public record ClientValidation(boolean valid, String clientId, String status, String companyName) {
    }

    public record TechnicianCandidate(String userId, String fullName, SupportLevel supportLevel,
                                      Set<String> specialties,
                                      @JsonProperty("isAvailable") boolean isAvailable,
                                      int currentTickets, int maxConcurrentTickets) {
    }

    public record AvailableTechnicians(boolean available, List<TechnicianCandidate> technicians) {
    }

    public record TechnicianProfile(String userId, String firstName, String lastName, SupportLevel supportLevel,
                                    Set<String> specialties, String status, String availability,
                                    @JsonProperty("isAvailable") boolean isAvailable,
                                    int currentTickets, int maxConcurrentTickets) {
        public String fullName() {
            return firstName + " " + lastName;
        }
    }
}
