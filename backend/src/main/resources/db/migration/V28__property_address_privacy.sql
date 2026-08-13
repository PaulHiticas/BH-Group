-- ============================================================
-- BH Group PMS — Public address privacy toggle
-- ============================================================
-- By default, the public property page shows only an approximate
-- location (rounded coordinates, no street address). Staff must
-- explicitly opt a property into showing its exact address/pin.

ALTER TABLE properties
    ADD COLUMN show_exact_address_publicly BOOLEAN NOT NULL DEFAULT false;
