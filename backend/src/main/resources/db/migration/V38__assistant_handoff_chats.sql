-- ============================================================
-- BH Group PMS — AI assistant human handoff chats
--
-- When a website visitor asks to talk to a person (or the AI assistant
-- detects it can't help), a persistent chat thread is created so staff
-- can pick it up from an inbox and the anonymous visitor can poll for
-- the reply via an unguessable public token. Deliberately mirrors
-- owner_threads/owner_thread_messages (V36) - same OPEN/RESOLVED
-- lifecycle, same plain VARCHAR+CHECK enum style - but for an anonymous
-- GUEST instead of an authenticated OWNER, plus an AI sender type for
-- the assistant's own turns saved into the same thread.
-- ============================================================

CREATE TABLE assistant_chats (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Unguessable capability token handed to the anonymous visitor so
    -- they can poll their own chat - same raw-token approach as
    -- reservations.management_token, not hashed (low-stakes: read access
    -- to one's own already-anonymous conversation, not a password reset).
    public_token      VARCHAR(100) NOT NULL,

    guest_name        VARCHAR(160),
    guest_email       VARCHAR(255),

    status            VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    last_message_at   TIMESTAMPTZ NOT NULL DEFAULT now(),

    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_assistant_chats_status CHECK (status IN ('OPEN', 'RESOLVED'))
);

CREATE UNIQUE INDEX ux_assistant_chats_public_token ON assistant_chats (public_token);
CREATE INDEX ix_assistant_chats_status_last_message ON assistant_chats (status, last_message_at DESC);

CREATE TRIGGER trg_assistant_chats_updated_at
    BEFORE UPDATE ON assistant_chats
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

-- sender_type is a plain VARCHAR+CHECK (Java-side enum), same reasoning
-- as owner_thread_messages.sender_type - three fixed values, not worth a
-- second Postgres native enum type.
CREATE TABLE assistant_chat_messages (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chat_id           UUID NOT NULL REFERENCES assistant_chats (id) ON DELETE CASCADE,
    sender_type       VARCHAR(10) NOT NULL,
    sender_user_id    UUID REFERENCES users (id) ON DELETE SET NULL,
    body              VARCHAR(4000) NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    read_at           TIMESTAMPTZ,

    CONSTRAINT chk_assistant_chat_messages_sender_type CHECK (sender_type IN ('GUEST', 'AI', 'STAFF'))
);

CREATE INDEX ix_assistant_chat_messages_chat_created ON assistant_chat_messages (chat_id, created_at);
CREATE INDEX ix_assistant_chat_messages_chat_read ON assistant_chat_messages (chat_id, read_at);

-- Same pattern as V36: the Java-side NotificationType value is added
-- here, used only after this migration's transaction commits.
ALTER TYPE notification_type ADD VALUE 'NEW_ASSISTANT_HANDOFF';
