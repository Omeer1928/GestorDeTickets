package pe.techsupport.user.web;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import pe.techsupport.user.domain.Specialty;
import pe.techsupport.user.domain.SupportLevel;
import pe.techsupport.user.service.TechnicianService;
import pe.techsupport.user.web.dto.UserDtos.AvailabilityRequest;
import pe.techsupport.user.web.dto.UserDtos.AvailableTechnicians;
import pe.techsupport.user.web.dto.UserDtos.CreateTechnicianRequest;
import pe.techsupport.user.web.dto.UserDtos.SpecialtyResponse;
import pe.techsupport.user.web.dto.UserDtos.TechnicianCandidate;
import pe.techsupport.user.web.dto.UserDtos.TechnicianResponse;
import pe.techsupport.user.web.dto.UserDtos.WorkloadAdjustRequest;
import pe.techsupport.user.web.dto.UserDtos.WorkloadResponse;

@RestController
@RequestMapping("/users")
@Tag(name = "User Service - Técnicos", description = "Técnicos, especialidades, nivel de soporte y disponibilidad")
public class TechnicianController {

    private final TechnicianService service;

    public TechnicianController(TechnicianService service) {
        this.service = service;
    }

    @GetMapping("/technicians")
    @PreAuthorize("hasAnyRole('ADMIN','SYSTEM','TECNICO')")
    @Operation(summary = "Lista todos los técnicos registrados")
    public List<TechnicianResponse> list() {
        return service.list();
    }

    @GetMapping("/technicians/available")
    @PreAuthorize("hasAnyRole('ADMIN','SYSTEM','TECNICO')")
    @Operation(summary = "Técnicos disponibles con carga libre, filtrables por especialidad y nivel mínimo")
    public AvailableTechnicians available(@RequestParam(required = false) Specialty specialty,
                                          @RequestParam(required = false) SupportLevel level) {
        return service.available(specialty, level);
    }

    @GetMapping("/technicians/specialty/{specialty}")
    @PreAuthorize("hasAnyRole('ADMIN','SYSTEM','TECNICO')")
    @Operation(summary = "Busca técnicos por especialidad")
    public List<TechnicianCandidate> bySpecialty(@PathVariable Specialty specialty) {
        return service.bySpecialty(specialty);
    }

    @GetMapping("/technicians/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SYSTEM','TECNICO')")
    @Operation(summary = "Obtiene el perfil de un técnico")
    public TechnicianResponse get(@PathVariable String id) {
        return service.get(id);
    }

    @PostMapping("/technicians")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','SYSTEM')")
    @Operation(summary = "Registra un nuevo técnico con nivel (N1-N3) y especialidades")
    public TechnicianResponse create(@Valid @RequestBody CreateTechnicianRequest request) {
        return service.create(request);
    }

    @PatchMapping("/technicians/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','TECNICO')")
    @Operation(summary = "Actualiza la disponibilidad (isAvailable) de un técnico")
    public TechnicianResponse updateStatus(@PathVariable String id, @Valid @RequestBody AvailabilityRequest request) {
        return service.updateAvailability(id, request.isAvailable());
    }

    @GetMapping("/technicians/{id}/workload")
    @PreAuthorize("hasAnyRole('ADMIN','SYSTEM','TECNICO')")
    @Operation(summary = "Consulta la carga de trabajo de un técnico")
    public WorkloadResponse workload(@PathVariable String id) {
        return service.workload(id);
    }

    @PatchMapping("/technicians/{id}/workload")
    @PreAuthorize("hasRole('SYSTEM')")
    @Operation(summary = "Interno: reserva (+1) o libera (-1) un cupo de forma atómica")
    public WorkloadResponse adjustWorkload(@PathVariable String id, @Valid @RequestBody WorkloadAdjustRequest request) {
        return service.adjustWorkload(id, request.delta());
    }

    @DeleteMapping("/technicians/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('SYSTEM')")
    @Operation(summary = "Interno: compensación de la saga de registro")
    public void delete(@PathVariable String id) {
        service.delete(id);
    }

    @GetMapping("/specialties")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Catálogo de especialidades técnicas")
    public List<SpecialtyResponse> specialties() {
        return service.specialties();
    }
}
