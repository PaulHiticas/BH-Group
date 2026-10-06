-- ============================================================
-- BH Stays PMS — Property commission reporting
--
-- The apartments belong to their owners; BH Stays only keeps a
-- management commission, and only on the accommodation part of the
-- money actually collected - never on the cleaning fee, extra-guest
-- fee, late checkout, taxes, add-ons or anything else. The property
-- report, the dashboard, /finance and owner statements all compute it
-- the same way, from the same captured payments.
--
-- 1. properties.commission_percent (added in V12, nullable) becomes a
--    validated 0.00-100.00 percentage. It was never validated
--    server-side, so a value outside that range is cleared to NULL
--    ("commission not configured") instead of being reinterpreted.
--    Valid values an administrator already set are kept as they are,
--    and properties that never had one stay NULL.
--
-- 2. reservations get a price breakdown snapshot taken from the
--    server-side quote at the moment the total is set:
--      accommodation_amount  - commissionable: nightly rates (base,
--                              weekend, seasonal, dynamic pricing) after
--                              the weekly/monthly stay discount
--      every other column    - not commissionable, one per kind
--    The CHECK makes the parts add up exactly to total_amount, so the
--    accommodation share can always be verified against the total and
--    no difference can hide in rounding. Existing reservations keep
--    NULL - their breakdown is unknown and is reported as such
--    ("fără defalcare"), never estimated.
--
-- 3. owner statements record the same figures the reports show
--    (captured, refunds, commissionable base, percent, commission, owner
--    amount, money without a breakdown). Statements generated before
--    this migration are marked LEGACY_GROSS: their commission was taken
--    on the whole net amount, and they are kept exactly as issued.
-- ============================================================

UPDATE properties
SET commission_percent = NULL
WHERE commission_percent < 0 OR commission_percent > 100;

ALTER TABLE properties
    ADD CONSTRAINT chk_properties_commission_percent
        CHECK (commission_percent IS NULL OR (commission_percent >= 0 AND commission_percent <= 100));

-- ------------------------------------------------------------
-- reservation price breakdown snapshot
-- ------------------------------------------------------------
ALTER TABLE reservations
    ADD COLUMN accommodation_amount      NUMERIC(10, 2),
    ADD COLUMN cleaning_fee_amount       NUMERIC(10, 2),
    ADD COLUMN extra_guest_fee_amount    NUMERIC(10, 2),
    ADD COLUMN late_checkout_fee_amount  NUMERIC(10, 2),
    ADD COLUMN tax_amount                NUMERIC(10, 2),
    ADD COLUMN addon_amount              NUMERIC(10, 2);

ALTER TABLE reservations
    ADD CONSTRAINT chk_reservations_price_breakdown CHECK (
        (accommodation_amount IS NULL
            AND cleaning_fee_amount IS NULL
            AND extra_guest_fee_amount IS NULL
            AND late_checkout_fee_amount IS NULL
            AND tax_amount IS NULL
            AND addon_amount IS NULL)
        OR (
            accommodation_amount IS NOT NULL
            AND cleaning_fee_amount IS NOT NULL
            AND extra_guest_fee_amount IS NOT NULL
            AND late_checkout_fee_amount IS NOT NULL
            AND tax_amount IS NOT NULL
            AND addon_amount IS NOT NULL
            AND total_amount IS NOT NULL
            AND accommodation_amount >= 0
            AND cleaning_fee_amount >= 0
            AND extra_guest_fee_amount >= 0
            AND late_checkout_fee_amount >= 0
            AND tax_amount >= 0
            AND addon_amount >= 0
            AND accommodation_amount + cleaning_fee_amount + extra_guest_fee_amount
                + late_checkout_fee_amount + tax_amount + addon_amount = total_amount
        )
    );

-- The commission report filters a property's reservations by check-in date.
CREATE INDEX ix_reservations_property_check_in ON reservations (property_id, check_in_date);

-- ------------------------------------------------------------
-- owner statements on the same formula
-- ------------------------------------------------------------
ALTER TABLE owner_statements
    ADD COLUMN calculation_method             VARCHAR(40) NOT NULL DEFAULT 'LEGACY_GROSS',
    ADD COLUMN captured_total                 NUMERIC(12, 2),
    ADD COLUMN refunded_total                 NUMERIC(12, 2),
    ADD COLUMN commissionable_base            NUMERIC(12, 2),
    ADD COLUMN owner_amount                   NUMERIC(12, 2),
    ADD COLUMN unallocated_net_revenue        NUMERIC(12, 2),
    ADD COLUMN unallocated_reservation_count  INTEGER;

ALTER TABLE owner_statements
    ADD CONSTRAINT chk_owner_statements_calculation CHECK (
        (calculation_method = 'LEGACY_GROSS'
            AND captured_total IS NULL AND refunded_total IS NULL AND commissionable_base IS NULL
            AND owner_amount IS NULL AND unallocated_net_revenue IS NULL AND unallocated_reservation_count IS NULL)
        OR (calculation_method = 'CAPTURED_ACCOMMODATION'
            AND captured_total IS NOT NULL AND refunded_total IS NOT NULL AND commissionable_base IS NOT NULL
            AND owner_amount IS NOT NULL AND unallocated_net_revenue IS NOT NULL
            AND unallocated_reservation_count IS NOT NULL
            AND gross_revenue = captured_total - refunded_total
            AND owner_amount = gross_revenue - commission_amount
            AND net_payout = owner_amount - expenses_total)
    );

ALTER TABLE owner_statement_lines
    ADD COLUMN captured_total                 NUMERIC(12, 2),
    ADD COLUMN refunded_total                 NUMERIC(12, 2),
    ADD COLUMN commissionable_base            NUMERIC(12, 2),
    ADD COLUMN commission_percent             NUMERIC(5, 2),
    ADD COLUMN owner_amount                   NUMERIC(12, 2),
    ADD COLUMN unallocated_net_revenue        NUMERIC(12, 2),
    ADD COLUMN unallocated_reservation_count  INTEGER;

ALTER TABLE owner_statement_lines
    ADD CONSTRAINT chk_owner_statement_lines_calculation CHECK (
        (captured_total IS NULL AND refunded_total IS NULL AND commissionable_base IS NULL
            AND commission_percent IS NULL AND owner_amount IS NULL
            AND unallocated_net_revenue IS NULL AND unallocated_reservation_count IS NULL)
        OR (captured_total IS NOT NULL AND refunded_total IS NOT NULL AND commissionable_base IS NOT NULL
            -- a line with collected money always has a percent; an expense-only line may not
            AND (commission_percent IS NOT NULL OR captured_total = 0)
            AND (commission_percent IS NULL OR (commission_percent >= 0 AND commission_percent <= 100))
            AND owner_amount IS NOT NULL AND unallocated_net_revenue IS NOT NULL
            AND unallocated_reservation_count IS NOT NULL
            AND gross_revenue = captured_total - refunded_total
            AND owner_amount = gross_revenue - commission_amount
            AND net_amount = owner_amount - expenses_total)
    );
