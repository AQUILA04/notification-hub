-- Client-declared environment (SMS: prod = real SMS, anything else = Mailpit intercept)

ALTER TABLE notifications
    ADD COLUMN environment VARCHAR(16) NOT NULL DEFAULT 'TEST';

ALTER TABLE notifications
    ADD CONSTRAINT chk_notifications_environment CHECK (environment IN ('TEST', 'PROD'));
