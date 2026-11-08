package pe.techsupport.user.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import pe.techsupport.user.domain.ClientStatus;
import pe.techsupport.user.domain.Specialty;
import pe.techsupport.user.domain.SupportLevel;

/**
 * Contratos del User Service (Tablas 2, 3 y 6 del documento).
 */
public final class UserDtos {

    private UserDtos() {
    }

    // ------------------------------------------------------------------ clientes

    public record CreateClientRequest(
            @NotBlank @Size(max = 60) String firstName,
            @NotBlank @Size(max = 80) String lastName,
            @NotBlank @Email @Size(max = 150) String email,
            @Size(max = 20) String phone,
            @Size(max = 150) String companyName,
            @Pattern(regexp = "^$|\\d{11}", message = "el RUC debe tener 11 dígitos") String ruc) {
    }

    public record UpdateClientRequest(
            @Size(max = 60) String firstName,
            @Size(max = 80) String lastName,
            @Size(max = 20) String phone,
            @Size(max = 150) String companyName,
            @Pattern(regexp = "^$|\\d{11}", message = "el RUC debe tener 11 dígitos") String ruc,
            ClientStatus status) {
    }

    public record ClientResponse(String userId, String firstName, String lastName, String email, String phone,
                                 String companyName, String ruc, String userType, ClientStatus status,
                                 Instant createdAt, Instant updatedAt) {
    }

    public record ClientSummary(String userId, String firstName, String lastName, String companyName,
                                ClientStatus status) {
    }

    public record ClientValidation(boolean valid, String clientId, ClientStatus status, String companyName) {
    }

    // ------------------------------------------------------------------ técnicos

    public record CreateTechnicianRequest(
            @NotBlank @Size(max = 60) String firstName,
            @NotBlank @Size(max = 80) String lastName,
            @NotBlank @Email @Size(max = 150) String email,
            @Size(max = 20) String phone,
            @NotNull SupportLevel supportLevel,
            @NotEmpty(message = "debe indicar al menos una especialidad") Set<Specialty> specialties,
            @Min(1) @Max(20) Integer maxConcurrentTickets) {
    }

    public record TechnicianResponse(String userId, String firstName, String lastName, String email, String phone,
                                     String userType, SupportLevel supportLevel, Set<Specialty> specialties,
                                     String status, String availability,
                                     @JsonProperty("isAvailable") boolean isAvailable,
                                     int currentTickets, int maxConcurrentTickets,
                                     Instant createdAt, Instant updatedAt) {
    }

    public record TechnicianCandidate(String userId, String fullName, SupportLevel supportLevel,
                                      Set<Specialty> specialties,
                                      @JsonProperty("isAvailable") boolean isAvailable,
                                      int currentTickets, int maxConcurrentTickets) {
    }

    public record AvailableTechnicians(boolean available, List<TechnicianCandidate> technicians) {
    }

    public record AvailabilityRequest(@NotNull @JsonProperty("isAvailable") Boolean isAvailable) {
    }

    public record WorkloadResponse(String userId, int currentAssignedTickets, int maxConcurrentTickets,
                                   @JsonProperty("isAvailable") boolean isAvailable, String availability) {
    }

    public record WorkloadAdjustRequest(
            @NotNull @Min(-1) @Max(1) Integer delta) {
    }

    public record SpecialtyResponse(String code, String description) {
    }
}
