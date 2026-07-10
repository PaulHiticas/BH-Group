-- ============================================================
-- Adds a per-reservation access code (smart lock PIN or lockbox
-- code) and tracks when check-in instructions were emailed to
-- the guest, so the code can be sent automatically before arrival.
-- ============================================================

ALTER TABLE reservations
    ADD COLUMN access_code VARCHAR(50),
    ADD COLUMN access_code_sent_at TIMESTAMPTZ;
