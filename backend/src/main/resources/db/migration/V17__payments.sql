-- ============================================================
-- BH Group PMS — Payment architecture (provider-agnostic)
--
-- Payment provider (Stripe vs Netopia) is not decided yet. This
-- builds the ledger (payments / payment_transactions / refunds /
-- payment_webhook_events) and starts with a MANUAL provider — staff
-- record payments collected offline (cash, bank transfer, card
-- terminal). A real online gateway can be added later as another
-- PaymentGateway implementation without touching this schema or the
-- booking logic: it only needs to create Payment/PaymentTransaction
-- rows and post to the webhook endpoint.
-- ============================================================

CREATE TYPE payment_provider AS ENUM (
    'MANUAL',
    'STRIPE',
    'NETOPIA'
);

CREATE TYPE payment_method AS ENUM (
    'CASH',
    'BANK_TRANSFER',
    'CARD_TERMINAL',
    'ONLINE_CARD',
    'OTHER'
);

CREATE TYPE payment_status AS ENUM (
    'PENDING',
    'PROCESSING',
    'SUCCEEDED',
    'FAILED',
    'CANCELLED',
    'PARTIALLY_REFUNDED',
    'REFUNDED'
);

CREATE TYPE payment_transaction_type AS ENUM (
    'CHARGE',
    'REFUND'
);

CREATE TYPE payment_transaction_status AS ENUM (
    'PENDING',
    'SUCCEEDED',
    'FAILED'
);

CREATE TYPE refund_status AS ENUM (
    'REQUESTED',
    'SUCCEEDED',
    'FAILED'
);

-- ------------------------------------------------------------
-- payments — one logical charge intent per reservation deposit/balance
-- ------------------------------------------------------------
CREATE TABLE payments (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reservation_id        UUID NOT NULL REFERENCES reservations (id) ON DELETE CASCADE,

    provider              payment_provider NOT NULL DEFAULT 'MANUAL',
    method                payment_method NOT NULL DEFAULT 'OTHER',
    status                payment_status NOT NULL DEFAULT 'PENDING',

    amount                NUMERIC(10, 2) NOT NULL,
    currency              VARCHAR(3) NOT NULL DEFAULT 'RON',
    refunded_amount       NUMERIC(10, 2) NOT NULL DEFAULT 0,

    provider_payment_id   VARCHAR(255),
    notes                 VARCHAR(1000),

    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by            UUID,
    updated_by            UUID,

    CONSTRAINT chk_payment_amount CHECK (amount > 0)
);

CREATE INDEX ix_payments_reservation_id ON payments (reservation_id);
CREATE INDEX ix_payments_status ON payments (status);
CREATE UNIQUE INDEX ux_payments_provider_payment_id ON payments (provider_payment_id)
    WHERE provider_payment_id IS NOT NULL;

CREATE TRIGGER trg_payments_updated_at
    BEFORE UPDATE ON payments
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

-- ------------------------------------------------------------
-- payment_transactions — raw ledger of gateway attempts/events for a payment
-- ------------------------------------------------------------
CREATE TABLE payment_transactions (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id               UUID NOT NULL REFERENCES payments (id) ON DELETE CASCADE,

    type                     payment_transaction_type NOT NULL,
    status                   payment_transaction_status NOT NULL DEFAULT 'PENDING',
    amount                   NUMERIC(10, 2) NOT NULL,
    provider_transaction_id  VARCHAR(255),
    failure_reason           VARCHAR(500),

    created_at               TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_payment_transactions_payment_id ON payment_transactions (payment_id);

-- ------------------------------------------------------------
-- refunds
-- ------------------------------------------------------------
CREATE TABLE refunds (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id           UUID NOT NULL REFERENCES payments (id) ON DELETE CASCADE,

    amount               NUMERIC(10, 2) NOT NULL,
    reason               VARCHAR(500),
    status               refund_status NOT NULL DEFAULT 'REQUESTED',
    provider_refund_id   VARCHAR(255),

    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by           UUID,

    CONSTRAINT chk_refund_amount CHECK (amount > 0)
);

CREATE INDEX ix_refunds_payment_id ON refunds (payment_id);

CREATE TRIGGER trg_refunds_updated_at
    BEFORE UPDATE ON refunds
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

-- ------------------------------------------------------------
-- payment_webhook_events — idempotent inbox for gateway webhook deliveries
-- ------------------------------------------------------------
CREATE TABLE payment_webhook_events (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider             payment_provider NOT NULL,
    external_event_id    VARCHAR(255) NOT NULL,
    event_type           VARCHAR(100) NOT NULL,
    payload              TEXT NOT NULL,
    processed_at         TIMESTAMPTZ,
    processing_error     VARCHAR(1000),

    received_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- A given gateway never delivers the same event id twice for the same
-- provider; enforcing this at the DB level makes webhook processing
-- idempotent even under concurrent duplicate deliveries.
CREATE UNIQUE INDEX ux_payment_webhook_events_provider_external_id
    ON payment_webhook_events (provider, external_event_id);
