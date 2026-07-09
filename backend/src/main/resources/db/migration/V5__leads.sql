-- ============================================================
-- BH Group PMS — Property owner leads
-- ============================================================

CREATE TABLE property_leads (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name          VARCHAR(150) NOT NULL,
    email              VARCHAR(255) NOT NULL,
    phone              VARCHAR(30),
    city               VARCHAR(100),
    message            VARCHAR(2000),
    contacted          BOOLEAN NOT NULL DEFAULT FALSE,

    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_property_leads_created_at ON property_leads (created_at);
CREATE INDEX ix_property_leads_contacted ON property_leads (contacted);

CREATE TRIGGER trg_property_leads_updated_at
    BEFORE UPDATE ON property_leads
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();
