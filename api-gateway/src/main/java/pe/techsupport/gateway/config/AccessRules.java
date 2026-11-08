package pe.techsupport.gateway.config;

import java.util.List;
import java.util.Set;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Autorización gruesa basada en roles (RBAC) aplicada en el borde. La autorización fina (propiedad
 * del recurso: "un CLIENTE solo ve sus tickets") la aplica cada servicio con Spring Security.
 * La primera regla que coincide decide.
 */
@Component
public class AccessRules {

    public enum Decision { PUBLIC, DENY, AUTHENTICATED, ROLES }

    private record Rule(HttpMethod method, PathPattern pattern, Decision decision, Set<String> roles) {
        boolean matches(HttpMethod m, PathContainer path) {
            return (method == null || method.equals(m)) && pattern.matches(path);
        }
    }

    private static final Set<String> ALL = Set.of("ADMIN", "CLIENTE", "TECNICO");
    private final List<Rule> rules;

    public AccessRules() {
        PathPatternParser p = PathPatternParser.defaultInstance;
        rules = List.of(
                // Público
                new Rule(HttpMethod.POST, p.parse("/auth/login"), Decision.PUBLIC, Set.of()),
                new Rule(HttpMethod.POST, p.parse("/auth/refresh"), Decision.PUBLIC, Set.of()),
                new Rule(HttpMethod.POST, p.parse("/auth/register"), Decision.PUBLIC, Set.of()),
                new Rule(HttpMethod.POST, p.parse("/auth/validate"), Decision.PUBLIC, Set.of()),
                new Rule(HttpMethod.POST, p.parse("/auth/logout"), Decision.PUBLIC, Set.of()),
                new Rule(null, p.parse("/swagger-ui.html"), Decision.PUBLIC, Set.of()),
                new Rule(null, p.parse("/swagger-ui/**"), Decision.PUBLIC, Set.of()),
                new Rule(null, p.parse("/webjars/**"), Decision.PUBLIC, Set.of()),
                new Rule(null, p.parse("/v3/api-docs/**"), Decision.PUBLIC, Set.of()),
                new Rule(HttpMethod.GET, p.parse("/auth/v3/api-docs/**"), Decision.PUBLIC, Set.of()),
                new Rule(HttpMethod.GET, p.parse("/users/v3/api-docs/**"), Decision.PUBLIC, Set.of()),
                new Rule(HttpMethod.GET, p.parse("/tickets/v3/api-docs/**"), Decision.PUBLIC, Set.of()),
                new Rule(null, p.parse("/fallback/**"), Decision.PUBLIC, Set.of()),
                // Solo interno (servicio a servicio): nunca expuesto por el gateway
                new Rule(HttpMethod.PATCH, p.parse("/users/technicians/{id}/workload"), Decision.DENY, Set.of()),
                new Rule(HttpMethod.DELETE, p.parse("/users/**"), Decision.DENY, Set.of()),
                // User Service
                new Rule(HttpMethod.GET, p.parse("/users/clients/{id}/validate"), Decision.ROLES, Set.of("ADMIN")),
                new Rule(null, p.parse("/users/clients"), Decision.ROLES, Set.of("ADMIN")),
                new Rule(null, p.parse("/users/clients/**"), Decision.ROLES, ALL),
                new Rule(null, p.parse("/users/technicians/**"), Decision.ROLES, Set.of("ADMIN", "TECNICO")),
                new Rule(null, p.parse("/users/technicians"), Decision.ROLES, Set.of("ADMIN", "TECNICO")),
                // Ticket Service
                new Rule(HttpMethod.GET, p.parse("/tickets"), Decision.ROLES, Set.of("ADMIN")),
                new Rule(HttpMethod.GET, p.parse("/tickets/queue"), Decision.ROLES, Set.of("ADMIN")),
                new Rule(HttpMethod.PATCH, p.parse("/tickets/{id}/priority"), Decision.ROLES, Set.of("ADMIN")),
                new Rule(HttpMethod.PATCH, p.parse("/tickets/{id}/assign"), Decision.ROLES, Set.of("ADMIN")),
                new Rule(null, p.parse("/tickets/**"), Decision.ROLES, ALL));
    }

    public Decision decide(HttpMethod method, String path) {
        PathContainer container = PathContainer.parsePath(path);
        return rules.stream().filter(r -> r.matches(method, container)).findFirst()
                .map(Rule::decision).orElse(Decision.AUTHENTICATED);
    }

    public boolean isAllowed(HttpMethod method, String path, String role) {
        PathContainer container = PathContainer.parsePath(path);
        return rules.stream().filter(r -> r.matches(method, container)).findFirst()
                .map(r -> switch (r.decision()) {
                    case PUBLIC, AUTHENTICATED -> true;
                    case DENY -> false;
                    case ROLES -> r.roles().contains(role);
                })
                .orElse(true);
    }
}
