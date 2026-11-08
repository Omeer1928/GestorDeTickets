package pe.techsupport.ticket.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.techsupport.ticket.domain.TicketEvent;

public interface TicketEventRepository extends JpaRepository<TicketEvent, Long> {

    List<TicketEvent> findByTicketPkOrderByCreatedAtAsc(Long ticketPk);
}
