package pe.techsupport.user.service;

import java.util.List;
import java.util.Locale;

import com.fasterxml.jackson.core.type.TypeReference;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.techsupport.common.audit.AuditLogger;
import pe.techsupport.common.error.ApiException;
import pe.techsupport.common.resilience.ResilientCache;
import pe.techsupport.common.security.AuthUser;
import pe.techsupport.common.security.SecurityUtils;
import pe.techsupport.user.config.CacheTtlProperties;
import pe.techsupport.user.domain.Client;
import pe.techsupport.user.domain.ClientStatus;
import pe.techsupport.user.repository.ClientRepository;
import pe.techsupport.user.web.dto.UserDtos.ClientResponse;
import pe.techsupport.user.web.dto.UserDtos.ClientSummary;
import pe.techsupport.user.web.dto.UserDtos.ClientValidation;
import pe.techsupport.user.web.dto.UserDtos.CreateClientRequest;
import pe.techsupport.user.web.dto.UserDtos.UpdateClientRequest;

@Service
public class ClientService {

    static final String CACHE = "client";
    static final String CACHE_LIST = "client-list";
    static final String CACHE_VALID = "client-valid";

    private final ClientRepository clients;
    private final ResilientCache cache;
    private final CacheTtlProperties ttl;
    private final AuditLogger audit;

    public ClientService(ClientRepository clients, ResilientCache cache, CacheTtlProperties ttl, AuditLogger audit) {
        this.clients = clients;
        this.cache = cache;
        this.ttl = ttl;
        this.audit = audit;
    }

    public List<ClientSummary> list() {
        return cache.get(CACHE_LIST, "all", ttl.getList(), new TypeReference<List<ClientSummary>>() { },
                () -> clients.findAllByOrderBySeqAsc().stream()
                        .map(c -> new ClientSummary(c.getUserId(), c.getFirstName(), c.getLastName(),
                                c.getCompanyName(), c.getStatus()))
                        .toList());
    }

    public ClientResponse get(String userId) {
        SecurityUtils.currentUserOptional()
                .filter(AuthUser::isClient)
                .ifPresent(u -> SecurityUtils.requireSelfOrPrivileged(userId));
        return cache.get(CACHE, userId, ttl.getProfile(), ClientResponse.class,
                () -> toResponse(find(userId)));
    }

    /** Usado por el Ticket Service antes de abrir un ticket (proceso 4.1). */
    public ClientValidation validate(String userId) {
        return cache.get(CACHE_VALID, userId, ttl.getProfile(), ClientValidation.class, () -> {
            Client c = find(userId);
            return new ClientValidation(c.getStatus() == ClientStatus.ACTIVO, c.getUserId(), c.getStatus(),
                    c.getCompanyName());
        });
    }

    @CircuitBreaker(name = "db")
    @Transactional
    public ClientResponse create(CreateClientRequest req) {
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        if (clients.existsByEmail(email)) {
            throw ApiException.conflict("email_already_registered", "Ya existe un cliente con el correo " + email);
        }
        Client client = clients.save(new Client(req.firstName().trim(), req.lastName().trim(), email, req.phone(),
                req.companyName(), emptyToNull(req.ruc())));
        client.assignUserId();
        ClientResponse response = toResponse(client);
        cache.afterCommit(() -> cache.evictAll(CACHE_LIST));
        audit.success("CLIENT_CREATED", client.getUserId(), "Cliente registrado: " + client.getCompanyName());
        return response;
    }

    @CircuitBreaker(name = "db")
    @Transactional
    public ClientResponse update(String userId, UpdateClientRequest req) {
        SecurityUtils.requireSelfOrPrivileged(userId);
        AuthUser user = SecurityUtils.currentUser();
        Client client = find(userId);
        client.updateContact(req.firstName(), req.lastName(), req.phone(), req.companyName(), emptyToNull(req.ruc()));
        if (req.status() != null && req.status() != client.getStatus()) {
            if (!user.isAdmin()) {
                throw ApiException.forbidden("Solo un ADMIN puede cambiar el estado de la cuenta");
            }
            audit.success("CLIENT_STATUS_CHANGED", userId, client.getStatus() + " -> " + req.status());
            client.changeStatus(req.status());
        }
        clients.flush();
        ClientResponse response = toResponse(client);
        cache.afterCommit(() -> {
            cache.put(CACHE, userId, response, ttl.getProfile());
            cache.evict(CACHE_VALID, userId);
            cache.evictAll(CACHE_LIST);
        });
        audit.success("CLIENT_UPDATED", userId, "Datos del cliente actualizados");
        return response;
    }

    /** Compensación de la saga de registro (solo SYSTEM). */
    @CircuitBreaker(name = "db")
    @Transactional
    public void delete(String userId) {
        clients.findByUserId(userId).ifPresent(clients::delete);
        cache.afterCommit(() -> {
            cache.evict(CACHE, userId);
            cache.evict(CACHE_VALID, userId);
            cache.evictAll(CACHE_LIST);
        });
        audit.success("CLIENT_DELETED", userId, "Perfil eliminado por compensación");
    }

    private Client find(String userId) {
        return clients.findByUserId(userId)
                .orElseThrow(() -> ApiException.notFound("client_not_found", "No existe el cliente " + userId));
    }

    private static ClientResponse toResponse(Client c) {
        return new ClientResponse(c.getUserId(), c.getFirstName(), c.getLastName(), c.getEmail(), c.getPhone(),
                c.getCompanyName(), c.getRuc(), "CLIENTE", c.getStatus(), c.getCreatedAt(), c.getUpdatedAt());
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
