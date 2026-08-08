-- Track the terminal outcome of each event, not just that we saw it.
-- APPLIED: event was applied to state (subscription created/updated).
-- IGNORED: event type we deliberately don't act on (invoice.payment_failed, unhandled types).
-- DEFERRED: we wanted to act but couldn't (user not found, etc.) — reconciliation will retry.
-- FAILED: hit max retry attempts — needs human review.
ALTER TABLE processed_payment_events
    ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT 'APPLIED';

ALTER TABLE processed_payment_events
    ALTER COLUMN status DROP DEFAULT;

ALTER TABLE processed_payment_events
    ADD COLUMN attempts INTEGER NOT NULL DEFAULT 1;

ALTER TABLE processed_payment_events
    ALTER COLUMN attempts DROP DEFAULT;

CREATE INDEX idx_processed_payment_events_status ON processed_payment_events (status);
