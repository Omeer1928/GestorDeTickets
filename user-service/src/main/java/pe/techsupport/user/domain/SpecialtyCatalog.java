package pe.techsupport.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "specialty")
public class SpecialtyCatalog {

    @Id
    @Column(length = 40)
    private String code;

    @Column(nullable = false)
    private String description;

    protected SpecialtyCatalog() {
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }
}
