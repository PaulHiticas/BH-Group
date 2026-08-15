-- ============================================================
-- BH Group PMS — Dynamic pricing engine
--
-- Adds an opt-in dynamic pricing layer on top of the existing
-- seasonal/weekend/base pricing: per-property occupancy- and
-- lead-time-based multipliers, plus city- or property-scoped local
-- events. When dynamic_pricing_config.enabled is false (the default),
-- nothing here changes existing quote behavior.
-- ============================================================

-- ------------------------------------------------------------
-- dynamic_pricing_config — one row per property, opt-in
-- ------------------------------------------------------------
CREATE TABLE dynamic_pricing_config (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    property_id                 UUID NOT NULL UNIQUE REFERENCES properties (id) ON DELETE CASCADE,

    enabled                     BOOLEAN NOT NULL DEFAULT false,

    min_price                   NUMERIC(10, 2),
    max_price                   NUMERIC(10, 2),

    occupancy_window_days       INTEGER NOT NULL DEFAULT 14,
    occupancy_multiplier_min    NUMERIC(4, 2) NOT NULL DEFAULT 0.90,
    occupancy_multiplier_max    NUMERIC(4, 2) NOT NULL DEFAULT 1.30,

    lead_time_days              INTEGER NOT NULL DEFAULT 7,
    lead_time_multiplier        NUMERIC(4, 2) NOT NULL DEFAULT 1.00,

    created_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_dpc_price_range
        CHECK (min_price IS NULL OR max_price IS NULL OR min_price <= max_price),
    CONSTRAINT chk_dpc_occupancy_window_days CHECK (occupancy_window_days > 0),
    CONSTRAINT chk_dpc_lead_time_days CHECK (lead_time_days >= 0),
    CONSTRAINT chk_dpc_occupancy_multiplier_min CHECK (occupancy_multiplier_min > 0),
    CONSTRAINT chk_dpc_occupancy_multiplier_max CHECK (occupancy_multiplier_max >= occupancy_multiplier_min),
    CONSTRAINT chk_dpc_lead_time_multiplier CHECK (lead_time_multiplier > 0)
);

CREATE TRIGGER trg_dynamic_pricing_config_updated_at
    BEFORE UPDATE ON dynamic_pricing_config
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

-- ------------------------------------------------------------
-- local_events — a date-ranged price multiplier scoped to either a
-- whole city (applies to every property in that city) or a single
-- property (an override) - deliberately never global-to-everything.
-- ------------------------------------------------------------
CREATE TABLE local_events (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    city              VARCHAR(120),
    property_id       UUID REFERENCES properties (id) ON DELETE CASCADE,

    label             VARCHAR(100) NOT NULL,
    start_date        DATE NOT NULL,
    end_date          DATE NOT NULL,
    price_multiplier  NUMERIC(4, 2) NOT NULL DEFAULT 1.00,

    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_local_events_scope CHECK (city IS NOT NULL OR property_id IS NOT NULL),
    CONSTRAINT chk_local_events_dates CHECK (end_date >= start_date),
    CONSTRAINT chk_local_events_multiplier CHECK (price_multiplier > 0)
);

-- City matching is case-insensitive and trimmed at query time
-- (lower(trim(city)) against the property's own lower(trim(address.city)));
-- these indexes still help narrow down by date range first.
CREATE INDEX ix_local_events_city_dates ON local_events (city, start_date, end_date);
CREATE INDEX ix_local_events_property_dates ON local_events (property_id, start_date, end_date);

CREATE TRIGGER trg_local_events_updated_at
    BEFORE UPDATE ON local_events
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();
