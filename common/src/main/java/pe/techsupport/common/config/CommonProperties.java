package pe.techsupport.common.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades compartidas por los servicios de negocio (prefijo {@code app}).
 */
@ConfigurationProperties(prefix = "app")
public class CommonProperties {

    /** Clave compartida que solo conocen los servicios internos (llamadas con rol SYSTEM). */
    private String internalKey = "change-me";

    private final Security security = new Security();
    private final Cache cache = new Cache();

    public String getInternalKey() { return internalKey; }
    public void setInternalKey(String internalKey) { this.internalKey = internalKey; }
    public Security getSecurity() { return security; }
    public Cache getCache() { return cache; }

    public static class Security {
        /** Rutas que no requieren usuario autenticado (patrones Ant). */
        private List<String> publicPaths = new ArrayList<>();

        public List<String> getPublicPaths() { return publicPaths; }
        public void setPublicPaths(List<String> publicPaths) { this.publicPaths = publicPaths; }
    }

    public static class Cache {
        /** Activa/desactiva la caché Redis (útil para comparar latencias en la demo). */
        private boolean enabled = true;
        /** Tiempo que se conserva la copia "stale" usada como respaldo cuando la BD no responde. */
        private Duration staleTtl = Duration.ofHours(24);

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public Duration getStaleTtl() { return staleTtl; }
        public void setStaleTtl(Duration staleTtl) { this.staleTtl = staleTtl; }
    }
}
