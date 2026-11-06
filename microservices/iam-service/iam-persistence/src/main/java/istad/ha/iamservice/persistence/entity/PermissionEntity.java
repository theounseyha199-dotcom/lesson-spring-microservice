package istad.ha.iamservice.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.time.LocalDate;
import istad.ha.iamservice.domain.model.Provider;

@Entity @Table(name = "permissions")
@Getter @Setter
public class PermissionEntity extends SoftDeletableEntity {
    @Column(name = "name", nullable = false, length = 100, unique = true) private String name;
    @Column(name = "resource", nullable = false, length = 50) private String resource;
    @Column(name = "action", nullable = false, length = 50) private String action;
    @Column(name = "description", nullable = true, length = 255) private String description;
    @Column(name = "is_system", nullable = false) private boolean system;
    @Column(name = "is_active", nullable = false) private boolean active;
}
