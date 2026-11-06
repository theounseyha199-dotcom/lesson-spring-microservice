package istad.ha.iamservice.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.time.LocalDate;
import istad.ha.iamservice.domain.model.Provider;

@Entity @Table(name = "roles")
@Getter @Setter
public class RoleEntity extends SoftDeletableEntity {
    @Column(name = "name", nullable = false, length = 50, unique = true) private String name;
    @Column(name = "description", nullable = true, length = 255) private String description;
    @Column(name = "is_system", nullable = false) private boolean system;
    @Column(name = "is_active", nullable = false) private boolean active;
}
