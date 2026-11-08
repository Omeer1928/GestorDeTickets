package pe.techsupport.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import pe.techsupport.auth.config.AuthProperties;

/**
 * Control de intentos fallidos (anti fuerza bruta) en Redis: tras N fallos la cuenta se bloquea
 * temporalmente. Vive en Redis y no en MySQL para no depender de la BD y expirar solo.
 */
@Service
public class LoginAttemptService {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);

    private final StringRedisTemplate redis;
    private final AuthProperties props;

    public LoginAttemptService(StringRedisTemplate redis, AuthProperties props) {
        this.redis = redis;
        this.props = props;
    }

    public boolean isLocked(String email) {
        try {
            return Boolean.TRUE.equals(redis.hasKey(lockKey(email)));
        } catch (Exception ex) {
            log.warn("Redis no disponible para verificar bloqueo de {}: {}", email, ex.getMessage());
            return false;
        }
    }

    /** @return intentos restantes antes del bloqueo (0 si quedó bloqueada) */
    public int registerFailure(String email) {
        try {
            Long failures = redis.opsForValue().increment(failKey(email));
            redis.expire(failKey(email), props.getLockDuration());
            int count = failures == null ? 1 : failures.intValue();
            if (count >= props.getMaxFailedAttempts()) {
                redis.opsForValue().set(lockKey(email), "1", props.getLockDuration());
                redis.delete(failKey(email));
                return 0;
            }
            return props.getMaxFailedAttempts() - count;
        } catch (Exception ex) {
            log.warn("Redis no disponible para registrar intento fallido de {}: {}", email, ex.getMessage());
            return props.getMaxFailedAttempts();
        }
    }

    public void reset(String email) {
        try {
            redis.delete(failKey(email));
        } catch (Exception ex) {
            log.debug("No se pudo limpiar contador de intentos de {}", email);
        }
    }

    private static String failKey(String email) {
        return "auth:fail:" + email;
    }

    private static String lockKey(String email) {
        return "auth:lock:" + email;
    }
}
