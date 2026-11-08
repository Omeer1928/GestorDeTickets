package pe.techsupport.common.error;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Formato único de error de todos los servicios (contrato "Salida esperada en caso de error":
 * código de error + mensaje), enriquecido con estado HTTP, ruta, fecha y traceId para auditoría.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(String error, String message, int status, String path, Instant timestamp, String traceId) {
}
