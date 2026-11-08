package pe.techsupport.common.resilience;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import pe.techsupport.common.config.CommonProperties;

/**
 * Caché "cache-aside" sobre Redis con respaldo stale.
 *
 * <ol>
 *   <li>HIT: el valor fresco está en Redis → se devuelve sin tocar la BD.</li>
 *   <li>MISS: se carga desde la fuente (BD u otro servicio) a través de un circuit breaker y se guardan
 *       dos copias: la fresca (TTL corto) y la stale (TTL largo).</li>
 *   <li>STALE: si la fuente falla por infraestructura (BD caída, circuito abierto) se devuelve la última
 *       copia conocida y se marca la respuesta con la cabecera {@code X-Cache: STALE}.</li>
 * </ol>
 * Si Redis cae, la caché se omite (fail-open) y el servicio sigue funcionando contra la BD.
 */
@Component
public class ResilientCache {

    private static final Logger log = LoggerFactory.getLogger(ResilientCache.class);
    public static final String DEFAULT_CIRCUIT = "db";
    public static final String HEADER = "X-Cache";

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final CircuitBreakerRegistry circuitBreakers;
    private final MeterRegistry meters;
    private final CommonProperties props;
    private final String prefix;

    public ResilientCache(StringRedisTemplate redis, ObjectMapper mapper, CircuitBreakerRegistry circuitBreakers,
                          MeterRegistry meters, CommonProperties props,
                          @Value("${spring.application.name}") String appName) {
        this.redis = redis;
        this.mapper = mapper;
        this.circuitBreakers = circuitBreakers;
        this.meters = meters;
        this.props = props;
        this.prefix = appName + ":";
    }

    public <T> T get(String cache, String key, Duration ttl, Class<T> type, Supplier<T> loader) {
        return get(cache, key, ttl, mapper.getTypeFactory().constructType(type), DEFAULT_CIRCUIT, loader);
    }

    public <T> T get(String cache, String key, Duration ttl, TypeReference<T> type, Supplier<T> loader) {
        return get(cache, key, ttl, mapper.getTypeFactory().constructType(type), DEFAULT_CIRCUIT, loader);
    }

    public <T> T get(String cache, String key, Duration ttl, TypeReference<T> type, String circuit,
                     Supplier<T> loader) {
        return get(cache, key, ttl, mapper.getTypeFactory().constructType(type), circuit, loader);
    }

    public <T> T get(String cache, String key, Duration ttl, Class<T> type, String circuit, Supplier<T> loader) {
        return get(cache, key, ttl, mapper.getTypeFactory().constructType(type), circuit, loader);
    }

    private <T> T get(String cache, String key, Duration ttl, JavaType type, String circuit, Supplier<T> loader) {
        if (!props.getCache().isEnabled()) {
            count(cache, "bypass");
            return circuitBreakers.circuitBreaker(circuit).executeSupplier(loader);
        }
        T fresh = read(freshKey(cache, key), type, cache);
        if (fresh != null) {
            count(cache, "hit");
            markResponse("HIT");
            return fresh;
        }
        T loaded;
        try {
            loaded = circuitBreakers.circuitBreaker(circuit).executeSupplier(loader);
        } catch (RuntimeException ex) {
            if (InfraFailures.isInfrastructureFailure(ex)) {
                T stale = read(staleKey(cache, key), type, cache);
                if (stale != null) {
                    count(cache, "stale");
                    markResponse("STALE");
                    log.warn("Fuente no disponible para {}:{} — respondiendo con copia stale ({})", cache, key,
                            InfraFailures.rootMessage(ex));
                    return stale;
                }
                count(cache, "unavailable");
            }
            throw ex;
        }
        count(cache, "miss");
        markResponse("MISS");
        put(cache, key, loaded, ttl);
        return loaded;
    }

    public void put(String cache, String key, Object value, Duration ttl) {
        if (value == null || !props.getCache().isEnabled()) {
            return;
        }
        try {
            String json = mapper.writeValueAsString(value);
            redis.opsForValue().set(freshKey(cache, key), json, ttl);
            redis.opsForValue().set(staleKey(cache, key), json, props.getCache().getStaleTtl());
        } catch (Exception ex) {
            count(cache, "error");
            log.debug("No se pudo escribir en Redis {}:{} — {}", cache, key, ex.getMessage());
        }
    }

    /**
     * Ejecuta trabajo de caché después del commit (si hay transacción activa), para no publicar en Redis
     * datos que luego se revierten ni dejar que una lectura concurrente re-cachee el valor anterior.
     */
    public void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    /** Invalida la copia fresca. La stale se conserva como "último valor conocido" para contingencias. */
    public void evict(String cache, String key) {
        try {
            redis.delete(freshKey(cache, key));
        } catch (Exception ex) {
            count(cache, "error");
            log.debug("No se pudo invalidar {}:{} — {}", cache, key, ex.getMessage());
        }
    }

    /** Invalida todas las entradas frescas de una caché (listados). */
    public void evictAll(String cache) {
        try {
            List<String> keys = new ArrayList<>();
            ScanOptions options = ScanOptions.scanOptions().match(prefix + "c:" + cache + ":*").count(200).build();
            try (Cursor<String> cursor = redis.scan(options)) {
                cursor.forEachRemaining(keys::add);
            }
            if (!keys.isEmpty()) {
                redis.delete(keys);
            }
        } catch (Exception ex) {
            count(cache, "error");
            log.debug("No se pudo invalidar la caché {} — {}", cache, ex.getMessage());
        }
    }

    private <T> T read(String redisKey, JavaType type, String cache) {
        try {
            String json = redis.opsForValue().get(redisKey);
            return json == null ? null : mapper.readValue(json, type);
        } catch (Exception ex) {
            count(cache, "error");
            log.debug("Lectura de Redis fallida ({}): {}", redisKey, ex.getMessage());
            return null;
        }
    }

    private String freshKey(String cache, String key) {
        return prefix + "c:" + cache + ":" + key;
    }

    private String staleKey(String cache, String key) {
        return prefix + "s:" + cache + ":" + key;
    }

    private void count(String cache, String result) {
        Counter.builder("cache.requests")
                .description("Resultado de las lecturas de la caché resiliente")
                .tag("cache", cache)
                .tag("result", result)
                .register(meters)
                .increment();
    }

    private static void markResponse(String value) {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletResponse response = attrs.getResponse();
            if (response != null && !response.isCommitted() && !"STALE".equals(response.getHeader(HEADER))) {
                response.setHeader(HEADER, value);
            }
        }
    }
}
