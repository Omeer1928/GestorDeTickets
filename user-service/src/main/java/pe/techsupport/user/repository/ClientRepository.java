package pe.techsupport.user.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.techsupport.user.domain.Client;

public interface ClientRepository extends JpaRepository<Client, Long> {

    Optional<Client> findByUserId(String userId);

    boolean existsByEmail(String email);

    List<Client> findAllByOrderBySeqAsc();
}
