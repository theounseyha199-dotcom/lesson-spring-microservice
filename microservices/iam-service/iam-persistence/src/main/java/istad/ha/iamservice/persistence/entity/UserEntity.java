package istad.ha.iamservice.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.time.LocalDate;
import istad.ha.iamservice.domain.model.Provider;

@Entity @Table(name = "users")
@Getter @Setter
public class UserEntity extends SoftDeletableEntity {
    @Column(name = "username", nullable = false, length = 50, unique = true) private String username;
    @Column(name = "email", nullable = false, length = 255, unique = true) private String email;
    @Column(name = "email_verified", nullable = false) private boolean emailVerified;
    @Column(name = "password_hash", nullable = true, length = 255) private String passwordHash;
    @Column(name = "given_name", nullable = false, length = 100) private String givenName;
    @Column(name = "family_name", nullable = false, length = 100) private String familyName;
    @Column(name = "middle_name", nullable = true, length = 100) private String middleName;
    @Column(name = "nickname", nullable = true, length = 100) private String nickname;
    @Column(name = "gender", nullable = true, length = 20) private String gender;
    @Column(name = "birthdate", nullable = true) private LocalDate birthdate;
    @Column(name = "profile_url", nullable = true, length = 500) private String profileUrl;
    @Column(name = "picture_url", nullable = true, length = 500) private String pictureUrl;
    @Column(name = "website_url", nullable = true, length = 500) private String websiteUrl;
    @Column(name = "zoneinfo", nullable = true, length = 50) private String zoneinfo;
    @Column(name = "locale", nullable = false, length = 10) private String locale;
    @Column(name = "phone_number", nullable = true, length = 20, unique = true) private String phoneNumber;
    @Column(name = "phone_number_verified", nullable = false) private boolean phoneNumberVerified;
    @Column(name = "address_street_address", nullable = true, length = 255) private String addressStreetAddress;
    @Column(name = "address_locality", nullable = true, length = 100) private String addressLocality;
    @Column(name = "address_region", nullable = true, length = 100) private String addressRegion;
    @Column(name = "address_postal_code", nullable = true, length = 20) private String addressPostalCode;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.CHAR)
    @Column(name = "address_country", nullable = true, length = 2, columnDefinition = "char(2)") private String addressCountry;
    @Column(name = "enabled", nullable = false) private boolean enabled;
    @Column(name = "account_non_expired", nullable = false) private boolean accountNonExpired;
    @Column(name = "account_non_locked", nullable = false) private boolean accountNonLocked;
    @Column(name = "credentials_non_expired", nullable = false) private boolean credentialsNonExpired;
    @Column(name = "failed_login_attempts", nullable = false) private int failedLoginAttempts;
    @Column(name = "last_login_at", nullable = true) private Instant lastLoginAt;
    @Column(name = "password_changed_at", nullable = true) private Instant passwordChangedAt;
}
