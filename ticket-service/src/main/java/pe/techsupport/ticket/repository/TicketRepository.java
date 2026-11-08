package pe.techsupport.ticket.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.techsupport.ticket.domain.QueueName;
import pe.techsupport.ticket.domain.Ticket;
import pe.techsupport.ticket.domain.TicketStatus;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    @EntityGraph(attributePaths = "escalations")
    Optional<Ticket> findByTicketId(String ticketId);

    List<Ticket> findByClientIdOrderByRegisteredAtDesc(String clientId);

    List<Ticket> findByAssignedTechIdOrderByRegisteredAtDesc(String techId);

    List<Ticket> findByStatusOrderByPriorityRankDescRegisteredAtAsc(TicketStatus status);

    List<Ticket> findAllByOrderByRegisteredAtDesc(Pageable pageable);

    /** Cola de despacho: primero mayor prioridad, luego el más antiguo (FIFO dentro de la prioridad). */
    @Query("""
            select t from Ticket t
            where t.status in :statuses and t.assignedTechId is null
            order by t.priorityRank desc, t.registeredAt asc""")
    List<Ticket> findWaiting(@Param("statuses") Collection<TicketStatus> statuses, Pageable pageable);

    /** Posición en la cola = tickets en espera con mayor prioridad o igual prioridad y más antiguos + 1. */
    @Query("""
            select count(t) from Ticket t
            where t.status in :statuses and t.assignedTechId is null and t.id <> :id
              and (t.priorityRank > :rank or (t.priorityRank = :rank and t.registeredAt < :registeredAt))""")
    long countAhead(@Param("statuses") Collection<TicketStatus> statuses, @Param("id") Long id,
                    @Param("rank") int rank, @Param("registeredAt") Instant registeredAt);

    long countByQueueNameAndAssignedTechIdIsNull(QueueName queueName);
}
