-- ============================================================
-- BH Group PMS — Owner statements (periodic payout documents)
-- ============================================================

CREATE TYPE owner_statement_status AS ENUM (
    'ISSUED',
    'PAID'
);

CREATE TABLE owner_statements (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id            UUID NOT NULL REFERENCES users (id),
    period_start        DATE NOT NULL,
    period_end          DATE NOT NULL,
    currency            VARCHAR(3) NOT NULL DEFAULT 'RON',

    gross_revenue       NUMERIC(12, 2) NOT NULL,
    commission_amount   NUMERIC(12, 2) NOT NULL,
    expenses_total      NUMERIC(12, 2) NOT NULL,
    net_payout          NUMERIC(12, 2) NOT NULL,

    status              owner_statement_status NOT NULL DEFAULT 'ISSUED',
    generated_by        UUID REFERENCES users (id) ON DELETE SET NULL,
    paid_at             TIMESTAMPTZ,
    payment_reference   VARCHAR(255),

    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_owner_statements_period UNIQUE (owner_id, currency, period_start, period_end)
);

CREATE INDEX ix_owner_statements_owner_id ON owner_statements (owner_id);
CREATE INDEX ix_owner_statements_status ON owner_statements (status);

CREATE TRIGGER trg_owner_statements_updated_at
    BEFORE UPDATE ON owner_statements
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

-- property_id is a soft reference (ON DELETE SET NULL, not CASCADE): a
-- statement is a financial record of what was owed at generation time and
-- must survive the referenced property later being deleted, so the
-- property's name is also snapshotted onto the line rather than only
-- looked up live through the FK.
CREATE TABLE owner_statement_lines (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    statement_id        UUID NOT NULL REFERENCES owner_statements (id) ON DELETE CASCADE,
    property_id         UUID REFERENCES properties (id) ON DELETE SET NULL,
    property_name       VARCHAR(255) NOT NULL,

    gross_revenue       NUMERIC(12, 2) NOT NULL,
    commission_amount   NUMERIC(12, 2) NOT NULL,
    expenses_total      NUMERIC(12, 2) NOT NULL,
    net_amount          NUMERIC(12, 2) NOT NULL,

    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_owner_statement_lines_statement_id ON owner_statement_lines (statement_id);

CREATE TRIGGER trg_owner_statement_lines_updated_at
    BEFORE UPDATE ON owner_statement_lines
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();
