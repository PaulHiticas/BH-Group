-- ============================================================
-- BH Group PMS — Cleaning task management
-- ============================================================

CREATE TYPE cleaning_task_status AS ENUM (
    'NEW',
    'ACCEPTED',
    'IN_PROGRESS',
    'DONE',
    'REJECTED'
);

-- ------------------------------------------------------------
-- property_checklist_items — per-property cleaning checklist template
-- ------------------------------------------------------------
CREATE TABLE property_checklist_items (
    property_id  UUID NOT NULL REFERENCES properties (id) ON DELETE CASCADE,
    sort_order   INTEGER NOT NULL,
    item         VARCHAR(200) NOT NULL,
    PRIMARY KEY (property_id, sort_order)
);

-- ------------------------------------------------------------
-- cleaning_tasks
-- ------------------------------------------------------------
CREATE TABLE cleaning_tasks (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    property_id       UUID NOT NULL REFERENCES properties (id) ON DELETE CASCADE,
    reservation_id    UUID REFERENCES reservations (id) ON DELETE SET NULL,
    status            cleaning_task_status NOT NULL DEFAULT 'NEW',
    assigned_cleaner_id UUID REFERENCES users (id) ON DELETE SET NULL,
    scheduled_date    DATE NOT NULL,
    notes             VARCHAR(2000),
    cost              NUMERIC(10, 2),
    estimated_minutes INTEGER,
    actual_minutes    INTEGER,
    started_at        TIMESTAMPTZ,
    completed_at      TIMESTAMPTZ,

    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by        UUID,
    updated_by        UUID
);

CREATE INDEX ix_cleaning_tasks_property_id ON cleaning_tasks (property_id);
CREATE INDEX ix_cleaning_tasks_cleaner_id ON cleaning_tasks (assigned_cleaner_id);
CREATE INDEX ix_cleaning_tasks_status ON cleaning_tasks (status);

CREATE TRIGGER trg_cleaning_tasks_updated_at
    BEFORE UPDATE ON cleaning_tasks
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

-- ------------------------------------------------------------
-- cleaning_task_checklist_results — snapshot of the checklist for one task
-- ------------------------------------------------------------
CREATE TABLE cleaning_task_checklist_results (
    cleaning_task_id UUID NOT NULL REFERENCES cleaning_tasks (id) ON DELETE CASCADE,
    item_label       VARCHAR(200) NOT NULL,
    checked          BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (cleaning_task_id, item_label)
);

-- ------------------------------------------------------------
-- cleaning_task_photos
-- ------------------------------------------------------------
CREATE TABLE cleaning_task_photos (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cleaning_task_id  UUID NOT NULL REFERENCES cleaning_tasks (id) ON DELETE CASCADE,
    file_key          VARCHAR(500) NOT NULL,
    url               VARCHAR(1000) NOT NULL,
    caption           VARCHAR(255),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_cleaning_task_photos_task_id ON cleaning_task_photos (cleaning_task_id);
