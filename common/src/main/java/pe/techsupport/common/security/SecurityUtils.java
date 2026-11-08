package pe.techsupport.common.security;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import pe.techsupport.common.error.ApiException;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<AuthUser> currentUserOptional() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthUser user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    public static AuthUser currentUser() {
        return currentUserOptional().orElseThrow(() -> ApiException.unauthorized("Se requiere autenticación"));
    }

    /** Un CLIENTE/TECNICO solo puede operar sobre su propio recurso; ADMIN y SYSTEM sobre cualquiera. */
    public static void requireSelfOrPrivileged(String ownerId) {
        AuthUser user = currentUser();
        if (user.isAdmin() || user.isSystem()) {
            return;
        }
        if (!user.userId().equals(ownerId)) {
            throw ApiException.forbidden("No tiene permisos sobre el recurso " + ownerId);
        }
    }
}
