-- ============================================================
-- BH Stays PMS — Property commission reporting
--
-- The apartments belong to their owners; BH Stays only keeps a
-- management commission, and only on the accommodation part of the
-- money actually collected - never on the cleaning fee, extra-guest
-- fee or anything else. Two things are needed to report that
-- verifiably:
--
-- 1. properties.commission_percent (added in V12, nullable) becomes a
--    validated 0.00-100.00 percentage. It was never validated
--    server-side, so a value outside that range is cleared to NULL
--    ("commission not configured") instead of being reinterpreted.
--    Valid values an administrator already set are kept as they are,
--    and properties that never had one stay NULL.
--
-- 2. reservations get a price breakdown snapshot taken from the
--    server-side quote at the moment the total is set. The CHECK makes
--    the three parts always add up to total_amount, so the
--    accommodation share used for the commission can be verified
--    against the total. Existing reservations keep NULL - their
--    breakdown is unknown and is reported as such, never guessed.
-- ============================================================

UPDATE properties
SET commission_percent = NULL
WHERE commission_percent < 0 OR commission_percent > 100;

ALTER TABLE properties
    ADD CONSTRAINT chk_properties_commission_percent
        CHECK (commission_percent IS NULL OR (commission_percent >= 0 AND commission_percent <= 100));

ALTER TABLE reservations
    ADD COLUMN accommodation_amount    NUMERIC(10, 2),
    ADD COLUMN cleaning_fee_amount     NUMERIC(10, 2),
    ADD COLUMN extra_guest_fee_amount  NUMERIC(10, 2);

ALTER TABLE reservations
    ADD CONSTRAINT chk_reservations_price_breakdown CHECK (
        (accommodation_amount IS NULL AND cleaning_fee_amount IS NULL AND extra_guest_fee_amount IS NULL)
        OR (
            accommodation_amount IS NOT NULL
            AND cleaning_fee_amount IS NOT NULL
            AND extra_guest_fee_amount IS NOT NULL
            AND total_amount IS NOT NULL
            AND accommodation_amount >= 0
            AND cleaning_fee_amount >= 0
            AND extra_guest_fee_amount >= 0
            AND accommodation_amount + cleaning_fee_amount + extra_guest_fee_amount = total_amount
        )
    );

-- The commission report filters a property's reservations by check-in date.
CREATE INDEX ix_reservations_property_check_in ON reservations (property_id, check_in_date);
