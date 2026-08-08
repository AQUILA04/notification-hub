-- Delayed outbox publish (retry backoff) + priority-aware dequeue

ALTER TABLE outbox_messages
    ADD COLUMN available_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

DROP INDEX IF EXISTS idx_outbox_unpublished;

CREATE INDEX idx_outbox_ready
    ON outbox_messages (available_at, created_at)
    WHERE published_at IS NULL;
