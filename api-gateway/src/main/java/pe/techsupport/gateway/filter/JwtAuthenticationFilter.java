package pe.techsupport.gateway.filter;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.micrometer.core.instrument.MeterRegistry;
import reactor.core.publisher.Mono;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import pe.techsupport.gateway.config.AccessRules;
import pe.techsupport.gateway.config.AccessRules.Decision;
import pe.techsupport.gateway.config.JwtProperties;

/**
 * Filtro de seguridad del gateway (sección 11 del documento):
 * <ol>
 *   <li>Elimina cabeceras de identidad que vengan del cliente (anti-suplantación).</li>
 *   <li>Valida localmente firma HMAC-SHA256, emisor y expiración del JWT (sin llamar a Auth en cada
 *       petición) y consulta en Redis si el token fue revocado por logout.</li>
 *   <li>Aplica RBAC por ruta y método; 401 sin token válido, 403 sin permisos.</li>
 *   <li>Propaga la identidad a los servicios internos con X-User-Id, X-User-Email y X-User-Role.</li>
 * </ol>
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    public static final String ATTR_USER_ID = "auth.userId";
    public static final String ATTR_ROLE = "auth.role";
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BLACKLIST_PREFIX = "auth:bl:";

    private final SecretKey key;
    private final String issuer;
    private final AccessRules rules;
    private final ReactiveStringRedisTemplate redis;
    private final MeterRegistry meters;

    public JwtAuthenticationFilter(JwtProperties props, AccessRules rules, ReactiveStringRedisTemplate redis,
                                   MeterRegistry meters) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(props.secret()));
        this.issuer = props.issuer();
        this.rules = rules;
        this.redis = redis;
        this.meters = meters;
    }

    @Override
    public int getOrder() {
        return -100; // antes del enrutamiento y de los filtros de ruta (rate limiter, circuit breaker)
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        Decision decision = rules.decide(request.getMethod(), path);
        boolean isPublic = decision == Decision.PUBLIC;

        if (decision == Decision.DENY) {
            return reject(exchange, HttpStatus.FORBIDDEN, "forbidden", "Operación interna no expuesta por el gateway");
        }

        ServerHttpRequest.Builder sanitized = request.mutate().headers(h -> {
            h.remove("X-User-Id");
            h.remove("X-User-Email");
            h.remove("X-User-Role");
            h.remove("X-Internal-Key");
        });

        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return isPublic ? chain.filter(exchange.mutate().request(sanitized.build()).build())
                    : reject(exchange, HttpStatus.UNAUTHORIZED, "missing_token",
                    "Se requiere la cabecera Authorization: Bearer <token>");
        }

        Claims claims;
        try {
            claims = Jwts.parser().verifyWith(key).requireIssuer(issuer).build()
                    .parseSignedClaims(authorization.substring(7).trim()).getPayload();
        } catch (ExpiredJwtException ex) {
            return isPublic ? chain.filter(exchange.mutate().request(sanitized.build()).build())
                    : reject(exchange, HttpStatus.UNAUTHORIZED, "token_expired", "El token expiró");
        } catch (JwtException | IllegalArgumentException ex) {
            return isPublic ? chain.filter(exchange.mutate().request(sanitized.build()).build())
                    : reject(exchange, HttpStatus.UNAUTHORIZED, "invalid_token", "Token inválido");
        }

        String userId = claims.get("userId", String.class);
        String role = claims.get("role", String.class);
        String email = claims.getSubject();

        return isRevoked(claims.getId()).flatMap(revoked -> {
            if (revoked) {
                return isPublic ? chain.filter(exchange.mutate().request(sanitized.build()).build())
                        : reject(exchange, HttpStatus.UNAUTHORIZED, "token_revoked", "El token fue revocado (logout)");
            }
            if (!isPublic && !rules.isAllowed(request.getMethod(), path, role)) {
                return reject(exchange, HttpStatus.FORBIDDEN, "forbidden",
                        "El rol " + role + " no tiene permisos para " + request.getMethod() + " " + path);
            }
            exchange.getAttributes().put(ATTR_USER_ID, userId);
            exchange.getAttributes().put(ATTR_ROLE, role);
            ServerHttpRequest authenticated = sanitized
                    .header("X-User-Id", userId)
                    .header("X-User-Role", role)
                    .header("X-User-Email", email)
                    .build();
            return chain.filter(exchange.mutate().request(authenticated).build());
        });
    }

    /** Lista negra en Redis. Si Redis no responde se permite el paso (fail-open) y se cuenta el evento. */
    private Mono<Boolean> isRevoked(String jti) {
        if (jti == null) {
            return Mono.just(false);
        }
        return redis.hasKey(BLACKLIST_PREFIX + jti)
                .timeout(Duration.ofMillis(300))
                .onErrorResume(ex -> {
                    meters.counter("gateway.blacklist.check.errors").increment();
                    log.warn("No se pudo consultar la lista negra en Redis: {}", ex.getMessage());
                    return Mono.just(false);
                })
                .defaultIfEmpty(false);
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String code, String message) {
        meters.counter("gateway.auth.rejections", "reason", code).increment();
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"error\":\"" + code + "\",\"message\":\"" + message.replace("\"", "'")
                + "\",\"status\":" + status.value() + ",\"path\":\"" + exchange.getRequest().getPath().value()
                + "\",\"timestamp\":\"" + Instant.now() + "\"}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
