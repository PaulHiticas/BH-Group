-- ============================================================
-- BH Group PMS — iCal sync with Airbnb / Booking.com
--
-- Export: each property gets a token-authenticated .ics feed URL
-- that can be pasted into Airbnb/Booking.com "import calendar"
-- settings, so they never show a date as available that's already
-- booked through BH Group.
--
-- Import: admins register the property's Airbnb/Booking.com .ics
-- feed URL(s); a scheduled job pulls them periodically and creates
-- blocking reservations (source AIRBNB/BOOKING_COM, no price) for
-- each external booking, keyed by the feed's stable event UID so
-- re-syncs update/remove cleanly instead of duplicating.
-- ============================================================

ALTER TABLE properties ADD COLUMN ical_export_token VARCHAR(64) UNIQUE;

ALTER TABLE reservations ADD COLUMN external_uid VARCHAR(500);
CREATE UNIQUE INDEX ux_reservations_external_uid ON reservations (external_uid) WHERE external_uid IS NOT NULL;

CREATE TABLE ical_import_feeds (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    property_id      UUID NOT NULL REFERENCES properties (id) ON DELETE CASCADE,
    source           reservation_source NOT NULL,
    feed_url         VARCHAR(2000) NOT NULL,
    last_synced_at   TIMESTAMPTZ,
    last_sync_status VARCHAR(20),
    last_sync_error  VARCHAR(1000),

    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_ical_import_feeds_property_id ON ical_import_feeds (property_id);

CREATE TRIGGER trg_ical_import_feeds_updated_at
    BEFORE UPDATE ON ical_import_feeds
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();
