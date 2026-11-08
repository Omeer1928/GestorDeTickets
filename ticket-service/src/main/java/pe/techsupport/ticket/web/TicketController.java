package pe.techsupport.ticket.web;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import pe.techsupport.ticket.domain.TicketStatus;
import pe.techsupport.ticket.service.TicketCommandService;
import pe.techsupport.ticket.service.TicketQueryService;
import pe.techsupport.ticket.web.dto.TicketDtos.AssignRequest;
import pe.techsupport.ticket.web.dto.TicketDtos.CloseRequest;
import pe.techsupport.ticket.web.dto.TicketDtos.CreateTicketRequest;
import pe.techsupport.ticket.web.dto.TicketDtos.EscalateRequest;
import pe.techsupport.ticket.web.dto.TicketDtos.PriorityRequest;
import pe.techsupport.ticket.web.dto.TicketDtos.ReassignRequest;
import pe.techsupport.ticket.web.dto.TicketDtos.ResolveRequest;
import pe.techsupport.ticket.web.dto.TicketDtos.TicketEventResponse;
import pe.techsupport.ticket.web.dto.TicketDtos.TicketResponse;
import pe.techsupport.ticket.web.dto.TicketDtos.TicketSummary;

@RestController
@RequestMapping("/tickets")
@Tag(name = "Ticket Service", description = "Orquestador del ciclo de vida de las incidencias")
public class TicketController {

    private final TicketCommandService commands;
    private final TicketQueryService queries;

    public TicketController(TicketCommandService commands, TicketQueryService queries) {
        this.commands = commands;
        this.queries = queries;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CLIENTE','ADMIN')")
    @Operation(summary = "Registra una incidencia: valida al cliente, evalúa criticidad y asigna si es urgente")
    public TicketResponse create(@Valid @RequestBody CreateTicketRequest request) {
        return commands.create(request);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lista los últimos 200 tickets (supervisión)")
    public List<TicketSummary> latest() {
        return queries.latest();
    }

    @GetMapping("/queue")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tickets en espera de técnico, en orden de despacho")
    public List<TicketSummary> queue() {
        return queries.queue();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtiene el detalle completo de un ticket")
    public TicketResponse get(@PathVariable String id) {
        return queries.get(id);
    }

    @GetMapping("/{id}/events")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Bitácora (trazabilidad) de un ticket")
    public List<TicketEventResponse> events(@PathVariable String id) {
        return queries.events(id);
    }

    @GetMapping("/client/{clientId}")
    @PreAuthorize("hasAnyRole('CLIENTE','ADMIN')")
    @Operation(summary = "Lista los tickets de un cliente")
    public List<TicketSummary> byClient(@PathVariable String clientId) {
        return queries.byClient(clientId);
    }

    @GetMapping("/technician/{technicianId}")
    @PreAuthorize("hasAnyRole('TECNICO','ADMIN')")
    @Operation(summary = "Lista los tickets asignados a un técnico")
    public List<TicketSummary> byTechnician(@PathVariable String technicianId) {
        return queries.byTechnician(technicianId);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lista tickets por estado (filtrado por propiedad para CLIENTE/TECNICO)")
    public List<TicketSummary> byStatus(@PathVariable TicketStatus status) {
        return queries.byStatus(status);
    }

    @PatchMapping("/{id}/priority")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Evalúa/actualiza manualmente la prioridad del ticket")
    public TicketResponse priority(@PathVariable String id, @Valid @RequestBody PriorityRequest request) {
        return commands.updatePriority(id, request);
    }

    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Asigna un técnico (manual si se indica technicianId, automática si no)")
    public TicketResponse assign(@PathVariable String id, @Valid @RequestBody(required = false) AssignRequest request) {
        return commands.assign(id, request);
    }

    @PatchMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('TECNICO','ADMIN')")
    @Operation(summary = "El técnico inicia el diagnóstico (EN_PROCESO)")
    public TicketResponse start(@PathVariable String id) {
        return commands.start(id);
    }

    @PatchMapping("/{id}/escalate")
    @PreAuthorize("hasAnyRole('TECNICO','ADMIN')")
    @Operation(summary = "Escala el ticket a un nivel superior o a otra especialidad")
    public TicketResponse escalate(@PathVariable String id, @Valid @RequestBody EscalateRequest request) {
        return commands.escalate(id, request);
    }

    @PatchMapping("/{id}/reassign")
    @PreAuthorize("hasAnyRole('TECNICO','ADMIN')")
    @Operation(summary = "Reasigna el ticket a un nuevo técnico preservando la bitácora")
    public TicketResponse reassign(@PathVariable String id, @Valid @RequestBody ReassignRequest request) {
        return commands.reassign(id, request);
    }

    @PatchMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('TECNICO','ADMIN')")
    @Operation(summary = "Resuelve el ticket y registra la solución aplicada")
    public TicketResponse resolve(@PathVariable String id, @Valid @RequestBody ResolveRequest request) {
        return commands.resolve(id, request);
    }

    @PatchMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('CLIENTE','ADMIN')")
    @Operation(summary = "Cierra formalmente el ticket (confirmación del cliente o de la mesa de ayuda)")
    public TicketResponse close(@PathVariable String id, @RequestBody(required = false) CloseRequest request) {
        return commands.close(id, request);
    }
}
