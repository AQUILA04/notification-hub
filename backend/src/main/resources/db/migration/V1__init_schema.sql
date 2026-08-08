-- Notification Hub — schéma initial (multi-tenant via tenant_id)

CREATE TABLE notifications (
    id                  UUID PRIMARY KEY,
    tenant_id           VARCHAR(128)  NOT NULL,
    channel             VARCHAR(32)   NOT NULL,
    status              VARCHAR(32)   NOT NULL,
    from_address        VARCHAR(512)  NOT NULL,
    to_addresses        JSONB         NOT NULL,
    subject             VARCHAR(998),
    body                TEXT,
    template_name       VARCHAR(255),
    template_data       JSONB,
    priority            VARCHAR(16)   NOT NULL DEFAULT 'NORMAL',
    max_attempts        INT           NOT NULL DEFAULT 5,
    attempt_count       INT           NOT NULL DEFAULT 0,
    idempotency_key     VARCHAR(255),
    metadata            JSONB,
    provider_message_id VARCHAR(512),
    last_error          TEXT,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    sent_at             TIMESTAMPTZ,
    CONSTRAINT chk_notifications_channel CHECK (channel IN ('EMAIL', 'SMS', 'WHATSAPP')),
    CONSTRAINT chk_notifications_priority CHECK (priority IN ('HIGH', 'NORMAL', 'LOW')),
    CONSTRAINT chk_notifications_content CHECK (
        body IS NOT NULL OR template_name IS NOT NULL
    )
);

CREATE UNIQUE INDEX uq_notifications_tenant_idempotency
    ON notifications (tenant_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE INDEX idx_notifications_tenant_status
    ON notifications (tenant_id, status);

CREATE INDEX idx_notifications_tenant_created
    ON notifications (tenant_id, created_at DESC);

CREATE INDEX idx_notifications_channel_status
    ON notifications (channel, status);

-- Event Store append-only
CREATE TABLE notification_events (
    id               BIGSERIAL PRIMARY KEY,
    notification_id  UUID         NOT NULL REFERENCES notifications (id),
    tenant_id        VARCHAR(128) NOT NULL,
    sequence_no      INT          NOT NULL,
    event_type       VARCHAR(64)  NOT NULL,
    payload          JSONB,
    occurred_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_notification_events_seq UNIQUE (notification_id, sequence_no)
);

CREATE INDEX idx_notification_events_tenant
    ON notification_events (tenant_id, occurred_at DESC);

-- Templates Pebble versionnés
CREATE TABLE templates (
    id               UUID PRIMARY KEY,
    tenant_id        VARCHAR(128) NOT NULL,
    name             VARCHAR(255) NOT NULL,
    version          INT          NOT NULL,
    channel          VARCHAR(32)  NOT NULL,
    engine           VARCHAR(32)  NOT NULL DEFAULT 'PEBBLE',
    subject_template TEXT,
    body_template    TEXT         NOT NULL,
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by       VARCHAR(255),
    CONSTRAINT uq_templates_tenant_name_version UNIQUE (tenant_id, name, version),
    CONSTRAINT chk_templates_channel CHECK (channel IN ('EMAIL', 'SMS', 'WHATSAPP')),
    CONSTRAINT chk_templates_engine CHECK (engine IN ('PEBBLE'))
);

CREATE INDEX idx_templates_tenant_name_active
    ON templates (tenant_id, name, active);

-- Transactional outbox → Artemis
CREATE TABLE outbox_messages (
    id               BIGSERIAL PRIMARY KEY,
    notification_id  UUID         NOT NULL REFERENCES notifications (id),
    tenant_id        VARCHAR(128) NOT NULL,
    channel          VARCHAR(32)  NOT NULL,
    priority         VARCHAR(16)  NOT NULL DEFAULT 'NORMAL',
    payload          JSONB        NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    published_at     TIMESTAMPTZ
);

CREATE INDEX idx_outbox_unpublished
    ON outbox_messages (created_at)
    WHERE published_at IS NULL;
