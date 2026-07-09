-- ============================================================
-- BH Group PMS — Property Management (Stage 2)
-- ============================================================

CREATE TYPE property_type AS ENUM (
    'APARTMENT',
    'HOUSE',
    'VILLA',
    'STUDIO',
    'ROOM',
    'CABIN',
    'OTHER'
);

CREATE TYPE property_status AS ENUM (
    'DRAFT',
    'ACTIVE',
    'INACTIVE',
    'MAINTENANCE'
);

CREATE TYPE property_facility AS ENUM (
    'WIFI',
    'PARKING',
    'POOL',
    'AIR_CONDITIONING',
    'HEATING',
    'KITCHEN',
    'TV',
    'WASHER',
    'DRYER',
    'ELEVATOR',
    'PET_FRIENDLY',
    'SMART_LOCK',
    'BALCONY',
    'GYM',
    'WORKSPACE',
    'BREAKFAST'
);

CREATE TYPE property_document_type AS ENUM (
    'CONTRACT',
    'INVOICE',
    'ID_COPY',
    'UTILITY_BILL',
    'OTHER'
);

-- ------------------------------------------------------------
-- properties
-- ------------------------------------------------------------
CREATE TABLE properties (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name              VARCHAR(200) NOT NULL,
    description       VARCHAR(4000),
    property_type     property_type NOT NULL DEFAULT 'APARTMENT',
    status            property_status NOT NULL DEFAULT 'DRAFT',

    address_line      VARCHAR(255) NOT NULL,
    city              VARCHAR(100) NOT NULL,
    county            VARCHAR(100),
    postal_code       VARCHAR(20),
    country           VARCHAR(100) NOT NULL DEFAULT 'Romania',
    latitude          DOUBLE PRECISION,
    longitude         DOUBLE PRECISION,

    bedrooms          INTEGER NOT NULL DEFAULT 0,
    bathrooms         INTEGER NOT NULL DEFAULT 0,
    max_guests        INTEGER NOT NULL DEFAULT 1,
    size_sqm          NUMERIC(8, 2),

    check_in_time     TIME NOT NULL DEFAULT '14:00',
    check_out_time    TIME NOT NULL DEFAULT '11:00',

    smart_lock_enabled  BOOLEAN NOT NULL DEFAULT FALSE,
    smart_lock_provider VARCHAR(100),
    smart_lock_device_id VARCHAR(150),

    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by        UUID,
    updated_by        UUID
);

CREATE INDEX ix_properties_status ON properties (status);
CREATE INDEX ix_properties_city ON properties (city);

CREATE TRIGGER trg_properties_updated_at
    BEFORE UPDATE ON properties
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

-- ------------------------------------------------------------
-- property_facilities (element collection)
-- ------------------------------------------------------------
CREATE TABLE property_facilities (
    property_id  UUID NOT NULL REFERENCES properties (id) ON DELETE CASCADE,
    facility     property_facility NOT NULL,
    PRIMARY KEY (property_id, facility)
);

-- ------------------------------------------------------------
-- property_photos
-- ------------------------------------------------------------
CREATE TABLE property_photos (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    property_id  UUID NOT NULL REFERENCES properties (id) ON DELETE CASCADE,
    file_key     VARCHAR(500) NOT NULL,
    url          VARCHAR(500) NOT NULL,
    caption      VARCHAR(255),
    sort_order   INTEGER NOT NULL DEFAULT 0,
    is_cover     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_property_photos_property_id ON property_photos (property_id);

-- ------------------------------------------------------------
-- property_documents
-- ------------------------------------------------------------
CREATE TABLE property_documents (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    property_id    UUID NOT NULL REFERENCES properties (id) ON DELETE CASCADE,
    file_name      VARCHAR(255) NOT NULL,
    file_key       VARCHAR(500) NOT NULL,
    url            VARCHAR(500) NOT NULL,
    document_type  property_document_type NOT NULL DEFAULT 'OTHER',
    uploaded_by    UUID REFERENCES users (id) ON DELETE SET NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_property_documents_property_id ON property_documents (property_id);
