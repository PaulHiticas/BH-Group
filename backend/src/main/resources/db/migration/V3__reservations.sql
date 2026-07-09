-- ============================================================
-- BH Group PMS — Reservations (Stage 3)
-- ============================================================

CREATE TYPE reservation_status AS ENUM (
    'PENDING',
    'CONFIRMED',
    'CHECKED_IN',
    'CHECKED_OUT',
    'CANCELLED',
    'NO_SHOW'
);

CREATE TYPE reservation_source AS ENUM (
    'DIRECT',
    'AIRBNB',
    'BOOKING_COM',
    'OTHER'
);

-- ------------------------------------------------------------
-- reservations
-- ------------------------------------------------------------
CREATE TABLE reservations (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    property_id       UUID NOT NULL REFERENCES properties (id) ON DELETE CASCADE,

    guest_first_name  VARCHAR(100) NOT NULL,
    guest_last_name   VARCHAR(100) NOT NULL,
    guest_email       VARCHAR(255),
    guest_phone       VARCHAR(30),

    check_in_date     DATE NOT NULL,
    check_out_date    DATE NOT NULL,
    number_of_guests  INTEGER NOT NULL DEFAULT 1,

    status            reservation_status NOT NULL DEFAULT 'PENDING',
    source            reservation_source NOT NULL DEFAULT 'DIRECT',

    total_amount      NUMERIC(10, 2),
    currency          VARCHAR(3) NOT NULL DEFAULT 'RON',
    notes             VARCHAR(2000),

    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by        UUID,
    updated_by        UUID,

    CONSTRAINT chk_reservation_dates CHECK (check_out_date > check_in_date)
);

CREATE INDEX ix_reservations_property_id ON reservations (property_id);
CREATE INDEX ix_reservations_status ON reservations (status);
CREATE INDEX ix_reservations_dates ON reservations (check_in_date, check_out_date);

CREATE TRIGGER trg_reservations_updated_at
    BEFORE UPDATE ON reservations
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();
