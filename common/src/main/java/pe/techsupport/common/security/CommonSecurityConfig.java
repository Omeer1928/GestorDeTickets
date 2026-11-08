package pe.techsupport.common.security;

import java.time.Instant;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import pe.techsupport.common.config.CommonProperties;
import pe.techsupport.common.error.ApiError;

/**
 * Cadena de seguridad común: sin sesión, sin CSRF (API REST con tokens), identidad tomada de las
 * cabeceras del gateway y autorización fina con @PreAuthorize en cada controlador.
 */
@Configuration
@EnableMethodSecurity
public class CommonSecurityConfig {

    private static final String[] DOCS = {"/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
            "/auth/v3/api-docs/**", "/users/v3/api-docs/**", "/tickets/v3/api-docs/**"};

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, CommonProperties props, ObjectMapper mapper)
            throws Exception {
        String[] publicPaths = props.getSecurity().getPublicPaths().toArray(String[]::new);
        http
            .csrf(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(new GatewayHeaderAuthenticationFilter(props.getInternalKey()),
                    UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(EndpointRequest.toAnyEndpoint()).permitAll()
                .requestMatchers(DOCS).permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers(publicPaths.length == 0 ? new String[]{"/__none__"} : publicPaths).permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) ->
                        write(res, mapper, 401, "unauthorized", "Se requiere un token válido", req.getRequestURI()))
                .accessDeniedHandler((req, res, e) ->
                        write(res, mapper, 403, "forbidden", "No tiene permisos para este recurso", req.getRequestURI())));
        return http.build();
    }

    private static void write(HttpServletResponse res, ObjectMapper mapper, int status, String code, String message,
                              String path) throws java.io.IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        mapper.writeValue(res.getOutputStream(), new ApiError(code, message, status, path, Instant.now(), null));
    }
}
