-- ============================================================
-- BH Group PMS — Guest messaging + staff in-app notifications
--
-- One conversation thread per reservation (no separate "conversation"
-- entity needed — the reservation is the natural thread key). Guests
-- reply through their existing management-token link (no account
-- needed, consistent with the rest of the public booking flow); staff
-- reply from the reservation detail page. Real-time delivery (WebSocket)
-- and SMS/WhatsApp are later increments — this is polling-based.
-- ============================================================

CREATE TYPE message_sender_type AS ENUM (
    'STAFF',
    'GUEST'
);

CREATE TABLE messages (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reservation_id    UUID NOT NULL REFERENCES reservations (id) ON DELETE CASCADE,
    sender_type       message_sender_type NOT NULL,
    sender_user_id    UUID REFERENCES users (id) ON DELETE SET NULL,
    body              VARCHAR(4000) NOT NULL,
    read_at           TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_messages_reservation_id ON messages (reservation_id);
CREATE INDEX ix_messages_reservation_read ON messages (reservation_id, read_at);

CREATE TYPE notification_type AS ENUM (
    'NEW_MESSAGE',
    'CRITICAL_MAINTENANCE',
    'NEW_LEAD'
);

-- One row per (recipient, event) — simpler and faster to query than a
-- nullable "broadcast" user_id, at the cost of a little duplication
-- when notifying every admin.
CREATE TABLE notifications (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type              notification_type NOT NULL,
    title             VARCHAR(200) NOT NULL,
    body              VARCHAR(1000),
    link_path         VARCHAR(255),
    read_at           TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_notifications_user_id ON notifications (user_id);
CREATE INDEX ix_notifications_user_unread ON notifications (user_id, read_at);
