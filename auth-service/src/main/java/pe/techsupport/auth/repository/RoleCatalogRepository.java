package pe.techsupport.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.techsupport.auth.domain.RoleCatalog;

public interface RoleCatalogRepository extends JpaRepository<RoleCatalog, String> {
}
