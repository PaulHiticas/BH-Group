-- ============================================================
-- BH Group PMS — Real pricing engine
--
-- A single base_price_per_night is not enough for real operation:
-- adds seasonal rate overrides, weekend pricing, cleaning/extra-guest
-- fees, weekly/monthly discounts, min/max stay, and a cancellation
-- policy per property.
-- ============================================================

CREATE TYPE property_cancellation_policy AS ENUM (
    'FLEXIBLE',
    'MODERATE',
    'STRICT',
    'NON_REFUNDABLE'
);

ALTER TABLE properties
    ADD COLUMN weekend_price_per_night   NUMERIC(10, 2),
    ADD COLUMN cleaning_fee              NUMERIC(10, 2),
    ADD COLUMN extra_guest_fee           NUMERIC(10, 2),
    ADD COLUMN base_guests_included      INTEGER,
    ADD COLUMN weekly_discount_percent   NUMERIC(5, 2),
    ADD COLUMN monthly_discount_percent  NUMERIC(5, 2),
    ADD COLUMN min_stay_nights           INTEGER,
    ADD COLUMN max_stay_nights           INTEGER,
    ADD COLUMN cancellation_policy       property_cancellation_policy NOT NULL DEFAULT 'MODERATE';

-- ------------------------------------------------------------
-- seasonal_rates — date-range price overrides per property
-- ------------------------------------------------------------
CREATE TABLE seasonal_rates (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    property_id       UUID NOT NULL REFERENCES properties (id) ON DELETE CASCADE,

    label             VARCHAR(100) NOT NULL,
    start_date        DATE NOT NULL,
    end_date          DATE NOT NULL,
    price_per_night   NUMERIC(10, 2) NOT NULL,

    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by        UUID,
    updated_by        UUID,

    CONSTRAINT chk_seasonal_rate_dates CHECK (end_date >= start_date),
    CONSTRAINT chk_seasonal_rate_price CHECK (price_per_night >= 0)
);

CREATE INDEX ix_seasonal_rates_property_id ON seasonal_rates (property_id);
CREATE INDEX ix_seasonal_rates_dates ON seasonal_rates (start_date, end_date);

-- A property cannot have two seasonal rates covering the same date.
CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE seasonal_rates
    ADD CONSTRAINT ex_seasonal_rates_no_overlap
    EXCLUDE USING gist (
        property_id WITH =,
        daterange(start_date, end_date, '[]') WITH &&
    );

CREATE TRIGGER trg_seasonal_rates_updated_at
    BEFORE UPDATE ON seasonal_rates
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();
