package istad.ha.iamservice.persistence.adapter;

import istad.ha.iamservice.domain.model.*;
import istad.ha.iamservice.persistence.entity.*;

public final class DomainMapper {
    private DomainMapper() { }
    public static void apply(User d, UserEntity e) {
        e.applyAudit(d.audit()); e.applyUuid(d.uuid()); e.applyDeletion(d.deletion());
        e.setUsername(d.username());
        e.setEmail(d.email());
        e.setEmailVerified(d.emailVerified());
        e.setPasswordHash(d.passwordHash());
        e.setGivenName(d.profile().givenName());
        e.setFamilyName(d.profile().familyName());
        e.setMiddleName(d.profile().middleName());
        e.setNickname(d.profile().nickname());
        e.setGender(d.profile().gender());
        e.setBirthdate(d.profile().birthdate());
        e.setProfileUrl(d.profile().profileUrl());
        e.setPictureUrl(d.profile().pictureUrl());
        e.setWebsiteUrl(d.profile().websiteUrl());
        e.setZoneinfo(d.profile().zoneinfo());
        e.setLocale(d.profile().locale());
        e.setPhoneNumber(d.profile().phoneNumber());
        e.setPhoneNumberVerified(d.profile().phoneNumberVerified());
        e.setAddressStreetAddress(d.profile().address() == null ? null : d.profile().address().streetAddress());
        e.setAddressLocality(d.profile().address() == null ? null : d.profile().address().locality());
        e.setAddressRegion(d.profile().address() == null ? null : d.profile().address().region());
        e.setAddressPostalCode(d.profile().address() == null ? null : d.profile().address().postalCode());
        e.setAddressCountry(d.profile().address() == null ? null : d.profile().address().country());
        e.setEnabled(d.enabled());
        e.setAccountNonExpired(d.accountNonExpired());
        e.setAccountNonLocked(d.accountNonLocked());
        e.setCredentialsNonExpired(d.credentialsNonExpired());
        e.setFailedLoginAttempts(d.failedLoginAttempts());
        e.setLastLoginAt(d.lastLoginAt());
        e.setPasswordChangedAt(d.passwordChangedAt());
    }
    public static User user(UserEntity e) {
        Address address = e.getAddressStreetAddress() == null && e.getAddressLocality() == null
                && e.getAddressRegion() == null && e.getAddressPostalCode() == null && e.getAddressCountry() == null ? null
                : new Address(e.getAddressStreetAddress(), e.getAddressLocality(), e.getAddressRegion(), e.getAddressPostalCode(), e.getAddressCountry());
        var profile = new UserProfile(e.getGivenName(), e.getFamilyName(), e.getMiddleName(), e.getNickname(), e.getGender(), e.getBirthdate(), e.getProfileUrl(), e.getPictureUrl(), e.getWebsiteUrl(), e.getZoneinfo(), e.getLocale(), e.getPhoneNumber(), e.isPhoneNumberVerified(), address);
        return new User(e.getId(), e.getUuid(), e.getUsername(), e.getEmail(), e.isEmailVerified(), e.getPasswordHash(), profile, e.isEnabled(), e.isAccountNonExpired(), e.isAccountNonLocked(), e.isCredentialsNonExpired(), e.getFailedLoginAttempts(), e.getLastLoginAt(), e.getPasswordChangedAt(), e.deletion(), e.audit());
    }
    public static void apply(Role d, RoleEntity e) {
        e.applyAudit(d.audit()); e.applyUuid(d.uuid()); e.applyDeletion(d.deletion());
        e.setName(d.name());
        e.setDescription(d.description());
        e.setSystem(d.system());
        e.setActive(d.active());
    }
    public static Role role(RoleEntity e) {
        return new Role(e.getId(), e.getUuid(), e.getName(), e.getDescription(), e.isSystem(), e.isActive(), e.deletion(), e.audit());
    }
    public static void apply(Permission d, PermissionEntity e) {
        e.applyAudit(d.audit()); e.applyUuid(d.uuid()); e.applyDeletion(d.deletion());
        e.setName(d.name());
        e.setResource(d.resource());
        e.setAction(d.action());
        e.setDescription(d.description());
        e.setSystem(d.system());
        e.setActive(d.active());
    }
    public static Permission permission(PermissionEntity e) {
        return new Permission(e.getId(), e.getUuid(), e.getName(), e.getResource(), e.getAction(), e.getDescription(), e.isSystem(), e.isActive(), e.deletion(), e.audit());
    }
    public static void apply(Identity d, IdentityEntity e) {
        if (e.getId() != null && (e.getUserId() != d.userId() || e.getProvider() != d.provider()
                || !e.getProviderSubject().equals(d.providerSubject()) || !e.getLinkedAt().equals(d.linkedAt()))) {
            throw new istad.ha.iamservice.domain.exception.DomainException("immutable_identity", "Linked identity keys cannot be changed");
        }
        e.applyAudit(d.audit()); e.applyUuid(d.uuid()); e.applyDeletion(d.deletion());
        e.setUserId(d.userId());
        e.setProvider(d.provider());
        e.setProviderSubject(d.providerSubject());
        e.setEmail(d.email());
        e.setEmailVerified(d.emailVerified());
        e.setDisplayName(d.displayName());
        e.setUsername(d.username());
        e.setPictureUrl(d.pictureUrl());
        e.setLinkedAt(d.linkedAt());
        e.setLastLoginAt(d.lastLoginAt());
        e.setAttributes(d.attributes());
    }
    public static Identity identity(IdentityEntity e) {
        return new Identity(e.getId(), e.getUuid(), e.getUserId(), e.getProvider(), e.getProviderSubject(), e.getEmail(), e.isEmailVerified(), e.getDisplayName(), e.getUsername(), e.getPictureUrl(), e.getAttributes(), e.getLinkedAt(), e.getLastLoginAt(), e.deletion(), e.audit());
    }
    public static void apply(UserRole d, UserRoleEntity e) {
        if (e.getId() != null && (e.getUserId() != d.userId() || e.getRoleId() != d.roleId())) {
            throw new istad.ha.iamservice.domain.exception.DomainException("immutable_history", "Assignment targets cannot be changed");
        }
        e.applyMetadata(d.metadata()); e.setUserId(d.userId()); e.setRoleId(d.roleId());
    }
    public static UserRole userRole(UserRoleEntity e) {
        return new UserRole(e.getId(), e.getUserId(), e.getRoleId(), e.metadata());
    }
    public static void apply(RolePermission d, RolePermissionEntity e) {
        if (e.getId() != null && (e.getRoleId() != d.roleId() || e.getPermissionId() != d.permissionId())) {
            throw new istad.ha.iamservice.domain.exception.DomainException("immutable_history", "Assignment targets cannot be changed");
        }
        e.applyMetadata(d.metadata()); e.setRoleId(d.roleId()); e.setPermissionId(d.permissionId());
    }
    public static RolePermission rolePermission(RolePermissionEntity e) {
        return new RolePermission(e.getId(), e.getRoleId(), e.getPermissionId(), e.metadata());
    }
    public static void apply(UserPermission d, UserPermissionEntity e) {
        if (e.getId() != null && (e.getUserId() != d.userId() || e.getPermissionId() != d.permissionId() || e.getEffect() != d.effect())) {
            throw new istad.ha.iamservice.domain.exception.DomainException("immutable_history", "Assignment targets cannot be changed");
        }
        e.applyMetadata(d.metadata()); e.setUserId(d.userId()); e.setPermissionId(d.permissionId());
        e.setEffect(d.effect());
    }
    public static UserPermission userPermission(UserPermissionEntity e) {
        return new UserPermission(e.getId(), e.getUserId(), e.getPermissionId(), e.getEffect(), e.metadata());
    }
}
