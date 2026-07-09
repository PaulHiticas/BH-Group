-- ============================================================
-- BH Group PMS — Initial schema (Stage 1: Auth & Identity)
-- ============================================================

CREATE TYPE user_role AS ENUM (
    'SUPER_ADMIN',
    'ADMINISTRATOR'
);

CREATE TYPE user_status AS ENUM (
    'PENDING',
    'ACTIVE',
    'SUSPENDED',
    'DISABLED'
);

CREATE TYPE verification_token_type AS ENUM (
    'PASSWORD_RESET'
);

-- ------------------------------------------------------------
-- users
-- ------------------------------------------------------------
CREATE TABLE users (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email                  VARCHAR(255) NOT NULL,
    password_hash          VARCHAR(255) NOT NULL,
    first_name             VARCHAR(100) NOT NULL,
    last_name              VARCHAR(100) NOT NULL,
    phone                  VARCHAR(30),
    role                   user_role NOT NULL DEFAULT 'ADMINISTRATOR',
    status                 user_status NOT NULL DEFAULT 'ACTIVE',
    email_verified         BOOLEAN NOT NULL DEFAULT TRUE,
    mfa_enabled            BOOLEAN NOT NULL DEFAULT FALSE,
    mfa_secret             VARCHAR(255),
    failed_login_attempts  INTEGER NOT NULL DEFAULT 0,
    locked_until           TIMESTAMPTZ,
    last_login_at          TIMESTAMPTZ,
    last_login_ip          VARCHAR(45),
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by             UUID,
    updated_by             UUID
);

CREATE UNIQUE INDEX ux_users_email_lower ON users (LOWER(email));
CREATE INDEX ix_users_role ON users (role);
CREATE INDEX ix_users_status ON users (status);

-- ------------------------------------------------------------
-- refresh_tokens
-- ------------------------------------------------------------
CREATE TABLE refresh_tokens (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                  UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash               VARCHAR(255) NOT NULL,
    expires_at               TIMESTAMPTZ NOT NULL,
    revoked                  BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at               TIMESTAMPTZ,
    replaced_by_token_hash   VARCHAR(255),
    created_by_ip            VARCHAR(45),
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_refresh_tokens_token_hash ON refresh_tokens (token_hash);
CREATE INDEX ix_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX ix_refresh_tokens_expires_at ON refresh_tokens (expires_at);

-- ------------------------------------------------------------
-- verification_tokens (email verification + password reset)
-- ------------------------------------------------------------
CREATE TABLE verification_tokens (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token        VARCHAR(255) NOT NULL,
    type         verification_token_type NOT NULL,
    expires_at   TIMESTAMPTZ NOT NULL,
    used_at      TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_verification_tokens_token ON verification_tokens (token);
CREATE INDEX ix_verification_tokens_user_id ON verification_tokens (user_id);
CREATE INDEX ix_verification_tokens_type ON verification_tokens (type);

-- ------------------------------------------------------------
-- audit_logs
-- ------------------------------------------------------------
CREATE TABLE audit_logs (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_name   VARCHAR(100) NOT NULL,
    entity_id     VARCHAR(100),
    action        VARCHAR(50) NOT NULL,
    actor_id      UUID,
    actor_email   VARCHAR(255),
    ip_address    VARCHAR(45),
    user_agent    VARCHAR(500),
    old_value     JSONB,
    new_value     JSONB,
    description   VARCHAR(1000),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_audit_logs_entity ON audit_logs (entity_name, entity_id);
CREATE INDEX ix_audit_logs_actor_id ON audit_logs (actor_id);
CREATE INDEX ix_audit_logs_created_at ON audit_logs (created_at);

-- ------------------------------------------------------------
-- updated_at trigger
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();
