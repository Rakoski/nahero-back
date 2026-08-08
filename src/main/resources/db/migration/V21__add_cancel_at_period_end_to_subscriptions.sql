-- Whether the subscription will terminate at the end of the current billing period
-- (user requested cancellation but still has access until period end).
ALTER TABLE subscriptions
    ADD COLUMN cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE subscriptions
    ALTER COLUMN cancel_at_period_end DROP DEFAULT;
