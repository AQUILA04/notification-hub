-- Audit trail multi-tenant (actions ops / API)

CREATE TABLE audit_events (
    id           BIGSERIAL PRIMARY KEY,
    tenant_id    VARCHAR(128) NOT NULL,
    action       VARCHAR(64)  NOT NULL,
    resource_id  VARCHAR(128),
    actor        VARCHAR(255),
    payload      JSONB,
    occurred_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_tenant_time ON audit_events (tenant_id, occurred_at DESC);
CREATE INDEX idx_audit_action ON audit_events (tenant_id, action);
