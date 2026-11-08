package pe.techsupport.user.service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

import com.fasterxml.jackson.core.type.TypeReference;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.techsupport.common.audit.AuditLogger;
import pe.techsupport.common.error.ApiException;
import pe.techsupport.common.resilience.ResilientCache;
import pe.techsupport.common.security.AuthUser;
import pe.techsupport.common.security.SecurityUtils;
import pe.techsupport.user.config.CacheTtlProperties;
import pe.techsupport.user.domain.Specialty;
import pe.techsupport.user.domain.SupportLevel;
import pe.techsupport.user.domain.Technician;
import pe.techsupport.user.domain.TechnicianStatus;
import pe.techsupport.user.repository.SpecialtyCatalogRepository;
import pe.techsupport.user.repository.TechnicianRepository;
import pe.techsupport.user.web.dto.UserDtos.AvailableTechnicians;
import pe.techsupport.user.web.dto.UserDtos.CreateTechnicianRequest;
import pe.techsupport.user.web.dto.UserDtos.SpecialtyResponse;
import pe.techsupport.user.web.dto.UserDtos.TechnicianCandidate;
import pe.techsupport.user.web.dto.UserDtos.TechnicianResponse;
import pe.techsupport.user.web.dto.UserDtos.WorkloadResponse;

@Service
public class TechnicianService {

    static final String CACHE = "tech";
    static final String CACHE_LIST = "tech-list";
    static final String CACHE_AVAILABLE = "tech-available";
    static final String CACHE_SPECIALTY = "tech-specialty";

    private final TechnicianRepository technicians;
    private final SpecialtyCatalogRepository specialties;
    private final ResilientCache cache;
    private final CacheTtlProperties ttl;
    private final AuditLogger audit;
    private final MeterRegistry meters;

    public TechnicianService(TechnicianRepository technicians, SpecialtyCatalogRepository specialties,
                             ResilientCache cache, CacheTtlProperties ttl, AuditLogger audit, MeterRegistry meters) {
        this.technicians = technicians;
        this.specialties = specialties;
        this.cache = cache;
        this.ttl = ttl;
        this.audit = audit;
        this.meters = meters;
    }

    public List<TechnicianResponse> list() {
        return cache.get(CACHE_LIST, "all", ttl.getList(), new TypeReference<List<TechnicianResponse>>() { },
                () -> technicians.findAllByOrderBySeqAsc().stream().map(TechnicianService::toResponse).toList());
    }

    public TechnicianResponse get(String userId) {
        return cache.get(CACHE, userId, ttl.getProfile(), TechnicianResponse.class,
                () -> toResponse(find(userId)));
    }

    /**
     * Técnicos aptos para despacho: especialidad requerida, nivel mínimo, disponibles y con cupo.
     * Ordenados por menor carga (balanceo). Sin especialidad devuelve todos los disponibles.
     */
    public AvailableTechnicians available(Specialty specialty, SupportLevel minLevel) {
        String key = (specialty == null ? "ANY" : specialty.name()) + ":" + (minLevel == null ? "N1" : minLevel.name());
        List<TechnicianCandidate> candidates = cache.get(CACHE_AVAILABLE, key, ttl.getAvailable(),
                new TypeReference<List<TechnicianCandidate>>() { },
                () -> {
                    List<Technician> found = specialty == null
                            ? technicians.findAllDispatchCandidates(TechnicianStatus.ACTIVO)
                            : technicians.findDispatchCandidates(specialty, TechnicianStatus.ACTIVO);
                    return found.stream()
                            .filter(t -> minLevel == null || t.getSupportLevel().covers(minLevel))
                            .map(TechnicianService::toCandidate)
                            .toList();
                });
        return new AvailableTechnicians(!candidates.isEmpty(), candidates);
    }

    public List<TechnicianCandidate> bySpecialty(Specialty specialty) {
        return cache.get(CACHE_SPECIALTY, specialty.name(), ttl.getList(),
                new TypeReference<List<TechnicianCandidate>>() { },
                () -> technicians.findBySpecialty(specialty).stream().map(TechnicianService::toCandidate).toList());
    }

    public WorkloadResponse workload(String userId) {
        TechnicianResponse t = get(userId);
        return new WorkloadResponse(t.userId(), t.currentTickets(), t.maxConcurrentTickets(), t.isAvailable(),
                t.availability());
    }

    public List<SpecialtyResponse> specialties() {
        return cache.get("specialties", "all", ttl.getList().multipliedBy(60),
                new TypeReference<List<SpecialtyResponse>>() { },
                () -> specialties.findAll().stream()
                        .map(s -> new SpecialtyResponse(s.getCode(), s.getDescription())).toList());
    }

    @CircuitBreaker(name = "db")
    @Transactional
    public TechnicianResponse create(CreateTechnicianRequest req) {
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        if (technicians.existsByEmail(email)) {
            throw ApiException.conflict("email_already_registered", "Ya existe un técnico con el correo " + email);
        }
        int max = req.maxConcurrentTickets() == null ? 5 : req.maxConcurrentTickets();
        Technician tech = technicians.save(new Technician(req.firstName().trim(), req.lastName().trim(), email,
                req.phone(), req.supportLevel(), req.specialties(), max));
        tech.assignUserId();
        evictTechnicianCaches(null);
        audit.success("TECHNICIAN_CREATED", tech.getUserId(),
                "Técnico " + req.supportLevel() + " con especialidades " + req.specialties());
        return toResponse(tech);
    }

    @CircuitBreaker(name = "db")
    @Transactional
    public TechnicianResponse updateAvailability(String userId, boolean available) {
        AuthUser user = SecurityUtils.currentUser();
        if (user.isTechnician()) {
            SecurityUtils.requireSelfOrPrivileged(userId);
        }
        Technician tech = find(userId);
        tech.setAvailable(available);
        technicians.flush();
        evictTechnicianCaches(userId);
        audit.success("TECHNICIAN_AVAILABILITY", userId, available ? "DISPONIBLE" : "NO DISPONIBLE");
        return toResponse(tech);
    }

    /**
     * Ajuste atómico de carga: +1 reserva un cupo (falla con 409 si ya no hay), -1 lo libera.
     * Solo lo invoca el Ticket Service (rol SYSTEM).
     */
    @CircuitBreaker(name = "db")
    @Transactional
    public WorkloadResponse adjustWorkload(String userId, int delta) {
        Instant now = Instant.now();
        if (delta > 0) {
            int updated = technicians.reserveSlot(userId, TechnicianStatus.ACTIVO, now);
            if (updated == 0) {
                find(userId); // 404 si no existe
                meters.counter("technicians.reservations", "result", "rejected").increment();
                throw ApiException.conflict("technician_unavailable",
                        "El técnico " + userId + " no está disponible o alcanzó su límite de tickets");
            }
            meters.counter("technicians.reservations", "result", "accepted").increment();
        } else if (delta < 0) {
            if (technicians.releaseSlot(userId, now) == 0) {
                throw ApiException.notFound("technician_not_found", "No existe el técnico " + userId);
            }
        }
        Technician tech = find(userId);
        evictTechnicianCaches(userId);
        return new WorkloadResponse(tech.getUserId(), tech.getCurrentTickets(), tech.getMaxConcurrentTickets(),
                tech.isAvailable(), tech.availabilityState());
    }

    @CircuitBreaker(name = "db")
    @Transactional
    public void delete(String userId) {
        technicians.findByUserId(userId).ifPresent(technicians::delete);
        evictTechnicianCaches(userId);
        audit.success("TECHNICIAN_DELETED", userId, "Perfil eliminado por compensación");
    }

    private void evictTechnicianCaches(String userId) {
        cache.afterCommit(() -> {
            if (userId != null) {
                cache.evict(CACHE, userId);
            }
            cache.evictAll(CACHE_LIST);
            cache.evictAll(CACHE_AVAILABLE);
            cache.evictAll(CACHE_SPECIALTY);
        });
    }

    private Technician find(String userId) {
        return technicians.findByUserId(userId)
                .orElseThrow(() -> ApiException.notFound("technician_not_found", "No existe el técnico " + userId));
    }

    static TechnicianResponse toResponse(Technician t) {
        return new TechnicianResponse(t.getUserId(), t.getFirstName(), t.getLastName(), t.getEmail(), t.getPhone(),
                "TECNICO", t.getSupportLevel(), t.getSpecialties(), t.getStatus().name(), t.availabilityState(),
                t.isAvailable(), t.getCurrentTickets(), t.getMaxConcurrentTickets(), t.getCreatedAt(),
                t.getUpdatedAt());
    }

    static TechnicianCandidate toCandidate(Technician t) {
        return new TechnicianCandidate(t.getUserId(), t.fullName(), t.getSupportLevel(), t.getSpecialties(),
                t.isAvailable(), t.getCurrentTickets(), t.getMaxConcurrentTickets());
    }
}
