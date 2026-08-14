-- ============================================================
-- BH Group PMS — Lead type, consent, and UTM capture for owner leads
-- ============================================================
-- Adds the fields needed to replace the hardcoded revenue calculator
-- with a real lead-collecting "request an estimate" flow, and to
-- capture consent/UTM data on every public lead submission.

CREATE TYPE lead_type AS ENUM (
    'GENERAL',
    'REVENUE_ESTIMATE'
);

ALTER TABLE property_leads
    ADD COLUMN lead_type      lead_type NOT NULL DEFAULT 'GENERAL',
    ADD COLUMN bedrooms       INTEGER,
    ADD COLUMN consent_given  BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN utm_source     VARCHAR(255),
    ADD COLUMN utm_medium     VARCHAR(255),
    ADD COLUMN utm_campaign   VARCHAR(255);

CREATE INDEX ix_property_leads_lead_type ON property_leads (lead_type);
