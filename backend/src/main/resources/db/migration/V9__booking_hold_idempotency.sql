-- ============================================================
-- BH Group PMS — Booking hold + idempotency for public bookings
--
-- Public guest bookings land as PENDING with no expiry and no
-- idempotency protection: a double form-submit or a network retry
-- can create duplicate reservations, and an unpaid PENDING booking
-- holds the calendar forever. This adds:
--   - idempotency_key: a client-supplied key so retries of the same
--     booking attempt return the existing reservation instead of
--     creating a duplicate.
--   - hold_expires_at: when set, a scheduled job cancels the
--     reservation past this instant if it is still unpaid/PENDING,
--     freeing the calendar for other guests.
-- ============================================================

ALTER TABLE reservations
    ADD COLUMN idempotency_key VARCHAR(100),
    ADD COLUMN hold_expires_at TIMESTAMPTZ;

CREATE UNIQUE INDEX ux_reservations_idempotency_key ON reservations (idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE INDEX ix_reservations_hold_expires_at ON reservations (hold_expires_at)
    WHERE hold_expires_at IS NOT NULL;
