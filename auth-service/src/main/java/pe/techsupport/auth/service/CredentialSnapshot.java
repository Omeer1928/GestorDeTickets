package pe.techsupport.auth.service;

import com.fasterxml.jackson.annotation.JsonIgnore;

import pe.techsupport.auth.domain.Credential;

/**
 * Copia inmutable de la credencial que se guarda en Redis. Permite que el login siga funcionando
 * (con el último estado conocido) si la BD de autenticación cae. Solo contiene el hash BCrypt,
 * nunca la contraseña. Se puede desactivar con CACHE_ENABLED=false.
 */
public record CredentialSnapshot(String userId, String email, String passwordHash, String role, String fullName,
                                 String status) {

    public static CredentialSnapshot from(Credential c) {
        return new CredentialSnapshot(c.getUserId(), c.getEmail(), c.getPasswordHash(), c.getRole(),
                c.getFullName(), c.getStatus());
    }

    @JsonIgnore
    public boolean isActive() {
        return "ACTIVO".equals(status);
    }
}
