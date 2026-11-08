package pe.techsupport.user.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.techsupport.user.domain.Specialty;
import pe.techsupport.user.domain.Technician;
import pe.techsupport.user.domain.TechnicianStatus;

public interface TechnicianRepository extends JpaRepository<Technician, Long> {

    Optional<Technician> findByUserId(String userId);

    boolean existsByEmail(String email);

    List<Technician> findAllByOrderBySeqAsc();

    @Query("select distinct t from Technician t join t.specialties s where s = :specialty order by t.seq")
    List<Technician> findBySpecialty(@Param("specialty") Specialty specialty);

    /** Candidatos para despacho: activos, disponibles y con capacidad libre; menor carga primero. */
    @Query("""
            select distinct t from Technician t join t.specialties s
            where s = :specialty and t.status = :active and t.available = true
              and t.currentTickets < t.maxConcurrentTickets
            order by t.currentTickets asc, t.seq asc""")
    List<Technician> findDispatchCandidates(@Param("specialty") Specialty specialty,
                                            @Param("active") TechnicianStatus active);

    @Query("""
            select t from Technician t
            where t.status = :active and t.available = true and t.currentTickets < t.maxConcurrentTickets
            order by t.currentTickets asc, t.seq asc""")
    List<Technician> findAllDispatchCandidates(@Param("active") TechnicianStatus active);

    /**
     * Reserva atómica de capacidad: el UPDATE solo afecta la fila si aún hay cupo, por lo que dos
     * asignaciones concurrentes nunca superan max_concurrent_tickets (sin bloqueos explícitos).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Technician t set t.currentTickets = t.currentTickets + 1, t.updatedAt = :now
            where t.userId = :userId and t.status = :active and t.available = true
              and t.currentTickets < t.maxConcurrentTickets""")
    int reserveSlot(@Param("userId") String userId, @Param("active") TechnicianStatus active,
                    @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Technician t
            set t.currentTickets = case when t.currentTickets > 0 then t.currentTickets - 1 else 0 end,
                t.updatedAt = :now
            where t.userId = :userId""")
    int releaseSlot(@Param("userId") String userId, @Param("now") Instant now);
}
