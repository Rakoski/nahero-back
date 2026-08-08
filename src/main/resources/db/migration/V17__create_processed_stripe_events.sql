CREATE TABLE processed_stripe_events (
    event_id VARCHAR(255) PRIMARY KEY,
    type VARCHAR(255) NOT NULL,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_processed_stripe_events_received_at ON processed_stripe_events (received_at);
