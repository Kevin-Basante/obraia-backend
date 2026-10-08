-- =====================================================================
-- ObraIA · V1: authentication block (10 tables)
-- Flyway runs this file only once. Never edit it after it has been
-- applied: any change goes in a new file (V4__..., V5__...).
-- =====================================================================

-- Reusable function: refreshes updated_at whenever a row changes.
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS trigger AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ---------------------------------------------------------------------
-- users: email accounts (owners and company administrators)
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id                uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    email             varchar(255) NOT NULL,
    password_hash     varchar(255) NOT NULL,
    first_name        varchar(100) NOT NULL,
    last_name         varchar(100) NOT NULL,
    phone             varchar(20),
    email_verified_at timestamptz,
    is_active         boolean      NOT NULL DEFAULT true,
    last_login_at     timestamptz,
    created_at        timestamptz  NOT NULL DEFAULT now(),
    updated_at        timestamptz  NOT NULL DEFAULT now(),
    deleted_at        timestamptz,
    CONSTRAINT ck_users_email_format CHECK (email ~* '^[^@\s]+@[^@\s]+\.[^@\s]+$')
);
-- An email can belong to only one active account (case-insensitive).
CREATE UNIQUE INDEX uq_users_email ON users (lower(email)) WHERE deleted_at IS NULL;
CREATE TRIGGER trg_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------
-- roles: 'platform' roles go in user_roles; 'project' roles in project_members
-- ---------------------------------------------------------------------
CREATE TABLE roles (
    id          uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    name        varchar(50)  NOT NULL UNIQUE,
    scope       varchar(10)  NOT NULL,
    description varchar(255),
    created_at  timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT ck_roles_scope CHECK (scope IN ('platform', 'project'))
);

CREATE TABLE permissions (
    id          uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    code        varchar(100) NOT NULL UNIQUE,
    description varchar(255),
    created_at  timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE role_permissions (
    role_id       uuid NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    permission_id uuid NOT NULL REFERENCES permissions (id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);
CREATE INDEX ix_role_permissions_permission ON role_permissions (permission_id);

CREATE TABLE user_roles (
    user_id     uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id     uuid        NOT NULL REFERENCES roles (id) ON DELETE RESTRICT,
    assigned_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, role_id)
);
CREATE INDEX ix_user_roles_role ON user_roles (role_id);

-- ---------------------------------------------------------------------
-- Tokens: store the SHA-256 hash of the token, never the plain token.
-- ---------------------------------------------------------------------
CREATE TABLE refresh_tokens (
    id         uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    uuid         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash varchar(255) NOT NULL UNIQUE,
    expires_at timestamptz  NOT NULL,
    revoked_at timestamptz,
    created_at timestamptz  NOT NULL DEFAULT now()
);
CREATE INDEX ix_refresh_tokens_user ON refresh_tokens (user_id);

CREATE TABLE password_reset_tokens (
    id         uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    uuid         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash varchar(255) NOT NULL UNIQUE,
    expires_at timestamptz  NOT NULL,
    used_at    timestamptz,
    created_at timestamptz  NOT NULL DEFAULT now()
);
CREATE INDEX ix_password_reset_tokens_user ON password_reset_tokens (user_id);

CREATE TABLE email_verification_tokens (
    id         uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    uuid         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash varchar(255) NOT NULL UNIQUE,
    expires_at timestamptz  NOT NULL,
    used_at    timestamptz,
    created_at timestamptz  NOT NULL DEFAULT now()
);
CREATE INDEX ix_email_verification_tokens_user ON email_verification_tokens (user_id);

-- Email confirmation before deleting an account or a project.
-- target_id is the user id (account) or the project id (project).
CREATE TABLE deletion_requests (
    id           uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      uuid         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    target_type  varchar(10)  NOT NULL,
    target_id    uuid         NOT NULL,
    token_hash   varchar(255) NOT NULL UNIQUE,
    expires_at   timestamptz  NOT NULL,
    confirmed_at timestamptz,
    created_at   timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT ck_deletion_requests_target_type CHECK (target_type IN ('account', 'project'))
);
CREATE INDEX ix_deletion_requests_user ON deletion_requests (user_id);

-- Security event log. user_id is null when someone tries to sign in
-- with an email that does not exist.
CREATE TABLE auth_audit_logs (
    id              uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         uuid         REFERENCES users (id) ON DELETE SET NULL,
    attempted_email varchar(255),
    event_type      varchar(40)  NOT NULL,
    ip_address      varchar(45),
    user_agent      varchar(500),
    created_at      timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT ck_auth_audit_logs_event_type CHECK (event_type IN (
        'register', 'email_verified', 'login_success', 'login_failed', 'logout',
        'token_refreshed', 'password_reset_requested', 'password_changed',
        'deletion_requested', 'account_deleted'))
);
CREATE INDEX ix_auth_audit_logs_user ON auth_audit_logs (user_id);
CREATE INDEX ix_auth_audit_logs_created_at ON auth_audit_logs (created_at);
