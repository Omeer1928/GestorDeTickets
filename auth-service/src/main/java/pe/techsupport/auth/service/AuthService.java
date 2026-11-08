package pe.techsupport.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.micrometer.core.instrument.MeterRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.techsupport.auth.client.UserServiceClient;
import pe.techsupport.auth.config.AuthProperties;
import pe.techsupport.auth.config.JwtProperties;
import pe.techsupport.auth.domain.Credential;
import pe.techsupport.auth.domain.RefreshToken;
import pe.techsupport.auth.repository.CredentialRepository;
import pe.techsupport.auth.repository.RefreshTokenRepository;
import pe.techsupport.auth.repository.RoleCatalogRepository;
import pe.techsupport.auth.web.dto.AuthDtos.LoginRequest;
import pe.techsupport.auth.web.dto.AuthDtos.LoginResponse;
import pe.techsupport.auth.web.dto.AuthDtos.MessageResponse;
import pe.techsupport.auth.web.dto.AuthDtos.RegisterRequest;
import pe.techsupport.auth.web.dto.AuthDtos.RegisterResponse;
import pe.techsupport.auth.web.dto.AuthDtos.RoleResponse;
import pe.techsupport.auth.web.dto.AuthDtos.ValidateResponse;
import pe.techsupport.common.audit.AuditLogger;
import pe.techsupport.common.error.ApiException;
import pe.techsupport.common.resilience.InfraFailures;
import pe.techsupport.common.resilience.ResilientCache;
import pe.techsupport.common.security.AuthUser;
import pe.techsupport.common.security.SecurityUtils;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String CRED_CACHE = "credentials";
    /** Hash ficticio para igualar el tiempo de respuesta cuando el correo no existe (evita enumeración). */
    private static final String DUMMY_HASH = "$2a$10$ozTgM8IhjGdKzKsVbfp8wOOy/FwEhu2ZzJilnDoHPiRXYNHO/iteO";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CredentialRepository credentials;
    private final RefreshTokenRepository refreshTokens;
    private final RoleCatalogRepository roles;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final JwtProperties jwtProps;
    private final AuthProperties authProps;
    private final LoginAttemptService attempts;
    private final TokenBlacklist blacklist;
    private final ResilientCache cache;
    private final UserServiceClient userService;
    private final AuditLogger audit;
    private final MeterRegistry meters;

    public AuthService(CredentialRepository credentials, RefreshTokenRepository refreshTokens,
                       RoleCatalogRepository roles, PasswordEncoder encoder, JwtService jwt, JwtProperties jwtProps,
                       AuthProperties authProps, LoginAttemptService attempts, TokenBlacklist blacklist,
                       ResilientCache cache, UserServiceClient userService, AuditLogger audit, MeterRegistry meters) {
        this.credentials = credentials;
        this.refreshTokens = refreshTokens;
        this.roles = roles;
        this.encoder = encoder;
        this.jwt = jwt;
        this.jwtProps = jwtProps;
        this.authProps = authProps;
        this.attempts = attempts;
        this.blacklist = blacklist;
        this.cache = cache;
        this.userService = userService;
        this.audit = audit;
        this.meters = meters;
    }

    // ---------------------------------------------------------------- login

    public LoginResponse login(LoginRequest req) {
        String email = normalize(req.email());
        AuthUser anonymous = new AuthUser("anonymous", email, "NONE");

        if (attempts.isLocked(email)) {
            countLogin("locked");
            audit.log("LOGIN", email, "FAILURE", "Cuenta bloqueada por intentos fallidos", anonymous);
            throw new ApiException(HttpStatus.LOCKED, "account_locked",
                    "Cuenta bloqueada temporalmente por intentos fallidos. Intente nuevamente en "
                            + authProps.getLockDuration().toMinutes() + " minutos.");
        }

        CredentialSnapshot cred = findCredential(email);
        boolean matches = encoder.matches(req.password(), cred != null ? cred.passwordHash() : DUMMY_HASH);
        if (cred == null || !matches) {
            int remaining = attempts.registerFailure(email);
            countLogin("failure");
            audit.log("LOGIN", email, "FAILURE", "Credenciales inválidas, intentos restantes: " + remaining, anonymous);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "invalid_credentials",
                    "El correo o la contraseña son incorrectos.");
        }
        if (!cred.isActive()) {
            countLogin("disabled");
            audit.log("LOGIN", email, "FAILURE", "Cuenta en estado " + cred.status(), anonymous);
            throw new ApiException(HttpStatus.FORBIDDEN, "account_disabled", "La cuenta no está activa.");
        }

        attempts.reset(email);
        JwtService.IssuedToken access = jwt.issueAccessToken(cred);
        String refresh = null;
        try {
            refresh = persistNewRefreshToken(cred, UUID.randomUUID().toString());
        } catch (RuntimeException ex) {
            if (!InfraFailures.isInfrastructureFailure(ex)) {
                throw ex;
            }
            // Degradación controlada: sin BD no se puede guardar la sesión larga, pero el acceso sigue.
            log.warn("BD de auth no disponible: login de {} sin refresh token ({})", email,
                    InfraFailures.rootMessage(ex));
            meters.counter("auth.degraded.logins").increment();
        }
        countLogin("success");
        audit.log("LOGIN", cred.userId(), "SUCCESS", refresh == null ? "Login sin refresh token (modo degradado)" : "Login correcto",
                new AuthUser(cred.userId(), cred.email(), cred.role()));
        return new LoginResponse(cred.userId(), cred.fullName(), cred.role(), access.token(), refresh, "Bearer",
                access.expiresInSeconds(), refresh == null ? null : jwtProps.getRefreshTtl().toSeconds());
    }

    // ---------------------------------------------------------------- refresh (rotación)

    @CircuitBreaker(name = "db")
    @Transactional(noRollbackFor = ApiException.class)
    public LoginResponse refresh(String rawRefreshToken) {
        RefreshToken current = refreshTokens.findByTokenHash(sha256(rawRefreshToken))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "invalid_refresh_token",
                        "Refresh token inválido"));
        if (current.isRevoked()) {
            // Reutilización de un token ya rotado: posible robo → se revoca toda la familia.
            refreshTokens.revokeFamily(current.getFamilyId(), Instant.now());
            audit.log("TOKEN_REFRESH", current.getUserId(), "FAILURE", "Reutilización de refresh token detectada",
                    new AuthUser(current.getUserId(), current.getEmail(), "NONE"));
            meters.counter("auth.refresh", "result", "reused").increment();
            throw new ApiException(HttpStatus.UNAUTHORIZED, "refresh_token_reused",
                    "El refresh token ya fue utilizado. Inicie sesión nuevamente.");
        }
        if (current.isExpired()) {
            meters.counter("auth.refresh", "result", "expired").increment();
            throw new ApiException(HttpStatus.UNAUTHORIZED, "refresh_token_expired",
                    "El refresh token expiró. Inicie sesión nuevamente.");
        }
        CredentialSnapshot cred = findCredential(current.getEmail());
        if (cred == null || !cred.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "account_disabled", "La cuenta no está activa.");
        }
        current.revoke();
        String newRefresh = persistNewRefreshToken(cred, current.getFamilyId());
        JwtService.IssuedToken access = jwt.issueAccessToken(cred);
        meters.counter("auth.refresh", "result", "success").increment();
        audit.log("TOKEN_REFRESH", cred.userId(), "SUCCESS", "Access token renovado",
                new AuthUser(cred.userId(), cred.email(), cred.role()));
        return new LoginResponse(cred.userId(), cred.fullName(), cred.role(), access.token(), newRefresh, "Bearer",
                access.expiresInSeconds(), jwtProps.getRefreshTtl().toSeconds());
    }

    // ---------------------------------------------------------------- validate / logout

    public ValidateResponse validate(String bearer) {
        Claims claims = parseBearer(bearer);
        if (blacklist.isRevoked(claims.getId())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "token_revoked", "El token fue revocado (logout).");
        }
        return new ValidateResponse(claims.get("userId", String.class), claims.getSubject(),
                claims.get("role", String.class), true, claims.getExpiration().toInstant());
    }

    public MessageResponse logout(String bearer, String rawRefreshToken) {
        Claims claims;
        try {
            claims = parseBearer(bearer);
        } catch (ApiException ex) {
            if (ex.getCode().equals("token_expired")) {
                return new MessageResponse("Se cerró sesión correctamente");
            }
            throw ex;
        }
        String userId = claims.get("userId", String.class);
        Duration remaining = Duration.between(Instant.now(), claims.getExpiration().toInstant());
        blacklist.revoke(claims.getId(), remaining);
        try {
            if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
                refreshTokens.findByTokenHash(sha256(rawRefreshToken))
                        .ifPresent(rt -> refreshTokens.revokeFamily(rt.getFamilyId(), Instant.now()));
            } else {
                refreshTokens.revokeAllForUser(userId, Instant.now());
            }
        } catch (RuntimeException ex) {
            if (!InfraFailures.isInfrastructureFailure(ex)) {
                throw ex;
            }
            log.warn("No se pudieron revocar los refresh tokens de {} (BD no disponible)", userId);
        }
        meters.counter("auth.logout").increment();
        audit.log("LOGOUT", userId, "SUCCESS", "Token revocado",
                new AuthUser(userId, claims.getSubject(), claims.get("role", String.class)));
        return new MessageResponse("Se cerró sesión correctamente");
    }

    // ---------------------------------------------------------------- register (saga)

    public RegisterResponse register(RegisterRequest req) {
        AuthUser caller = SecurityUtils.currentUserOptional().orElse(null);
        if ("TECNICO".equals(req.userType()) && (caller == null || !caller.isAdmin())) {
            throw ApiException.forbidden("Solo un ADMIN puede registrar técnicos");
        }
        String email = normalize(req.email());
        if (credentials.existsByEmail(email)) {
            throw ApiException.conflict("email_already_registered", "El correo ya se encuentra registrado");
        }
        String userId = userService.createProfile(req);
        try {
            String fullName = req.firstName().trim() + " " + req.lastName().trim();
            credentials.save(new Credential(userId, email, encoder.encode(req.password()), req.userType(), fullName));
        } catch (RuntimeException ex) {
            userService.deleteProfile(req.userType(), userId);
            audit.failure("REGISTER", userId, "Falló el guardado de credencial; compensación ejecutada");
            throw ex;
        }
        meters.counter("auth.register", "userType", req.userType()).increment();
        audit.log("REGISTER", userId, "SUCCESS", "Usuario " + req.userType() + " registrado",
                caller != null ? caller : new AuthUser(userId, email, req.userType()));
        return new RegisterResponse("Usuario registrado correctamente", userId, req.userType());
    }

    public List<RoleResponse> roles() {
        return cache.get("roles", "all", Duration.ofHours(1), new TypeReference<List<RoleResponse>>() { },
                () -> roles.findAll().stream().map(r -> new RoleResponse(r.getCode(), r.getDescription())).toList());
    }

    // ---------------------------------------------------------------- helpers

    private CredentialSnapshot findCredential(String email) {
        return cache.get(CRED_CACHE, email, authProps.getCredentialCacheTtl(), CredentialSnapshot.class,
                () -> credentials.findByEmail(email).map(CredentialSnapshot::from).orElse(null));
    }

    private String persistNewRefreshToken(CredentialSnapshot cred, String familyId) {
        byte[] bytes = new byte[48];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        refreshTokens.save(new RefreshToken(sha256(raw), cred.userId(), cred.email(), familyId,
                Instant.now().plus(jwtProps.getRefreshTtl())));
        return raw;
    }

    private Claims parseBearer(String bearer) {
        if (bearer == null || !bearer.startsWith("Bearer ")) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "missing_token",
                    "Se requiere la cabecera Authorization: Bearer <token>");
        }
        try {
            return jwt.parse(bearer.substring(7).trim());
        } catch (ExpiredJwtException ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "token_expired", "El token expiró");
        } catch (JwtException | IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "invalid_token", "Token inválido");
        }
    }

    private void countLogin(String result) {
        meters.counter("auth.login", "result", result).increment();
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    static String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
