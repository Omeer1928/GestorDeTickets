package pe.techsupport.auth.service;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import pe.techsupport.common.error.ApiException;

/**
 * Lista negra de access tokens revocados (logout). La clave expira junto con el token, así Redis
 * nunca crece sin control. El API Gateway consulta la misma clave en cada petición.
 */
@Service
public class TokenBlacklist {

    public static final String PREFIX = "auth:bl:";
    private static final Logger log = LoggerFactory.getLogger(TokenBlacklist.class);

    private final StringRedisTemplate redis;

    public TokenBlacklist(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void revoke(String jti, Duration remaining) {
        if (remaining.isNegative() || remaining.isZero()) {
            return;
        }
        try {
            redis.opsForValue().set(PREFIX + jti, "1", remaining);
        } catch (Exception ex) {
            throw ApiException.unavailable("revocation_unavailable",
                    "No se pudo revocar el token (Redis no disponible). Intente nuevamente.");
        }
    }

    public boolean isRevoked(String jti) {
        try {
            return Boolean.TRUE.equals(redis.hasKey(PREFIX + jti));
        } catch (Exception ex) {
            log.warn("Redis no disponible al consultar la lista negra: {}", ex.getMessage());
            return false;
        }
    }
}
