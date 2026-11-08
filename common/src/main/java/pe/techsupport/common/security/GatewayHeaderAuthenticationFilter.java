package pe.techsupport.common.security;

import java.io.IOException;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Convierte las cabeceras de identidad que inyecta el API Gateway (tras validar el JWT) en una
 * Authentication de Spring Security. El gateway elimina estas cabeceras si vienen del cliente,
 * por lo que no se pueden falsificar desde fuera. El rol SYSTEM además exige la clave interna.
 */
public class GatewayHeaderAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(GatewayHeaderAuthenticationFilter.class);
    private static final Set<String> VALID_ROLES = Set.of(Roles.ADMIN, Roles.CLIENTE, Roles.TECNICO, Roles.SYSTEM);

    private final byte[] internalKey;

    public GatewayHeaderAuthenticationFilter(String internalKey) {
        this.internalKey = internalKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String userId = request.getHeader(Roles.HEADER_USER_ID);
        String role = request.getHeader(Roles.HEADER_USER_ROLE);
        String email = request.getHeader(Roles.HEADER_USER_EMAIL);

        if (StringUtils.hasText(userId) && StringUtils.hasText(role) && VALID_ROLES.contains(role)) {
            boolean trusted = true;
            if (Roles.SYSTEM.equals(role)) {
                String key = request.getHeader(Roles.HEADER_INTERNAL_KEY);
                trusted = key != null && MessageDigest.isEqual(internalKey, key.getBytes(StandardCharsets.UTF_8));
                if (!trusted) {
                    log.warn("Llamada con rol SYSTEM rechazada: clave interna inválida desde {}", request.getRemoteAddr());
                }
            }
            if (trusted) {
                AuthUser principal = new AuthUser(userId, email, role);
                var auth = new UsernamePasswordAuthenticationToken(principal, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        chain.doFilter(request, response);
    }
}
