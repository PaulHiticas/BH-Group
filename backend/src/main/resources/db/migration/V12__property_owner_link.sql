ALTER TABLE properties
    ADD COLUMN owner_id           UUID REFERENCES users (id) ON DELETE SET NULL,
    ADD COLUMN commission_percent NUMERIC(5, 2);

CREATE INDEX ix_properties_owner_id ON properties (owner_id);
