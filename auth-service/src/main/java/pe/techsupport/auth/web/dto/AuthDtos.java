package pe.techsupport.auth.web.dto;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Contratos de datos del Authentication Service (Tabla 1 y Tabla 5 del documento).
 */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password) {
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record LoginResponse(
            String userId,
            String fullName,
            String userType,
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresIn,
            Long refreshExpiresIn) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record LogoutRequest(String refreshToken) {
    }

    public record ValidateResponse(String userId, String email, String role, boolean valid, Instant expiresAt) {
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 60) String firstName,
            @NotBlank @Size(max = 80) String lastName,
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank
            @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,64}$",
                    message = "debe tener 8-64 caracteres con mayúscula, minúscula, número y símbolo")
            String password,
            @NotBlank @Pattern(regexp = "CLIENTE|TECNICO", message = "debe ser CLIENTE o TECNICO") String userType,
            @Size(max = 20) String phone,
            // Solo CLIENTE
            @Size(max = 150) String companyName,
            @Pattern(regexp = "^$|\\d{11}", message = "el RUC debe tener 11 dígitos") String ruc,
            // Solo TECNICO
            @Pattern(regexp = "^$|N1|N2|N3", message = "debe ser N1, N2 o N3") String supportLevel,
            List<String> specialties,
            @Min(1) @Max(20) Integer maxConcurrentTickets) {
    }

    public record RegisterResponse(String message, String userId, String userType) {
    }

    public record MessageResponse(String message) {
    }

    public record RoleResponse(String code, String description) {
    }
}
