package pe.techsupport.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * Registro de cada petición (método, ruta, estado, duración y usuario) → logger "ACCESS".
 * Cumple la función "Monitoreo y trazabilidad" del API Gateway como ESB.
 */
@Component
public class AccessLogFilter implements GlobalFilter, Ordered {

    private static final Logger ACCESS = LoggerFactory.getLogger("ACCESS");

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long start = System.nanoTime();
        return chain.filter(exchange).doFinally(signal -> {
            long ms = (System.nanoTime() - start) / 1_000_000;
            Object user = exchange.getAttribute(JwtAuthenticationFilter.ATTR_USER_ID);
            Object role = exchange.getAttribute(JwtAuthenticationFilter.ATTR_ROLE);
            var status = exchange.getResponse().getStatusCode();
            try {
                MDC.put("http_method", String.valueOf(exchange.getRequest().getMethod()));
                MDC.put("http_path", exchange.getRequest().getPath().value());
                MDC.put("http_status", status == null ? "-" : String.valueOf(status.value()));
                MDC.put("duration_ms", String.valueOf(ms));
                MDC.put("user_id", user == null ? "anonymous" : user.toString());
                MDC.put("user_role", role == null ? "NONE" : role.toString());
                ACCESS.info("{} {} -> {} ({} ms) user={}", exchange.getRequest().getMethod(),
                        exchange.getRequest().getPath().value(), status == null ? "-" : status.value(), ms,
                        user == null ? "anonymous" : user);
            } finally {
                MDC.remove("http_method");
                MDC.remove("http_path");
                MDC.remove("http_status");
                MDC.remove("duration_ms");
                MDC.remove("user_id");
                MDC.remove("user_role");
            }
        });
    }
}
