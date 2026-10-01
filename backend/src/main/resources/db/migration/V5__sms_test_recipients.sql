-- Global distribution list for SMS test intercept (environment ≠ prod → email copy)

CREATE TABLE sms_test_recipients (
    id          UUID PRIMARY KEY,
    email       VARCHAR(320) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by  VARCHAR(255)
);

CREATE UNIQUE INDEX uq_sms_test_recipients_email_lower
    ON sms_test_recipients (lower(email));

INSERT INTO sms_test_recipients (id, email, created_at, created_by)
VALUES
    ('a0000000-0000-4000-8000-000000000001', 'sms@optimizesolux.com', NOW(), 'system'),
    ('a0000000-0000-4000-8000-000000000002', 'ahonsueric01@gmail.com', NOW(), 'system');
