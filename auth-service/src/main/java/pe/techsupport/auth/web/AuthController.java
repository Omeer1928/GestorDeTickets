package pe.techsupport.auth.web;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import pe.techsupport.auth.service.AuthService;
import pe.techsupport.auth.web.dto.AuthDtos.LoginRequest;
import pe.techsupport.auth.web.dto.AuthDtos.LoginResponse;
import pe.techsupport.auth.web.dto.AuthDtos.LogoutRequest;
import pe.techsupport.auth.web.dto.AuthDtos.MessageResponse;
import pe.techsupport.auth.web.dto.AuthDtos.RefreshRequest;
import pe.techsupport.auth.web.dto.AuthDtos.RegisterRequest;
import pe.techsupport.auth.web.dto.AuthDtos.RegisterResponse;
import pe.techsupport.auth.web.dto.AuthDtos.RoleResponse;
import pe.techsupport.auth.web.dto.AuthDtos.ValidateResponse;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication Service", description = "Autenticación, emisión/validación de JWT y control de acceso (RBAC)")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/login")
    @Operation(summary = "Autentica un usuario y genera access token (JWT) + refresh token")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return service.login(request);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renueva el access token usando el refresh token (rotación con detección de reutilización)")
    public LoginResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return service.refresh(request.refreshToken());
    }

    @PostMapping("/validate")
    @Operation(summary = "Valida la firma, vigencia y revocación de un token JWT")
    public ValidateResponse validate(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String bearer) {
        return service.validate(bearer);
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoca el token JWT (lista negra en Redis) y los refresh tokens de la sesión")
    public MessageResponse logout(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String bearer,
                                  @RequestBody(required = false) LogoutRequest body) {
        return service.logout(bearer, body == null ? null : body.refreshToken());
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra un usuario. CLIENTE: público. TECNICO: solo ADMIN")
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return service.register(request);
    }

    @GetMapping("/roles")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lista los roles disponibles en el sistema")
    public List<RoleResponse> roles() {
        return service.roles();
    }
}
