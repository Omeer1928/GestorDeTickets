package pe.techsupport.common.resilience;

import java.util.concurrent.atomic.AtomicInteger;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cada servicio comprueba cada 5 s su propia BD (SELECT 1) y Redis (PING) y lo publica como métricas
 * database_up y cache_redis_up (1 = arriba, 0 = caída). Así Prometheus/Grafana muestran el estado de la
 * capa de datos sin contenedores exporter adicionales.
 */
@Component
public class DependencyHealthProbe {

    private final ObjectProvider<JdbcTemplate> jdbc;
    private final StringRedisTemplate redis;
    private final AtomicInteger databaseUp = new AtomicInteger(1);
    private final AtomicInteger redisUp = new AtomicInteger(1);

    public DependencyHealthProbe(ObjectProvider<JdbcTemplate> jdbc, StringRedisTemplate redis, MeterRegistry meters) {
        this.jdbc = jdbc;
        this.redis = redis;
        Gauge.builder("database.up", databaseUp, AtomicInteger::get)
                .description("1 si la base de datos propia del servicio responde, 0 si está caída")
                .register(meters);
        Gauge.builder("cache.redis.up", redisUp, AtomicInteger::get)
                .description("1 si Redis responde, 0 si está caído")
                .register(meters);
    }

    @Scheduled(initialDelay = 5000, fixedDelay = 5000)
    public void probe() {
        JdbcTemplate template = jdbc.getIfAvailable();
        if (template != null) {
            try {
                template.queryForObject("SELECT 1", Integer.class);
                databaseUp.set(1);
            } catch (Exception ex) {
                databaseUp.set(0);
            }
        }
        try {
            redis.execute((RedisCallback<String>) RedisConnection::ping);
            redisUp.set(1);
        } catch (Exception ex) {
            redisUp.set(0);
        }
    }
}
