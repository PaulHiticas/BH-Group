-- ============================================================
-- BH Group PMS — Owner <-> admin contact threads
--
-- Lets a property owner send a request/question to the BH Group team,
-- optionally about a specific one of their apartments, and have a
-- back-and-forth conversation about it. Deliberately parallel to (and
-- independent from) the existing per-reservation guest/staff messaging
-- (V18, table `messages`) — different audience, different lifecycle
-- (a thread can be resolved/reopened; a reservation message thread
-- can't), so it gets its own tables rather than overloading that one.
-- ============================================================

CREATE TABLE owner_threads (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- RESTRICT, not CASCADE: an owner account can be deactivated, but we
    -- keep the history of what they asked. Deleting a user account
    -- outright (if that ever happens) must not silently wipe it.
    owner_id          UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,

    -- A request can be about one specific apartment, or general
    -- (property_id NULL). If the property is later deleted, the thread
    -- survives as a general request rather than disappearing.
    property_id       UUID REFERENCES properties (id) ON DELETE SET NULL,

    subject           VARCHAR(160) NOT NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    last_message_at   TIMESTAMPTZ NOT NULL DEFAULT now(),

    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_owner_threads_status CHECK (status IN ('OPEN', 'RESOLVED'))
);

CREATE INDEX ix_owner_threads_owner_last_message ON owner_threads (owner_id, last_message_at DESC);
CREATE INDEX ix_owner_threads_status_last_message ON owner_threads (status, last_message_at DESC);
CREATE INDEX ix_owner_threads_property_id ON owner_threads (property_id);

CREATE TRIGGER trg_owner_threads_updated_at
    BEFORE UPDATE ON owner_threads
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

-- sender_type is a plain VARCHAR+CHECK (Java-side enum), not a Postgres
-- native enum type like V18's message_sender_type — only two fixed
-- values are ever expected here, so a second PG enum type isn't worth
-- the extra ALTER TYPE ceremony every time a value might change.
CREATE TABLE owner_thread_messages (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    thread_id         UUID NOT NULL REFERENCES owner_threads (id) ON DELETE CASCADE,
    sender_type       VARCHAR(10) NOT NULL,
    sender_user_id    UUID REFERENCES users (id) ON DELETE SET NULL,
    body              VARCHAR(4000) NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    read_at           TIMESTAMPTZ,

    CONSTRAINT chk_owner_thread_messages_sender_type CHECK (sender_type IN ('OWNER', 'STAFF'))
);

CREATE INDEX ix_owner_thread_messages_thread_created ON owner_thread_messages (thread_id, created_at);
CREATE INDEX ix_owner_thread_messages_thread_read ON owner_thread_messages (thread_id, read_at);

-- Same pattern as V21 (DOCUMENT_EXPIRING) and V32 (LATE_CHECKOUT_REQUEST):
-- the Java-side NotificationType values are added here, but the
-- application only starts using them after this migration's transaction
-- commits (never inserted/updated within this same migration).
ALTER TYPE notification_type ADD VALUE 'NEW_OWNER_REQUEST';
ALTER TYPE notification_type ADD VALUE 'OWNER_REQUEST_REPLY';
