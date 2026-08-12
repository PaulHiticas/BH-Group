CREATE TYPE integration_mode AS ENUM (
    'MANUAL',
    'ICAL',
    'CHANNEL_MANAGER'
);

ALTER TABLE properties
    ADD COLUMN integration_mode integration_mode NOT NULL DEFAULT 'MANUAL';

-- Properties that already have an iCal feed registered are, in practice,
-- already relying on iCal as their source of truth - reflect that instead
-- of silently defaulting them to MANUAL and having their scheduled syncs
-- skipped by the new mode guard.
UPDATE properties
SET integration_mode = 'ICAL'
WHERE id IN (SELECT DISTINCT property_id FROM ical_import_feeds);
