-- ============================================================
-- BH Group PMS — GDPR request compliance register
-- ============================================================
-- Deliberately does not store the full email of the data subject
-- (masked_email only) and does not store the admin's free-text
-- verification note - both are legal/retention decisions not made
-- here. This table only proves a request was handled, by whom, and
-- how identity was checked.

CREATE TYPE gdpr_request_type AS ENUM (
    'EXPORT',
    'ERASE'
);

CREATE TYPE gdpr_request_status AS ENUM (
    'COMPLETED',
    'FAILED'
);

CREATE TYPE gdpr_verification_method AS ENUM (
    'EMAIL_CONFIRMATION',
    'RESERVATION_DETAILS',
    'IDENTITY_DOCUMENT',
    'OTHER'
);

CREATE TABLE gdpr_requests (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_type          gdpr_request_type NOT NULL,
    status                gdpr_request_status NOT NULL DEFAULT 'COMPLETED',
    actor_id              UUID REFERENCES users (id) ON DELETE SET NULL,
    masked_email          VARCHAR(255) NOT NULL,
    verification_method   gdpr_verification_method NOT NULL,
    verified_at           TIMESTAMPTZ NOT NULL,
    records_affected      INTEGER NOT NULL,

    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_gdpr_requests_actor_id ON gdpr_requests (actor_id);
CREATE INDEX ix_gdpr_requests_created_at ON gdpr_requests (created_at);

CREATE TRIGGER trg_gdpr_requests_updated_at
    BEFORE UPDATE ON gdpr_requests
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();
