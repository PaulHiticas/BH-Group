-- ============================================================
-- Adds MAINTENANCE as a reservation source so admins can block
-- calendar dates (e.g. cleaning, repairs) without a guest booking.
-- ============================================================

ALTER TYPE reservation_source ADD VALUE 'MAINTENANCE';
