-- ============================================================
-- BH Group PMS — Property expenses & financial reporting
-- ============================================================

CREATE TYPE expense_category AS ENUM (
    'CLEANING',
    'MAINTENANCE',
    'UTILITIES',
    'SUPPLIES',
    'TAX',
    'INSURANCE',
    'COMMISSION',
    'OTHER'
);

CREATE TABLE expenses (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    property_id            UUID NOT NULL REFERENCES properties (id) ON DELETE CASCADE,
    maintenance_ticket_id  UUID REFERENCES maintenance_tickets (id) ON DELETE SET NULL,

    category               expense_category NOT NULL DEFAULT 'OTHER',
    amount                 NUMERIC(10, 2) NOT NULL,
    currency               VARCHAR(3) NOT NULL DEFAULT 'RON',
    vendor                 VARCHAR(150),
    expense_date           DATE NOT NULL,
    notes                  VARCHAR(1000),
    charge_to_owner        BOOLEAN NOT NULL DEFAULT false,

    receipt_file_key       VARCHAR(500),
    receipt_url            VARCHAR(1000),

    created_by             UUID REFERENCES users (id) ON DELETE SET NULL,

    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_expenses_property_id ON expenses (property_id);
CREATE INDEX ix_expenses_expense_date ON expenses (expense_date);
CREATE INDEX ix_expenses_category ON expenses (category);

CREATE TRIGGER trg_expenses_updated_at
    BEFORE UPDATE ON expenses
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();
