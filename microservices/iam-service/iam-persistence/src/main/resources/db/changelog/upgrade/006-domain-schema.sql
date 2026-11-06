-- Forward-only domain migration. Legacy credentials and primary keys remain intact.
ALTER TABLE users RENAME COLUMN password TO password_hash;
ALTER TABLE users RENAME COLUMN birth_date TO birthdate;
ALTER TABLE users RENAME COLUMN picture TO picture_url;
ALTER TABLE users RENAME COLUMN is_enabled TO enabled;
ALTER TABLE users RENAME COLUMN is_account_non_expired TO account_non_expired;
ALTER TABLE users RENAME COLUMN is_account_non_locked TO account_non_locked;
ALTER TABLE users RENAME COLUMN is_credentials_non_expired TO credentials_non_expired;
ALTER TABLE roles RENAME COLUMN role TO name;
ALTER TABLE permissions RENAME COLUMN permission TO name;
ALTER TABLE users ALTER COLUMN username SET DATA TYPE VARCHAR(50);
ALTER TABLE users ALTER COLUMN gender SET DATA TYPE VARCHAR(20);
ALTER TABLE users ALTER COLUMN phone_number SET DATA TYPE VARCHAR(20);
ALTER TABLE users ALTER COLUMN birthdate SET DATA TYPE DATE;
ALTER TABLE roles ALTER COLUMN name SET DATA TYPE VARCHAR(50);
ALTER TABLE roles ALTER COLUMN description SET DATA TYPE VARCHAR(255);
ALTER TABLE permissions ALTER COLUMN description SET DATA TYPE VARCHAR(255);
ALTER TABLE users ADD COLUMN middle_name VARCHAR(100);
ALTER TABLE users ADD COLUMN nickname VARCHAR(100);
ALTER TABLE users ADD COLUMN profile_url VARCHAR(500);
ALTER TABLE users ADD COLUMN website_url VARCHAR(500);
ALTER TABLE users ADD COLUMN zoneinfo VARCHAR(50);
ALTER TABLE users ADD COLUMN locale VARCHAR(10) NOT NULL DEFAULT 'en';
ALTER TABLE users ADD COLUMN address_street_address VARCHAR(255);
ALTER TABLE users ADD COLUMN address_locality VARCHAR(100);
ALTER TABLE users ADD COLUMN address_region VARCHAR(100);
ALTER TABLE users ADD COLUMN address_postal_code VARCHAR(20);
ALTER TABLE users ADD COLUMN address_country CHAR(2);
ALTER TABLE users ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN last_login_at TIMESTAMPTZ;
ALTER TABLE users ADD COLUMN password_changed_at TIMESTAMPTZ;
ALTER TABLE users ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN deleted_at TIMESTAMPTZ;
ALTER TABLE users ADD COLUMN deleted_by VARCHAR(100);
ALTER TABLE users ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE roles ADD COLUMN uuid UUID;
ALTER TABLE roles ADD COLUMN is_system BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE roles ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE roles ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE roles ADD COLUMN deleted_at TIMESTAMPTZ;
ALTER TABLE roles ADD COLUMN deleted_by VARCHAR(100);
ALTER TABLE roles ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE roles ADD COLUMN created_by VARCHAR(100) NOT NULL DEFAULT 'migration';
ALTER TABLE roles ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE roles ADD COLUMN updated_by VARCHAR(100) NOT NULL DEFAULT 'migration';
ALTER TABLE roles ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE permissions ADD COLUMN uuid UUID;
ALTER TABLE permissions ADD COLUMN resource VARCHAR(50);
ALTER TABLE permissions ADD COLUMN action VARCHAR(50);
ALTER TABLE permissions ADD COLUMN is_system BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE permissions ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE permissions ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE permissions ADD COLUMN deleted_at TIMESTAMPTZ;
ALTER TABLE permissions ADD COLUMN deleted_by VARCHAR(100);
ALTER TABLE permissions ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE permissions ADD COLUMN created_by VARCHAR(100) NOT NULL DEFAULT 'migration';
ALTER TABLE permissions ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE permissions ADD COLUMN updated_by VARCHAR(100) NOT NULL DEFAULT 'migration';
ALTER TABLE permissions ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
UPDATE users SET email = LOWER(TRIM(email)), username = TRIM(username),
    given_name = TRIM(given_name), family_name = TRIM(family_name), gender = LOWER(NULLIF(TRIM(gender), '')),
    phone_number = NULLIF(TRIM(phone_number), ''), picture_url = NULLIF(TRIM(picture_url), ''),
    email_verified = COALESCE(email_verified, FALSE), phone_number_verified = COALESCE(phone_number_verified, FALSE),
    enabled = COALESCE(enabled, TRUE), account_non_expired = COALESCE(account_non_expired, TRUE),
    account_non_locked = COALESCE(account_non_locked, TRUE), credentials_non_expired = COALESCE(credentials_non_expired, TRUE),
    created_at = COALESCE(created_at, CURRENT_TIMESTAMP), created_by = COALESCE(NULLIF(TRIM(created_by), ''), 'migration'),
    updated_at = GREATEST(COALESCE(updated_at, created_at, CURRENT_TIMESTAMP), COALESCE(created_at, CURRENT_TIMESTAMP)),
    updated_by = COALESCE(NULLIF(TRIM(updated_by), ''), 'migration');
UPDATE users SET phone_number_verified = FALSE WHERE phone_number IS NULL;
UPDATE roles SET uuid = gen_random_uuid(), is_system = name IN ('ADMIN', 'USER'), description = NULLIF(TRIM(description), '');
UPDATE permissions SET uuid = gen_random_uuid(),
    resource = SUBSTRING(name FROM 1 FOR POSITION(':' IN name) - 1),
    action = SUBSTRING(name FROM POSITION(':' IN name) + 1), description = NULLIF(TRIM(description), '');
UPDATE permissions SET is_system = resource IN ('user', 'role', 'permission');
ALTER TABLE users ALTER COLUMN uuid SET NOT NULL;
ALTER TABLE users ALTER COLUMN username SET NOT NULL;
ALTER TABLE users ALTER COLUMN email SET NOT NULL;
ALTER TABLE users ALTER COLUMN email_verified SET NOT NULL;
ALTER TABLE users ALTER COLUMN given_name SET NOT NULL;
ALTER TABLE users ALTER COLUMN family_name SET NOT NULL;
ALTER TABLE users ALTER COLUMN phone_number_verified SET NOT NULL;
ALTER TABLE users ALTER COLUMN enabled SET NOT NULL;
ALTER TABLE users ALTER COLUMN account_non_expired SET NOT NULL;
ALTER TABLE users ALTER COLUMN account_non_locked SET NOT NULL;
ALTER TABLE users ALTER COLUMN credentials_non_expired SET NOT NULL;
ALTER TABLE users ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE users ALTER COLUMN created_by SET NOT NULL;
ALTER TABLE users ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE users ALTER COLUMN updated_by SET NOT NULL;
ALTER TABLE roles ALTER COLUMN uuid SET NOT NULL;
ALTER TABLE permissions ALTER COLUMN uuid SET NOT NULL;
ALTER TABLE permissions ALTER COLUMN resource SET NOT NULL;
ALTER TABLE permissions ALTER COLUMN action SET NOT NULL;
ALTER TABLE users ADD CONSTRAINT uq_users_phone UNIQUE (phone_number);
ALTER TABLE users ADD CONSTRAINT ck_users_email_lower CHECK (email = LOWER(email));
ALTER TABLE users ADD CONSTRAINT ck_users_locale CHECK (locale IN ('en', 'km'));
ALTER TABLE users ADD CONSTRAINT ck_users_failed_attempts CHECK (failed_login_attempts >= 0);
ALTER TABLE users ADD CONSTRAINT ck_users_gender CHECK (gender IS NULL OR gender IN ('female', 'male', 'other'));
ALTER TABLE roles ADD CONSTRAINT uq_roles_uuid UNIQUE (uuid);
ALTER TABLE permissions ADD CONSTRAINT uq_permissions_uuid UNIQUE (uuid);
ALTER TABLE permissions ADD CONSTRAINT ck_permissions_name CHECK (name = resource || ':' || action);
ALTER TABLE users ADD CONSTRAINT ck_users_version CHECK (version >= 0);
ALTER TABLE users ADD CONSTRAINT ck_users_deletion CHECK ((is_deleted = FALSE AND deleted_at IS NULL AND deleted_by IS NULL) OR (is_deleted = TRUE AND deleted_at IS NOT NULL AND deleted_by IS NOT NULL));
ALTER TABLE roles ADD CONSTRAINT ck_roles_version CHECK (version >= 0);
ALTER TABLE roles ADD CONSTRAINT ck_roles_deletion CHECK ((is_deleted = FALSE AND deleted_at IS NULL AND deleted_by IS NULL) OR (is_deleted = TRUE AND deleted_at IS NOT NULL AND deleted_by IS NOT NULL));
ALTER TABLE permissions ADD CONSTRAINT ck_permissions_version CHECK (version >= 0);
ALTER TABLE permissions ADD CONSTRAINT ck_permissions_deletion CHECK ((is_deleted = FALSE AND deleted_at IS NULL AND deleted_by IS NULL) OR (is_deleted = TRUE AND deleted_at IS NOT NULL AND deleted_by IS NOT NULL));
CREATE TABLE identities (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    provider VARCHAR(20) NOT NULL,
    provider_subject VARCHAR(255) NOT NULL,
    email VARCHAR(255), email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    display_name VARCHAR(150), username VARCHAR(100), picture_url VARCHAR(500), attributes JSONB,
    linked_at TIMESTAMPTZ NOT NULL, last_login_at TIMESTAMPTZ,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE, deleted_at TIMESTAMPTZ, deleted_by VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'migration',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100) NOT NULL DEFAULT 'migration',
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_identity_subject UNIQUE(provider, provider_subject),
    CONSTRAINT uq_identity_user_provider UNIQUE(user_id, provider),
    CONSTRAINT ck_identity_provider CHECK (provider IN ('GOOGLE', 'GITHUB', 'FACEBOOK', 'TELEGRAM')),
    CONSTRAINT ck_identity_email CHECK (email IS NULL OR email = LOWER(email)),
    CONSTRAINT ck_identity_verified_email CHECK (email IS NOT NULL OR email_verified = FALSE),
    CONSTRAINT ck_identity_deletion CHECK ((is_deleted = FALSE AND deleted_at IS NULL AND deleted_by IS NULL) OR (is_deleted = TRUE AND deleted_at IS NOT NULL AND deleted_by IS NOT NULL)),
    CONSTRAINT ck_identity_version CHECK (version >= 0)
);
ALTER TABLE users_roles RENAME TO legacy_users_roles;
ALTER TABLE roles_permissions RENAME TO legacy_roles_permissions;
CREATE TABLE user_roles (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    role_id BIGINT NOT NULL REFERENCES roles(id),
    assigned_at TIMESTAMPTZ NOT NULL, assigned_by VARCHAR(100) NOT NULL,
    expires_at TIMESTAMPTZ, revoked_at TIMESTAMPTZ, revoked_by VARCHAR(100), reason VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'migration',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100) NOT NULL DEFAULT 'migration',
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_user_roles_expiry CHECK (expires_at IS NULL OR expires_at > assigned_at),
    CONSTRAINT ck_user_roles_revocation CHECK ((revoked_at IS NULL AND revoked_by IS NULL) OR (revoked_at IS NOT NULL AND revoked_by IS NOT NULL AND revoked_at >= assigned_at)),
    CONSTRAINT ck_user_roles_version CHECK (version >= 0)
);
CREATE INDEX ix_user_roles_user ON user_roles(user_id);
CREATE TABLE role_permissions (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    role_id BIGINT NOT NULL REFERENCES roles(id),
    permission_id BIGINT NOT NULL REFERENCES permissions(id),
    assigned_at TIMESTAMPTZ NOT NULL, assigned_by VARCHAR(100) NOT NULL,
    expires_at TIMESTAMPTZ, revoked_at TIMESTAMPTZ, revoked_by VARCHAR(100), reason VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'migration',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100) NOT NULL DEFAULT 'migration',
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_role_permissions_expiry CHECK (expires_at IS NULL OR expires_at > assigned_at),
    CONSTRAINT ck_role_permissions_revocation CHECK ((revoked_at IS NULL AND revoked_by IS NULL) OR (revoked_at IS NOT NULL AND revoked_by IS NOT NULL AND revoked_at >= assigned_at)),
    CONSTRAINT ck_role_permissions_version CHECK (version >= 0)
);
CREATE INDEX ix_role_permissions_role ON role_permissions(role_id);
CREATE TABLE user_permissions (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    permission_id BIGINT NOT NULL REFERENCES permissions(id),
    effect VARCHAR(5) NOT NULL CHECK (effect IN ('GRANT', 'DENY')),
    assigned_at TIMESTAMPTZ NOT NULL, assigned_by VARCHAR(100) NOT NULL,
    expires_at TIMESTAMPTZ, revoked_at TIMESTAMPTZ, revoked_by VARCHAR(100), reason VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'migration',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100) NOT NULL DEFAULT 'migration',
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_user_permissions_expiry CHECK (expires_at IS NULL OR expires_at > assigned_at),
    CONSTRAINT ck_user_permissions_revocation CHECK ((revoked_at IS NULL AND revoked_by IS NULL) OR (revoked_at IS NOT NULL AND revoked_by IS NOT NULL AND revoked_at >= assigned_at)),
    CONSTRAINT ck_user_permissions_version CHECK (version >= 0)
);
CREATE INDEX ix_user_permissions_user ON user_permissions(user_id);
INSERT INTO user_roles(user_id, role_id, assigned_at, assigned_by, reason)
    SELECT user_id, role_id, CURRENT_TIMESTAMP, 'migration', 'Preserved legacy membership' FROM legacy_users_roles;
INSERT INTO role_permissions(role_id, permission_id, assigned_at, assigned_by, reason)
    SELECT role_id, permission_id, CURRENT_TIMESTAMP, 'migration', 'Preserved legacy grant' FROM legacy_roles_permissions;
CREATE INDEX ix_identities_user ON identities(user_id);
