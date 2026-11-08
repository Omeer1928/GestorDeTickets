package pe.techsupport.common.security;

/**
 * Roles del sistema (sección 11 del documento). SYSTEM es un rol técnico, solo para llamadas servicio-a-servicio.
 */
public final class Roles {
    public static final String ADMIN = "ADMIN";
    public static final String CLIENTE = "CLIENTE";
    public static final String TECNICO = "TECNICO";
    public static final String SYSTEM = "SYSTEM";

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_EMAIL = "X-User-Email";
    public static final String HEADER_USER_ROLE = "X-User-Role";
    public static final String HEADER_INTERNAL_KEY = "X-Internal-Key";

    private Roles() {
    }
}
