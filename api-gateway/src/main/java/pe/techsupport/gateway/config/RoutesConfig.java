package pe.techsupport.gateway.config;

import java.time.Duration;
import java.util.Optional;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import reactor.core.publisher.Mono;

import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import pe.techsupport.gateway.filter.JwtAuthenticationFilter;

/**
 * Enrutamiento por path hacia cada servicio (el cliente nunca conoce direcciones ni puertos internos).
 * Cada ruta aplica: rate limiting en Redis, circuit breaker con fallback JSON y reintento solo para
 * GET ante 502/504 (idempotentes).
 */
@Configuration
public class RoutesConfig {

    @Bean
    RouteLocator routes(RouteLocatorBuilder builder, GatewayProperties props, RedisRateLimiter rateLimiter,
                        KeyResolver userOrIpKeyResolver) {
        return builder.routes()
                .route("auth-service", r -> r.path("/auth/**")
                        .filters(f -> common(f, "auth-service", rateLimiter, userOrIpKeyResolver))
                        .uri(props.services().authUrl()))
                .route("user-service", r -> r.path("/users/**")
                        .filters(f -> common(f, "user-service", rateLimiter, userOrIpKeyResolver))
                        .uri(props.services().userUrl()))
                .route("ticket-service", r -> r.path("/tickets/**")
                        .filters(f -> common(f, "ticket-service", rateLimiter, userOrIpKeyResolver))
                        .uri(props.services().ticketUrl()))
                .build();
    }

    private static GatewayFilterSpec common(GatewayFilterSpec f, String service, RedisRateLimiter limiter,
                                            KeyResolver keyResolver) {
        return f.requestRateLimiter(c -> c.setRateLimiter(limiter).setKeyResolver(keyResolver))
                .circuitBreaker(c -> c.setName(service).setFallbackUri("forward:/fallback/" + service))
                .retry(c -> c.setRetries(2)
                        .setMethods(HttpMethod.GET)
                        .setSeries()
                        .setStatuses(HttpStatus.BAD_GATEWAY, HttpStatus.GATEWAY_TIMEOUT));
    }

    @Bean
    RedisRateLimiter redisRateLimiter(GatewayProperties props) {
        return new RedisRateLimiter(props.rateLimit().replenishRate(), props.rateLimit().burstCapacity(), 1);
    }

    /** Limita por usuario autenticado; si es anónimo (login, registro), por IP de origen. */
    @Bean
    @Primary
    KeyResolver userOrIpKeyResolver() {
        return exchange -> {
            Object userId = exchange.getAttribute(JwtAuthenticationFilter.ATTR_USER_ID);
            if (userId != null) {
                return Mono.just("user:" + userId);
            }
            // Se usa la IP real de la conexión (no X-Forwarded-For, que el cliente puede falsificar)
            String ip = Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                    .map(a -> a.getAddress().getHostAddress()).orElse("unknown");
            return Mono.just("ip:" + ip);
        };
    }

    /**
     * Circuit breaker por servicio: abre tras 50% de fallos de conexión/timeout. Timeout de 6 s, salvo el
     * Ticket Service (20 s), que orquesta varias llamadas al User Service dentro de una misma petición.
     */
    @Bean
    Customizer<ReactiveResilience4JCircuitBreakerFactory> circuitBreakerDefaults() {
        return factory -> {
            factory.configureDefault(id -> breaker(id, Duration.ofSeconds(6)));
            factory.configure(builder -> builder
                    .circuitBreakerConfig(breakerConfig())
                    .timeLimiterConfig(TimeLimiterConfig.custom().timeoutDuration(Duration.ofSeconds(20)).build()),
                    "ticket-service");
        };
    }

    private static org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder.Resilience4JCircuitBreakerConfiguration
    breaker(String id, Duration timeout) {
        return new Resilience4JConfigBuilder(id)
                .circuitBreakerConfig(breakerConfig())
                .timeLimiterConfig(TimeLimiterConfig.custom().timeoutDuration(timeout).build())
                .build();
    }

    private static CircuitBreakerConfig breakerConfig() {
        return CircuitBreakerConfig.custom()
                .slidingWindowSize(20)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();
    }
}
