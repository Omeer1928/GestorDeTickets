package pe.techsupport.common.error;

import java.time.Instant;
import java.util.stream.Collectors;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import pe.techsupport.common.resilience.InfraFailures;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiError> handleApi(ApiException ex, HttpServletRequest req) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(ex.getStatus());
        if (ex.getStatus() == HttpStatus.SERVICE_UNAVAILABLE) {
            builder.header(HttpHeaders.RETRY_AFTER, "10");
        }
        return builder.body(body(ex.getCode(), ex.getMessage(), ex.getStatus(), req));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(body("validation_error", msg, HttpStatus.BAD_REQUEST, req));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    ResponseEntity<ApiError> handleBadInput(Exception ex, HttpServletRequest req) {
        String msg = ex instanceof MethodArgumentTypeMismatchException m
                ? "Valor inválido para '" + m.getName() + "': " + m.getValue()
                : "Solicitud mal formada o incompleta";
        return ResponseEntity.badRequest().body(body("invalid_request", msg, HttpStatus.BAD_REQUEST, req));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> handleDenied(AccessDeniedException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(body("forbidden", "No tiene permisos para esta operación", HttpStatus.FORBIDDEN, req));
    }

    @ExceptionHandler(CallNotPermittedException.class)
    ResponseEntity<ApiError> handleCircuitOpen(CallNotPermittedException ex, HttpServletRequest req) {
        log.warn("Circuit breaker abierto: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(HttpHeaders.RETRY_AFTER, "10")
                .body(body("circuit_open",
                        "Dependencia temporalmente no disponible (circuit breaker abierto). Reintente en unos segundos.",
                        HttpStatus.SERVICE_UNAVAILABLE, req));
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ApiError> handleOptimistic(OptimisticLockingFailureException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body("concurrent_modification",
                "El recurso fue modificado por otra operación. Vuelva a intentarlo.", HttpStatus.CONFLICT, req));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> handleIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body("data_conflict",
                "Los datos entran en conflicto con un registro existente", HttpStatus.CONFLICT, req));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> handleNotFound(NoResourceFoundException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(body("not_found", "Recurso no encontrado", HttpStatus.NOT_FOUND, req));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest req) {
        if (ex instanceof ErrorResponse framework && framework.getStatusCode().is4xxClientError()) {
            HttpStatus status = HttpStatus.valueOf(framework.getStatusCode().value());
            return ResponseEntity.status(status).body(body("invalid_request", ex.getMessage(), status, req));
        }
        if (InfraFailures.isInfrastructureFailure(ex)) {
            log.warn("Dependencia no disponible en {}: {}", req.getRequestURI(), InfraFailures.rootMessage(ex));
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .header(HttpHeaders.RETRY_AFTER, "10")
                    .body(body("dependency_unavailable",
                            "La base de datos u otro servicio no está disponible. La operación no se realizó.",
                            HttpStatus.SERVICE_UNAVAILABLE, req));
        }
        log.error("Error no controlado en {}", req.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body("internal_error", "Error interno del servidor", HttpStatus.INTERNAL_SERVER_ERROR, req));
    }

    private static ApiError body(String code, String message, HttpStatus status, HttpServletRequest req) {
        return new ApiError(code, message, status.value(), req.getRequestURI(), Instant.now(), MDC.get("traceId"));
    }
}
