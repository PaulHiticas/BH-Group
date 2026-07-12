-- ============================================================
-- BH Group PMS — Maintenance ticketing
-- ============================================================

CREATE TYPE maintenance_category AS ENUM (
    'PLUMBING',
    'ELECTRICAL',
    'APPLIANCE',
    'HVAC',
    'STRUCTURAL',
    'OTHER'
);

CREATE TYPE maintenance_priority AS ENUM (
    'LOW',
    'MEDIUM',
    'HIGH',
    'CRITICAL'
);

CREATE TYPE maintenance_status AS ENUM (
    'OPEN',
    'IN_PROGRESS',
    'RESOLVED',
    'CLOSED'
);

-- ------------------------------------------------------------
-- maintenance_tickets
-- ------------------------------------------------------------
CREATE TABLE maintenance_tickets (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    property_id       UUID NOT NULL REFERENCES properties (id) ON DELETE CASCADE,

    title             VARCHAR(200) NOT NULL,
    description       VARCHAR(4000),
    category          maintenance_category NOT NULL DEFAULT 'OTHER',
    priority          maintenance_priority NOT NULL DEFAULT 'MEDIUM',
    status            maintenance_status NOT NULL DEFAULT 'OPEN',

    reported_by_id    UUID REFERENCES users (id) ON DELETE SET NULL,
    assigned_to_id    UUID REFERENCES users (id) ON DELETE SET NULL,
    vendor            VARCHAR(150),

    estimated_cost    NUMERIC(10, 2),
    actual_cost       NUMERIC(10, 2),
    resolved_at       TIMESTAMPTZ,

    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by        UUID,
    updated_by        UUID
);

CREATE INDEX ix_maintenance_tickets_property_id ON maintenance_tickets (property_id);
CREATE INDEX ix_maintenance_tickets_assigned_to_id ON maintenance_tickets (assigned_to_id);
CREATE INDEX ix_maintenance_tickets_status ON maintenance_tickets (status);
CREATE INDEX ix_maintenance_tickets_priority ON maintenance_tickets (priority);

CREATE TRIGGER trg_maintenance_tickets_updated_at
    BEFORE UPDATE ON maintenance_tickets
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

-- ------------------------------------------------------------
-- maintenance_ticket_photos
-- ------------------------------------------------------------
CREATE TABLE maintenance_ticket_photos (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    maintenance_ticket_id UUID NOT NULL REFERENCES maintenance_tickets (id) ON DELETE CASCADE,
    file_key              VARCHAR(500) NOT NULL,
    url                   VARCHAR(1000) NOT NULL,
    caption               VARCHAR(255),
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_maintenance_ticket_photos_ticket_id ON maintenance_ticket_photos (maintenance_ticket_id);
