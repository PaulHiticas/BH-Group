-- ============================================================
-- BH Group PMS — Enforce no-overlap at the database level
--
-- The application already checks availability before creating a
-- reservation, but that check-then-insert is not atomic: two
-- concurrent requests can both pass the check and both insert,
-- double-booking the same property for overlapping dates. A GiST
-- exclusion constraint makes the database itself reject the second
-- insert, atomically, regardless of application-level races.
-- ============================================================

CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE reservations
    ADD CONSTRAINT ex_reservations_no_overlap
    EXCLUDE USING gist (
        property_id WITH =,
        daterange(check_in_date, check_out_date, '[)') WITH &&
    )
    WHERE (status NOT IN ('CANCELLED', 'NO_SHOW'));
