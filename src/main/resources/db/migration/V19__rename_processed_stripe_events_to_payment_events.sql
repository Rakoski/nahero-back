ALTER TABLE processed_stripe_events RENAME TO processed_payment_events;

ALTER TABLE processed_payment_events
    ADD COLUMN provider VARCHAR(32) NOT NULL DEFAULT 'STRIPE';

ALTER TABLE processed_payment_events
    ALTER COLUMN provider DROP DEFAULT;

ALTER INDEX idx_processed_stripe_events_received_at
    RENAME TO idx_processed_payment_events_received_at;

CREATE INDEX idx_processed_payment_events_provider
    ON processed_payment_events (provider);
