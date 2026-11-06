package istad.ha.iamservice.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.time.LocalDate;
import istad.ha.iamservice.domain.model.Provider;

@Entity @Table(name = "identities")
@Getter @Setter
public class IdentityEntity extends SoftDeletableEntity {
    @Column(name = "user_id", nullable = false) private long userId;
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20) private Provider provider;
    @Column(name = "provider_subject", nullable = false, length = 255) private String providerSubject;
    @Column(name = "email", nullable = true, length = 255) private String email;
    @Column(name = "email_verified", nullable = false) private boolean emailVerified;
    @Column(name = "display_name", nullable = true, length = 150) private String displayName;
    @Column(name = "username", nullable = true, length = 100) private String username;
    @Column(name = "picture_url", nullable = true, length = 500) private String pictureUrl;
    @Column(name = "linked_at", nullable = false) private Instant linkedAt;
    @Column(name = "last_login_at", nullable = true) private Instant lastLoginAt;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "attributes", columnDefinition = "jsonb")
    private java.util.Map<String, Object> attributes;
}
