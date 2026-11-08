package pe.techsupport.user.web;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import pe.techsupport.user.service.ClientService;
import pe.techsupport.user.web.dto.UserDtos.ClientResponse;
import pe.techsupport.user.web.dto.UserDtos.ClientSummary;
import pe.techsupport.user.web.dto.UserDtos.ClientValidation;
import pe.techsupport.user.web.dto.UserDtos.CreateClientRequest;
import pe.techsupport.user.web.dto.UserDtos.UpdateClientRequest;

@RestController
@RequestMapping("/users/clients")
@Tag(name = "User Service - Clientes", description = "Gestión de clientes con contrato de soporte")
public class ClientController {

    private final ClientService service;

    public ClientController(ClientService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SYSTEM')")
    @Operation(summary = "Lista todos los clientes registrados")
    public List<ClientSummary> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SYSTEM','CLIENTE','TECNICO')")
    @Operation(summary = "Obtiene los datos de un cliente por su ID (un CLIENTE solo puede ver su propio perfil)")
    public ClientResponse get(@PathVariable String id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','SYSTEM')")
    @Operation(summary = "Registra un nuevo cliente (invocado internamente por Auth en /auth/register)")
    public ClientResponse create(@Valid @RequestBody CreateClientRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CLIENTE')")
    @Operation(summary = "Actualiza los datos de un cliente (el estado solo lo cambia un ADMIN)")
    public ClientResponse update(@PathVariable String id, @Valid @RequestBody UpdateClientRequest request) {
        return service.update(id, request);
    }

    @GetMapping("/{id}/validate")
    @PreAuthorize("hasAnyRole('ADMIN','SYSTEM')")
    @Operation(summary = "Valida si un cliente existe y está activo (usado por Ticket Service)")
    public ClientValidation validate(@PathVariable String id) {
        return service.validate(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('SYSTEM')")
    @Operation(summary = "Interno: compensación de la saga de registro")
    public void delete(@PathVariable String id) {
        service.delete(id);
    }
}
