package pe.techsupport.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "role_catalog")
public class RoleCatalog {

    @Id
    @Column(length = 20)
    private String code;

    @Column(nullable = false)
    private String description;

    protected RoleCatalog() {
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }
}
