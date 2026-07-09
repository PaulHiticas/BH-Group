-- ============================================================
-- BH Group PMS — Booking Engine support (Stage 5)
-- ============================================================

ALTER TABLE properties
    ADD COLUMN base_price_per_night NUMERIC(10, 2);

ALTER TABLE reservations
    ADD COLUMN management_token VARCHAR(100);

CREATE UNIQUE INDEX ux_reservations_management_token ON reservations (management_token);
