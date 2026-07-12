-- ============================================================
-- BH Group PMS — Advanced document management: expiry tracking
--
-- Property documents (contracts, insurance, utility bills, ID copies)
-- often have a real-world expiration date. Adds an optional expiry
-- date plus a one-time reminder flag so admins get notified once,
-- ahead of time, instead of having to remember to check manually.
-- ============================================================

ALTER TABLE property_documents ADD COLUMN expires_at DATE;
ALTER TABLE property_documents ADD COLUMN expiry_notified_at TIMESTAMPTZ;

CREATE INDEX ix_property_documents_expires_at ON property_documents (expires_at)
    WHERE expires_at IS NOT NULL;

ALTER TYPE notification_type ADD VALUE 'DOCUMENT_EXPIRING';
