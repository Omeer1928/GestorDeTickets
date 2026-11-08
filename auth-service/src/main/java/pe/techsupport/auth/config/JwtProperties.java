package pe.techsupport.auth.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** Clave HMAC-SHA256 en Base64 (mínimo 256 bits). Compartida solo con el API Gateway. */
    private String secret;
    private String issuer = "techsupport-auth";
    private Duration refreshTtl = Duration.ofHours(24);
    /** Duración del access token por rol: ADMIN 15m, TECNICO 30m, CLIENTE 30m. */
    private Map<String, Duration> accessTtl = new HashMap<>();

    public Duration accessTtlFor(String role) {
        return accessTtl.entrySet().stream()
                .filter(e -> e.getKey().equalsIgnoreCase(role))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(Duration.ofMinutes(15));
    }

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
    public Duration getRefreshTtl() { return refreshTtl; }
    public void setRefreshTtl(Duration refreshTtl) { this.refreshTtl = refreshTtl; }
    public Map<String, Duration> getAccessTtl() { return accessTtl; }
    public void setAccessTtl(Map<String, Duration> accessTtl) { this.accessTtl = accessTtl; }
}
