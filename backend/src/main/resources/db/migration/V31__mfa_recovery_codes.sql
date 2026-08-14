-- ============================================================
-- BH Group PMS — Hashed one-time MFA recovery codes
-- ============================================================
-- Codes are shown to the user exactly once, at MFA-enable time, and
-- only their SHA-256 hash is ever stored (same approach as refresh
-- tokens - see SecureTokenGenerator.hash()).

CREATE TABLE mfa_recovery_codes (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    code_hash   VARCHAR(255) NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_mfa_recovery_codes_user_id ON mfa_recovery_codes (user_id);

CREATE TRIGGER trg_mfa_recovery_codes_updated_at
    BEFORE UPDATE ON mfa_recovery_codes
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();
