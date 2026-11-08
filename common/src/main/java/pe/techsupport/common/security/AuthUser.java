package pe.techsupport.common.security;

/**
 * Identidad propagada por el API Gateway mediante las cabeceras X-User-Id, X-User-Email y X-User-Role.
 */
public record AuthUser(String userId, String email, String role) {

    public boolean isAdmin() { return Roles.ADMIN.equals(role); }
    public boolean isClient() { return Roles.CLIENTE.equals(role); }
    public boolean isTechnician() { return Roles.TECNICO.equals(role); }
    public boolean isSystem() { return Roles.SYSTEM.equals(role); }
}
