package pe.techsupport.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.techsupport.user.domain.SpecialtyCatalog;

public interface SpecialtyCatalogRepository extends JpaRepository<SpecialtyCatalog, String> {
}
