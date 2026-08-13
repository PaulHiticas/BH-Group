-- ============================================================
-- BH Group PMS — Late checkout add-on (request-only, no payment yet)
-- ============================================================

ALTER TABLE properties
    ADD COLUMN late_checkout_enabled BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN late_checkout_time    TIME,
    ADD COLUMN late_checkout_fee     NUMERIC(10, 2);

CREATE TYPE late_checkout_status AS ENUM (
    'REQUESTED',
    'APPROVED',
    'REJECTED',
    'PAID'
);

-- The offered time and fee are snapshotted onto the request at the moment
-- it's made (same reasoning as owner_statement_lines.property_name): if
-- staff later change the property's late-checkout config, an existing
-- request must keep showing what the guest actually asked for and was
-- quoted, not silently pick up the new numbers.
CREATE TABLE late_checkout_requests (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reservation_id          UUID NOT NULL UNIQUE REFERENCES reservations (id) ON DELETE CASCADE,

    requested_checkout_time TIME NOT NULL,
    fee                     NUMERIC(10, 2),
    currency                VARCHAR(3) NOT NULL DEFAULT 'RON',

    status                  late_checkout_status NOT NULL DEFAULT 'REQUESTED',
    guest_note              VARCHAR(1000),

    decided_by              UUID REFERENCES users (id) ON DELETE SET NULL,
    decided_at              TIMESTAMPTZ,
    paid_at                 TIMESTAMPTZ,

    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_late_checkout_requests_status ON late_checkout_requests (status);

CREATE TRIGGER trg_late_checkout_requests_updated_at
    BEFORE UPDATE ON late_checkout_requests
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();
